package com.google.android.clockwork.settings.provider;

import android.content.ContentValues;
import android.content.SharedPreferences;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.utils.SettingsCursor;

class WristGesturesPrefExistsProperties extends PreferencesProperties {
    public WristGesturesPrefExistsProperties(SharedPreferences prefs) {
        super(prefs, SettingsContract.WRIST_GESTURES_ENABLED_PREF_EXISTS_PATH);

        add(new Property(SettingsContract.KEY_WRIST_GESTURES_ENABLED_PREF_EXISTS) {
            @Override
            public void populateQuery(SettingsCursor c) {
                c.addRow(SettingsContract.KEY_WRIST_GESTURES_ENABLED_PREF_EXISTS,
                        mPrefs.contains(SettingsContract.KEY_WRIST_GESTURES_ENABLED) ? 1 : 0);
            }

            @Override
            public int updateProperty(ContentValues values, SharedPreferences.Editor editor) {
                // updates not supported as value updated automatically
                throw new UnsupportedOperationException(
                        "WRIST_GESTURES_ENABLED_PREF_EXISTS is updated automatically");
            }
        });
    }
}
