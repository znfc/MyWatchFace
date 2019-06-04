package com.google.android.clockwork.settings.provider;

import android.content.res.Resources;
import android.content.SharedPreferences;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.utils.SettingsCursor;

/**
 * Property can't just use SingleBoolProperties due to legacy reasons as query uses String to
 * represent boolean instead of 1 or 0.
 */
class MobileSignalDetectorAllowedProperties extends PreferencesProperties {
    public MobileSignalDetectorAllowedProperties(SharedPreferences prefs, Resources res) {
        super(prefs, SettingsContract.MOBILE_SIGNAL_DETECTOR_PATH);
        add(new BooleanProperty(SettingsContract.KEY_MOBILE_SIGNAL_DETECTOR,
                    res.getBoolean(R.bool.config_mobileSignalDetectorAllowed)) {
            /**
             * @inheritDoc
             * <p>
             * @return cursor with the property as either String "true" or "false"
             */
            @Override
            public void populateQuery(SettingsCursor c) {
                c.addRow(mKey, mVal);
            }
        });
    }
}
