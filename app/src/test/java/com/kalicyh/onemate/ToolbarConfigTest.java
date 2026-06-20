package com.kalicyh.onemate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.LinkedHashSet;

import org.junit.Test;

public final class ToolbarConfigTest {
    @Test
    public void defaultsToDisabledWithoutRemotePrefs() {
        assertFalse(ToolbarConfig.isEnabled(null));
        assertFalse(ToolbarConfig.isTextEditingEnabled(null));
    }

    @Test
    public void parseIds_acceptsCommaWhitespaceAndDeduplicates() {
        assertEquals(
                new LinkedHashSet<>(Arrays.asList("text_editing", "edit_toolbar", "kbd_handwriting")),
                ToolbarConfig.parseIds(" text_editing, edit_toolbar\nkbd_handwriting text_editing "));
    }
}
