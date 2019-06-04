package com.google.android.clockwork.settings.provider;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Binder;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.utils.SettingsCursor;
import com.google.common.base.Preconditions;

/**
 * A helper class to read and write settings associated with ambient mode.
 */
class AmbientProperties extends PreferencesProperties {
    private static final String TAG = "AmbientProperties";

    /** System property to determine whether ambient mode is disabled. */
    private static final String PROP_DISABLE_AMBIENT = "persist.sys.disable_ambient";

    /** System property to force ambient when docked. */
    private static final String PROP_FORCE_WHEN_DOCKED = "ro.ambient.force_when_docked";

    /** System property storing the gesture sensor id. */
    private static final String PROP_GESTURE_SENSOR_ID = "ro.ambient.gesture_sensor_id";

    /** System property storing whether low bit mode is enabled. */
    private static final String PROP_LOW_BIT_ENABLED = "ro.ambient.low_bit_enabled";

    /** System property for the time duration before ambient times out when plugged in. */
    private static final String PROP_PLUGGED_TIMEOUT_MIN = "ro.ambient.plugged_timeout_min";

    /** Default gesture sensor id. */
    private static final int DEFAULT_GESTURE_SENSOR_ID = 0;

    AmbientProperties(Context context, SharedPreferences prefs, Resources res) {
        super(prefs, SettingsContract.AMBIENT_CONFIG_PATH);
        mContext = Preconditions.checkNotNull(context);

        add(new AmbientEnabledProperty());

        addBoolean(SettingsContract.KEY_AMBIENT_LOW_BIT_ENABLED_DEV, false);
        addBoolean(SettingsContract.KEY_AMBIENT_TILT_TO_WAKE,
                res.getBoolean(R.bool.config_tiltToWakeDefaultEnabled));
        addBoolean(SettingsContract.KEY_AMBIENT_TOUCH_TO_WAKE,
                res.getBoolean(R.bool.config_touchToWakeDefaultEnabled));

        addImmutable(SettingsContract.KEY_AMBIENT_FORCE_WHEN_DOCKED,
                SystemProperties.getBoolean(PROP_FORCE_WHEN_DOCKED, false) ? 1 : 0);
        addImmutable(SettingsContract.KEY_AMBIENT_GESTURE_SENSOR_ID,
                SystemProperties.getInt(PROP_GESTURE_SENSOR_ID, DEFAULT_GESTURE_SENSOR_ID));
        addImmutable(SettingsContract.KEY_AMBIENT_LOW_BIT_ENABLED,
                SystemProperties.getBoolean(PROP_LOW_BIT_ENABLED, false));
        addImmutable(SettingsContract.KEY_AMBIENT_PLUGGED_TIMEOUT_MIN,
                SystemProperties.getInt(PROP_PLUGGED_TIMEOUT_MIN, -1));
    }

    private final Context mContext;

    private class AmbientEnabledProperty extends Property {
        public AmbientEnabledProperty() {
            super(SettingsContract.KEY_AMBIENT_ENABLED);
        }

        @Override
        public void populateQuery(SettingsCursor c) {
            c.addRow(mKey, !SystemProperties.getBoolean(PROP_DISABLE_AMBIENT, false) ? 1 : 0);
        }

        @Override
        public int updateProperty(ContentValues values, SharedPreferences.Editor editor) {
            long token = Binder.clearCallingIdentity();
            try {
                final boolean enabled = PropertiesPreconditions.checkBoolean(values, mKey);
                Log.d(TAG, "ambient enabled:" + enabled);
                Settings.Secure.putInt(mContext.getContentResolver(),
                        Settings.Secure.DOZE_ENABLED,
                        enabled ? 1 : 0);
                SystemProperties.set(PROP_DISABLE_AMBIENT, String.valueOf(!enabled));
            } finally {
                Binder.restoreCallingIdentity(token);
            }
            return 1;
        }
    }
}
