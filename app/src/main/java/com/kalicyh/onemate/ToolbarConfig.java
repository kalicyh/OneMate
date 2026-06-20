package com.kalicyh.onemate;

import android.content.SharedPreferences;

final class ToolbarConfig {
    static final String TARGET_PACKAGE = "com.samsung.android.honeyboard";
    static final String PREF_GROUP = "toolbar";
    static final String KEY_FORCE_TEXT_EDITING = "force_text_editing";
    static final String TEXT_EDITING_ID = "text_editing";

    private ToolbarConfig() {
    }

    static boolean isEnabled(SharedPreferences prefs) {
        return isTextEditingEnabled(prefs);
    }

    static boolean isTextEditingEnabled(SharedPreferences prefs) {
        return prefs != null && prefs.getBoolean(KEY_FORCE_TEXT_EDITING, false);
    }
}
