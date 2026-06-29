package com.kalicyh.onemate;

import android.content.SharedPreferences;

final class ToolbarConfig {
    static final String TARGET_PACKAGE = "com.samsung.android.honeyboard";
    static final String AQ_PACKAGE = "com.antgroup.aijk.android";
    static final String MEDIA_PROVIDER_AUTHORITY = "com.kalicyh.onemate.media";
    static final String PREF_GROUP = "toolbar";
    static final String ACTION_AQ_BODY_DATA =
            "com.kalicyh.onemate.action.AQ_BODY_DATA";
    static final String EXTRA_AQ_BODY_RESPONSE = "response";
    static final String EXTRA_AQ_BODY_TOKEN = "token";
    static final String KEY_AQ_BODY_TOKEN = "aq_body_token";
    static final String ACTION_HIDDEN_SETTING_RUNTIME =
            "com.kalicyh.onemate.action.HIDDEN_SETTING_RUNTIME";
    static final String EXTRA_HIDDEN_SETTING_KEY = "key";
    static final String EXTRA_HIDDEN_SETTING_RUNTIME_ENABLED = "enabled";
    static final String KEY_FORCE_TEXT_EDITING = "force_text_editing";
    static final String KEY_FORCE_RECENT_MEDIA = "force_recent_media";
    static final String KEY_DISABLE_TOOLBAR_BADGES = "disable_toolbar_badges";
    static final String TEXT_EDITING_ID = "onemate_text_editing";
    static final String RECENT_MEDIA_ID = "onemate_recent_media";
    static final String[] HIDDEN_SETTING_KEYS = {
            "SETTINGS_SHOW_SMS_OTP",
            "SETTINGS_SPECIFIC_ASSIST",
            "SETTINGS_TOUCH_EVENT_RECORD",
            "SETTINGS_WRITING_ASSIST",
            "SETTINGS_DRAWING_ASSIST",
            "SETTINGS_WRITING_ASSIST_TRANSLATION",
            "SETTINGS_VOICE_INPUT",
            "SETTINGS_DEFAULT_HWR_ON",
            "settings_direct_writing",
            "SETTINGS_SAVE_SCREENSHOTS_TO_CLIPBOARD",
            "SETTINGS_PHYSICAL_KEYBOARD_TOOLBAR",
            "SETTINGS_SHOW_BUTTON_TO_HIDE_KEYBOARD_RELATIVE_LINK",
            "japanese_input_options",
            "enhanced_prediction",
            "selected_language_download_cue",
    };

    private ToolbarConfig() {
    }

    static boolean isEnabled(SharedPreferences prefs) {
        return isTextEditingEnabled(prefs) || isRecentMediaEnabled(prefs);
    }

    static boolean isTextEditingEnabled(SharedPreferences prefs) {
        return prefs != null && prefs.getBoolean(KEY_FORCE_TEXT_EDITING, false);
    }

    static boolean isRecentMediaEnabled(SharedPreferences prefs) {
        return prefs != null && prefs.getBoolean(KEY_FORCE_RECENT_MEDIA, false);
    }

    static boolean areToolbarBadgesDisabled(SharedPreferences prefs) {
        return prefs != null && prefs.getBoolean(KEY_DISABLE_TOOLBAR_BADGES, false);
    }

    static String hiddenSettingPrefKey(String preferenceKey) {
        return "show_setting_" + preferenceKey;
    }

    static String runtimeEnabledPrefKey(String preferenceKey) {
        return "runtime_enabled_" + preferenceKey;
    }

    static boolean isHiddenSettingEnabled(SharedPreferences prefs, String preferenceKey) {
        return prefs != null && prefs.getBoolean(hiddenSettingPrefKey(preferenceKey), false);
    }

    static boolean shouldForceSettingsPreference(SharedPreferences prefs, Object preferenceKey) {
        if (!(preferenceKey instanceof String)) {
            return false;
        }
        String key = (String) preferenceKey;
        if (isKnownHiddenSetting(key)) {
            return isHiddenSettingEnabled(prefs, key);
        }
        switch (key) {
            case "SETTINGS_TOUCH_EVENT_RECORD_CATEGORY":
                return isHiddenSettingEnabled(prefs, "SETTINGS_TOUCH_EVENT_RECORD");
            case "SETTINGS_CATEGORY_VOICE_INPUT":
                return isHiddenSettingEnabled(prefs, "SETTINGS_VOICE_INPUT")
                        || isHiddenSettingEnabled(prefs, "SETTINGS_DEFAULT_HWR_ON")
                        || isHiddenSettingEnabled(prefs, "settings_direct_writing")
                        || isHiddenSettingEnabled(prefs, "SETTINGS_SAVE_SCREENSHOTS_TO_CLIPBOARD")
                        || isHiddenSettingEnabled(prefs, "SETTINGS_WRITING_ASSIST_TRANSLATION");
            case "SETTINGS_SHOW_BUTTON_TO_HIDE_KEYBOARD_RELATIVE_LINK_CATEGORY":
                return isHiddenSettingEnabled(prefs, "SETTINGS_SHOW_BUTTON_TO_HIDE_KEYBOARD_RELATIVE_LINK");
            case "regional_language_settings":
                return isHiddenSettingEnabled(prefs, "japanese_input_options")
                        || isHiddenSettingEnabled(prefs, "enhanced_prediction")
                        || isHiddenSettingEnabled(prefs, "selected_language_download_cue");
            default:
                return false;
        }
    }

    static boolean isKnownHiddenSetting(String preferenceKey) {
        if (preferenceKey == null) {
            return false;
        }
        for (String key : HIDDEN_SETTING_KEYS) {
            if (key.equals(preferenceKey)) {
                return true;
            }
        }
        return false;
    }
}
