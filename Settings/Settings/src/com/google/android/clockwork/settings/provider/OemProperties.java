package com.google.android.clockwork.settings.provider;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.support.annotation.VisibleForTesting;
import com.google.android.clockwork.common.setup.wearable.Constants;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.utils.SettingsCursor;

/**
 * Collection of settings values related to the OEM config state.
 */
class OemProperties extends PreferencesProperties {
    @VisibleForTesting static final String PREF_OEM_SETUP_VERSION = "oem_setup_version";

    private Integer mVersion;

    public OemProperties(SharedPreferences prefs) {
        super(prefs, SettingsContract.OEM_PATH);
        if (prefs.contains(PREF_OEM_SETUP_VERSION)) {
            mVersion = prefs.getInt(PREF_OEM_SETUP_VERSION, -1);
        }
    }

    @Override
    public SettingsCursor query() {
        SettingsCursor c = super.query();

        if (mVersion != null) {
            c.addRow(SettingsContract.KEY_OEM_SETUP_VERSION, mVersion);
        }

        c.addRow(SettingsContract.KEY_OEM_SETUP_CURRENT,
                mVersion != null && Constants.OEM_DATA_VERSION == mVersion
                ? SettingsContract.VALUE_TRUE
                : SettingsContract.VALUE_FALSE);

        return c;
    }

    @Override
    public int update(ContentValues values) {
        int newVal = PropertiesPreconditions.checkInt(
                values, SettingsContract.KEY_OEM_SETUP_VERSION);
        if (mVersion != null && newVal == mVersion
                && mPrefs.contains(SettingsContract.KEY_OEM_SETUP_VERSION)) {
            return super.update(values);
        } else {
            mVersion = newVal;
            mPrefs.edit().putInt(SettingsContract.KEY_OEM_SETUP_VERSION, mVersion).apply();
            return super.update(values) + 1;
        }
    }
}
