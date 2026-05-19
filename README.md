# MondoNode Plugin Template

A minimal working example of a MondoNode monitoring plugin. Clone or copy this module as the starting point for your own plugin APK.

## Prerequisites

- Android Studio Ladybug or later
- Min SDK 26 (Android 8.0)
- `mondonode-sdk` added as a dependency (see [Adding the SDK](#adding-the-sdk))

## Quickstart

1. Copy this module into your project (or use it as a standalone Android app module).
2. Rename the package from `com.example.monitorplugin` to your own reverse-domain package.
3. Update `applicationId` and `namespace` in `build.gradle.kts` to match.
4. Edit `ExamplePluginService` — replace the manifest values and implement your monitoring logic.
5. Build and install the APK on the same device as MondoNode. The host app discovers it automatically via `PackageManager`.

## What to implement

Your plugin service extends `PluginServiceBase` and overrides four methods:

### `onGetManifest(): PluginManifest`

Describes your plugin to the host. Fill in a stable, globally unique `pluginId` (reverse-domain format recommended), your capabilities, and the parameter schema each capability accepts.

```kotlin
override fun onGetManifest() = PluginManifest(
    pluginId   = "com.example.myplugin.latency",
    name       = "Latency Monitor",
    version    = "1.0.0",
    author     = "Your Name",
    description = "Measures round-trip latency to a configurable host.",
    capabilities = listOf(
        PluginCapability(
            type        = PluginCapabilityType.NETWORK_MONITOR,
            id          = "ping",
            name        = "Ping",
            description = "ICMP echo round-trip time.",
            parameters  = listOf(
                PluginParameterSpec(
                    key          = "host",
                    type         = ParamType.STRING,
                    shortName    = "Host",
                    description  = "Hostname or IP address to ping.",
                    required     = true
                )
            )
        )
    ),
    publicKeyBase64 = "" // filled by SDK at runtime — leave empty
)
```

### `onSubscribe(callback: IDataCallback)`

Called when the host starts a continuous monitoring session. Start your measurement loop here and call `callback.onMetricReceived(json)` each time you have a result. Use a coroutine `Job` so you can cancel cleanly.

```kotlin
override fun onSubscribe(callback: IDataCallback) {
    streamJob?.cancel()
    streamJob = scope.launch {
        while (true) {
            val rtt = measureRtt()
            callback.onMetricReceived(Json.encodeToString(MetricData(
                pluginId      = "com.example.myplugin.latency",
                capabilityId  = "ping",
                timestampMs   = System.currentTimeMillis(),
                values        = mapOf("rtt_ms" to rtt.toString())
            )))
            delay(intervalMs)
        }
    }
}
```

### `onUnsubscribe()`

Cancel your measurement loop and release any held resources.

```kotlin
override fun onUnsubscribe() {
    streamJob?.cancel()
    streamJob = null
}
```

### `onConfigure(configJson: String): Boolean`

Called when the host pushes a configuration update. Parse the JSON and apply your settings. Return `true` on success, `false` if the config is invalid.

### `onExecute(configJson: String, callback: IDataCallback)` *(optional)*

Called for single-shot, alarm-triggered executions. If your plugin already performs discrete one-shot measurements (e.g. a single ping), implement this directly — it avoids starting a streaming loop unnecessarily. If you omit it, the base class default (subscribe → first result → unsubscribe) works correctly without any changes.

## AndroidManifest requirements

Your service must be exported and declare the plugin intent action so MondoNode can discover and bind to it:

```xml
<service
    android:name=".YourPluginService"
    android:exported="true"
    android:permission="com.systemhalted.mondonode.BIND_PLUGIN">
    <intent-filter>
        <action android:name="com.systemhalted.mondonode.PLUGIN" />
    </intent-filter>
</service>
```

`android:permission="com.systemhalted.mondonode.BIND_PLUGIN"` is required. It is a
signature-level permission held only by the MondoNode host app, so Android rejects bind attempts
from any other process before your service code runs. Other plugin categories use their own
`BIND_*` permission (`BIND_TRANSFORMER`, `BIND_ROUTER`, `BIND_OUTPUT`, `BIND_STORAGE`,
`BIND_EVENT`, `BIND_MANAGEMENT_PLUGIN`).

## MetricData values

`MetricData.values` is a `Map<String, String>` — all metric values are strings regardless of their underlying type. Consumers can use transformer plugins to parse or convert them. Use descriptive keys that match the descriptions in your `PluginParameterSpec` list.

`timestampMs` must be UTC epoch milliseconds (`System.currentTimeMillis()`). Never use a timezone-aware type here.

## Plugin trust tiers

MondoNode uses a three-layer authentication protocol, with a per-call UID check as defence-in-depth:

0. **OS binding gate (signature permission)** — your service declares `android:permission="com.systemhalted.mondonode.BIND_PLUGIN"`. The OS only allows apps signed with the same certificate as the host to hold the permission, so unauthorised callers cannot even `bindService()`.
1. **Cert fingerprint check** — the host reads your APK's signing certificate and compares its SHA-256 fingerprint against a pinned list. Official SystemHalted plugins are in that list; third-party plugins are not, and will be offered to the user as *Unverified*.
2. **Challenge-response** — after binding, the host sends a random nonce. Your plugin signs it using an EC key in the Android Keystore. `PluginServiceBase` handles key generation and signing automatically — you do not need to touch this.

In addition, every AIDL method checks that the calling UID is the MondoNode host's UID and throws `SecurityException` otherwise — handled for you by the base class.

During development, enable **insecure mode** in the MondoNode host app to allow unverified plugins to connect.

## Adding the SDK

The SDK is not yet published to Maven Central. In the meantime, include it as a local module or a git subtree dependency.

**Local module (recommended during development):**

```kotlin
// settings.gradle.kts
includeBuild("../mondonode-sdk")

// build.gradle.kts
dependencies {
    implementation("com.systemhalted.mondonode:mondonode-sdk")
}
```

**Version catalog entry:**

```toml
[libraries]
mondonode-sdk = { group = "com.systemhalted.mondonode", name = "mondonode-sdk", version = "1.0.0" }
```

## Other plugin types

This template covers **monitoring plugins** (`PluginServiceBase`). The SDK also provides base classes for:

| Plugin type | Base class | Intent action |
|---|---|---|
| Transformer | `TransformerServiceBase` | `com.systemhalted.mondonode.TRANSFORMER` |
| Router | `RouterServiceBase` | `com.systemhalted.mondonode.ROUTER` |
| Output | `OutputServiceBase` | `com.systemhalted.mondonode.OUTPUT` |
| Storage | `StorageServiceBase` | `com.systemhalted.mondonode.STORAGE` |
| Event | `EventServiceBase` | `com.systemhalted.mondonode.EVENT` |
| Management app | `ManagementServiceBase` | `com.systemhalted.mondonode.MANAGEMENT_PLUGIN` |

Each follows the same pattern: extend the base class, implement the hooks, declare the service with the corresponding intent action and `android:exported="true"`.

## License

© 2025 SystemHalted. See [LICENSE](../LICENSE) for terms.
