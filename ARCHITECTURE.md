# mondonode-plugin-template — Architecture

Minimal starting point for third-party monitoring plugin authors. One plugin service, one About activity, a billing-disabled stub. No non-SDK dependencies beyond kotlinx.serialization and coroutines. Public git subtree (`plugin-template-origin`).

```mermaid
classDiagram
    direction TB

    class Service {
        <<Android>>
        +onBind(intent) IBinder
    }

    class PluginServiceBase {
        <<SDK · abstract>>
        #hostBridge IPluginHostBridge?
        +onBind(intent) IBinder
        #onGetManifest()* PluginManifest
        #onSubscribe(callback IDataCallback)*
        #onUnsubscribe()*
        #onConfigure(configJson String) Boolean*
        #onExecute(configJson String, callback IDataCallback)*
        note: handles Keystore key generation + nonce signing
        note: default onExecute: subscribe → first metric → unsubscribe
    }

    class ExamplePluginService {
        <<Service APK · com.example.monitorplugin>>
        -scope CoroutineScope
        -streamJob Job?
        -counter Int
        +onGetManifest() PluginManifest
        +onSubscribe(callback IDataCallback)
        +onUnsubscribe()
        +onConfigure(configJson) Boolean
        +onExecute(configJson, callback IDataCallback)
        note: onSubscribe: emits MetricData every 5 s on Dispatchers.IO
        note: onUnsubscribe: cancels the streaming Job
        note: onExecute: shown as override but base default is valid
        note: metric emits counter incremented each interval
    }

    class IDataCallback {
        <<AIDL oneway>>
        +onMetricReceived(metricJson)
        +onPluginError(code, message)
        +onPluginStatusChanged(status)
    }

    class PluginManifest {
        <<SDK data class>>
        +pluginId = "com.example.monitorplugin.example" (replace)
        +name = "Example Plugin"
        +version = "1.0.0"
        +capabilities List~PluginCapability~
        +publicKeyBase64 = "" (SDK fills from Keystore at runtime)
        +pluginDependencies = emptyList()
    }

    class PluginCapability {
        <<SDK data class>>
        +id = "example_counter"
        +type = CUSTOM
        +parameters List~PluginParameterSpec~
        note: interval_seconds INT default=5 range 1-3600
    }

    class AboutActivity {
        <<Activity · com.example.monitorplugin>>
        +onCreate(savedInstanceState)
        +onDestroy()
        note: delegates to BillingGateway.launch / teardown
        note: hardcodes pluginName, description, githubUrl, playStoreUrl — replace
    }

    class BillingGateway {
        <<seam object>>
        +launch(activity, pluginName, pluginDescription, githubUrl, playStoreUrl)
        +teardown(activity)
        note: billing-disabled stub opens githubUrl and finishes
        note: real impl from mondonode-billing when superproject sets mondonode.billing.enabled=true
    }

    Service <|-- PluginServiceBase
    PluginServiceBase <|-- ExamplePluginService
    ExamplePluginService ..> IDataCallback : calls onMetricReceived
    ExamplePluginService ..> PluginManifest : returns from onGetManifest
    PluginManifest "1" *-- "1..*" PluginCapability
    AboutActivity ..> BillingGateway : launch / teardown
```
