package com.kalicyh.onemate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AqBodyDataReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ToolbarConfig.ACTION_AQ_BODY_DATA) return
        val prefs = context.getSharedPreferences(HealthConnectSync.PREFS, Context.MODE_PRIVATE)
        val expectedToken = prefs.getString(ToolbarConfig.KEY_AQ_BODY_TOKEN, "").orEmpty()
        val actualToken = intent.getStringExtra(ToolbarConfig.EXTRA_AQ_BODY_TOKEN).orEmpty()
        if (expectedToken.isBlank() || actualToken != expectedToken) return
        val response = intent.getStringExtra(ToolbarConfig.EXTRA_AQ_BODY_RESPONSE).orEmpty()
        if (response.isBlank()) return

        val pending = goAsync()
        HealthConnectSync.saveAndMaybeSync(context.applicationContext, response) {
            pending.finish()
        }
    }
}
