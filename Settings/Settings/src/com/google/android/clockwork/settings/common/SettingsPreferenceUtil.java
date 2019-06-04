package com.google.android.clockwork.settings.common;

import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilities related to Android Preference.
 */
public final class SettingsPreferenceUtil {
    private SettingsPreferenceUtil() {}

    /**
     * Find all the {@link Preference} in {@param prefGroup} that match {@param key} and remove
     * them from {@param prefGroup}
     * @return {@link PreferenceGroup} for chaining.
     */
    public static PreferenceGroup removeAllPrefsWithKey(PreferenceGroup prefGroup, String key) {
        if (prefGroup == null || TextUtils.isEmpty(key)) {
            return prefGroup;
        }

        // First figure out all the prefs that match the criteria.
        List<Preference> prefsToRemove = new ArrayList<>();
        for (int i = 0, count = prefGroup.getPreferenceCount(); i < count; i++) {
            Preference pref = prefGroup.getPreference(i);
            if ((pref.getKey() != null) && (pref.getKey().startsWith(key))) {
                prefsToRemove.add(pref);
            }
        }

        // Remove all of them
        for (int i = 0, count = prefsToRemove.size(); i < count; i++) {
            prefGroup.removePreference(prefsToRemove.get(i));
        }

        return prefGroup;
    }
}
