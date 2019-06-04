package com.google.android.clockwork.settings.provider;

import android.content.ContentValues;
import android.content.res.Resources;
import android.content.SharedPreferences;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.utils.SettingsCursor;
import java.util.Objects;

class CustomColorProperties extends PreferencesProperties {
    private Integer mCustomColorBackground;
    private Integer mCustomColorForeground;

    public CustomColorProperties(SharedPreferences prefs) {
        super(prefs, SettingsContract.CUSTOM_COLORS_PATH);
        add(new CustomColorProperty(SettingsContract.KEY_CUSTOM_COLOR_FOREGROUND));
        add(new CustomColorProperty(SettingsContract.KEY_CUSTOM_COLOR_BACKGROUND));
    }

    private class CustomColorProperty extends Property {
        Integer mVal;

        CustomColorProperty(String key) {
            super(key);
            if (mPrefs.contains(key)) {
                mVal = mPrefs.getInt(key, 0);
            }
        }

        public void populateQuery(SettingsCursor c) {
            c.addRow(mKey, mVal);
        }

        public int updateProperty(ContentValues values, SharedPreferences.Editor editor) {
            Integer newVal = values.getAsInteger(mKey);
            if (!Objects.equals(newVal, mVal)) {
                mVal = newVal;
                if (newVal == null) {
                    editor.remove(mKey);
                } else {
                    editor.putInt(mKey, newVal);
                }
                return 1;
            }
            return 0;
        }
    }
}
