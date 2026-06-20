package com.kalicyh.onemate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

private const val RUNTIME_PREFS = "runtime"

class HiddenSettingsStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ToolbarConfig.ACTION_HIDDEN_SETTING_RUNTIME) return
        val key = intent.getStringExtra(ToolbarConfig.EXTRA_HIDDEN_SETTING_KEY) ?: return
        if (!ToolbarConfig.isKnownHiddenSetting(key)) return
        context.getSharedPreferences(RUNTIME_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(
                ToolbarConfig.runtimeEnabledPrefKey(key),
                intent.getBooleanExtra(ToolbarConfig.EXTRA_HIDDEN_SETTING_RUNTIME_ENABLED, false),
            )
            .apply()
    }
}
