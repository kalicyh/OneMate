package com.kalicyh.onemate;

import android.content.SharedPreferences;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

final class ToolbarConfig {
    static final String TARGET_PACKAGE = "com.samsung.android.honeyboard";
    static final String PREF_GROUP = "toolbar";
    static final String KEY_ENABLED = "enabled";
    static final String KEY_FORCE_TEXT_EDITING = "force_text_editing";
    static final String KEY_EXTRA_IDS = "extra_ids";
    static final String TEXT_EDITING_ID = "text_editing";

    private ToolbarConfig() {
    }

    static boolean isEnabled(SharedPreferences prefs) {
        return prefs != null && prefs.getBoolean(KEY_ENABLED, false);
    }

    static boolean isTextEditingEnabled(SharedPreferences prefs) {
        return prefs != null && prefs.getBoolean(KEY_FORCE_TEXT_EDITING, false);
    }

    static Set<String> forcedIds(SharedPreferences prefs) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        if (isTextEditingEnabled(prefs)) {
            ids.add(TEXT_EDITING_ID);
        }
        if (prefs != null) {
            ids.addAll(parseIds(prefs.getString(KEY_EXTRA_IDS, "")));
        }
        return Collections.unmodifiableSet(ids);
    }

    static Set<String> parseIds(String raw) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        if (raw == null) {
            return ids;
        }
        for (String part : raw.split("[,\\s]+")) {
            String id = part.trim();
            if (!id.isEmpty()) {
                ids.add(id);
            }
        }
        return ids;
    }
}
