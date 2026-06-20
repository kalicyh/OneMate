package com.kalicyh.onemate;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

public final class ToolbarConfigTest {
    @Test
    public void defaultsToDisabledWithoutRemotePrefs() {
        assertFalse(ToolbarConfig.isEnabled(null));
        assertFalse(ToolbarConfig.isTextEditingEnabled(null));
    }
}
