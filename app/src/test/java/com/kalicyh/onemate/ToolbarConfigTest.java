package com.kalicyh.onemate;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ToolbarConfigTest {
    @Test
    public void defaultsToDisabledWithoutRemotePrefs() {
        assertFalse(ToolbarConfig.isEnabled(null));
        assertFalse(ToolbarConfig.isTextEditingEnabled(null));
        assertFalse(ToolbarConfig.areToolbarBadgesDisabled(null));
    }

    @Test
    public void hiddenKeyboardSettingsAreKnownButDisabledWithoutPrefs() {
        assertTrue(ToolbarConfig.isKnownHiddenSetting("SETTINGS_SHOW_SMS_OTP"));
        assertTrue(ToolbarConfig.isKnownHiddenSetting("SETTINGS_SPECIFIC_ASSIST"));
        assertTrue(ToolbarConfig.isKnownHiddenSetting("SETTINGS_WRITING_ASSIST"));
        assertFalse(ToolbarConfig.isKnownHiddenSetting("use_developer_options"));
        assertFalse(ToolbarConfig.isKnownHiddenSetting("SETTINGS_WRITING_ASSIST_UNKNOWN"));
        assertFalse(ToolbarConfig.shouldForceSettingsPreference(null, "use_developer_options"));
        assertFalse(ToolbarConfig.shouldForceSettingsPreference(null, null));
    }
}
