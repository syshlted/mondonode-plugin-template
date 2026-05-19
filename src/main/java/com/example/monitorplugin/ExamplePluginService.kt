package com.example.monitorplugin

import com.systemhalted.mondonode.sdk.IDataCallback
import com.systemhalted.mondonode.sdk.MetricData
import com.systemhalted.mondonode.sdk.ParamType
import com.systemhalted.mondonode.sdk.PluginCapability
import com.systemhalted.mondonode.sdk.PluginCapabilityType
import com.systemhalted.mondonode.sdk.PluginManifest
import com.systemhalted.mondonode.sdk.PluginParameterSpec
import com.systemhalted.mondonode.sdk.PluginServiceBase
import com.systemhalted.mondonode.sdk.ValidRange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Minimal example of a MondoNode plugin service.
 *
 * Steps to create your own plugin:
 *  1. Copy this module and rename the package.
 *  2. Replace the manifest values (pluginId, name, capabilities, etc.).
 *  3. Implement your monitoring logic in onSubscribe / onUnsubscribe.
 *  4. Declare the service in AndroidManifest.xml (see this module's manifest).
 *  5. Publish to Play Store — the host app will discover it automatically.
 */
class ExamplePluginService : PluginServiceBase() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var streamJob: Job? = null

    // The public key is populated at runtime from the Keystore by PluginServiceBase.
    // Pass an empty string here; getPublicKey() in the base class returns the real value.
    override fun onGetManifest() = PluginManifest(
        pluginId = "com.example.monitorplugin.example",
        name = "Example Plugin",
        version = "1.0.0",
        author = "Your Name",
        description = "A minimal example MondoNode plugin.",
        capabilities = listOf(
            PluginCapability(
                type = PluginCapabilityType.NETWORK_MONITOR,
                id = "example_metric",
                name = "Example Metric",
                description = "Emits a counter at a configurable interval.",
                parameters = listOf(
                    PluginParameterSpec(
                        key = "interval_seconds",
                        type = ParamType.INT,
                        shortName = "Interval",
                        description = "How often to emit a counter value.",
                        required = false,
                        unit = "seconds",
                        validRange = ValidRange(min = 1.0, max = 3600.0),
                        defaultValue = "5"
                    )
                )
            )
        ),
        publicKeyBase64 = "" // filled by SDK at runtime
    )

    override fun onSubscribe(callback: IDataCallback) {
        streamJob?.cancel()
        streamJob = scope.launch {
            var counter = 0
            while (true) {
                val metric = MetricData(
                    pluginId = "com.example.monitorplugin.example",
                    capabilityId = "example_metric",
                    timestampMs = System.currentTimeMillis(),
                    values = mapOf("counter" to counter.toString())
                )
                callback.onMetricReceived(Json.encodeToString(metric))
                counter++
                delay(5_000)
            }
        }
    }

    override fun onUnsubscribe() {
        streamJob?.cancel()
        streamJob = null
    }

    override fun onConfigure(configJson: String): Boolean {
        // Parse configJson and apply settings here.
        return true
    }

    // Override onExecute() for efficient single-shot alarm-triggered monitoring.
    // The base class default (subscribe → first result → unsubscribe) works without
    // this override, but plugins that already perform one-shot measurements should
    // implement this directly to avoid starting a streaming loop unnecessarily.
    // If you do not override, delete this method — the default behaviour is correct.
    override fun onExecute(configJson: String, callback: com.systemhalted.mondonode.sdk.IDataCallback) {
        scope.launch {
            val metric = MetricData(
                pluginId = "com.example.monitorplugin.example",
                capabilityId = "example_metric",
                timestampMs = System.currentTimeMillis(),
                values = mapOf("counter" to "0")
            )
            callback.onMetricReceived(Json.encodeToString(metric))
        }
    }
}
