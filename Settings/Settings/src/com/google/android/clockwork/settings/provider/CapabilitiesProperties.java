package com.google.android.clockwork.settings.provider;

import android.content.res.Resources;
import android.content.SharedPreferences;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.SettingsContract;

/**
 * Helper to grab details about the device capabilities.
 */
class CapabilitiesProperties extends PreferencesProperties {
    CapabilitiesProperties(SharedPreferences prefs, Resources res) {
        super(prefs, SettingsContract.CAPABILITIES_PATH);

        addBoolean(SettingsContract.KEY_BUTTON_SET, false);
        addImmutable(SettingsContract.KEY_SIDE_BUTTON,
                res.getBoolean(R.bool.side_button_present) ? 1 : 0);
    }
}
