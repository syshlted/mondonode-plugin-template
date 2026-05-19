package com.example.monitorplugin

import android.app.Activity
import android.os.Bundle
import com.systemhalted.mondonode.billing.BillingGateway

class AboutActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BillingGateway.launch(
            activity = this,
            pluginName = "Plugin Template",
            pluginDescription = "A starting-point template for building custom MondoNode monitoring plugins.",
            githubUrl = "https://github.com/syshlted/mondonode-plugin-template",
            playStoreUrl = "https://play.google.com/store/apps/details?id=com.example.monitorplugin",
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        BillingGateway.teardown(this)
    }
}
