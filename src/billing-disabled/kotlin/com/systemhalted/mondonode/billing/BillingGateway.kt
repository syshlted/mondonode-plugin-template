package com.systemhalted.mondonode.billing

import android.app.Activity
import android.content.Intent
import android.net.Uri

object BillingGateway {
    fun launch(activity: Activity, pluginName: String, pluginDescription: String, githubUrl: String, playStoreUrl: String) {
        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl)))
        activity.finish()
    }
    fun teardown(activity: Activity) {}
}
