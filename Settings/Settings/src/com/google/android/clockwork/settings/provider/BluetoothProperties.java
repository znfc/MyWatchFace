package com.google.android.clockwork.settings.provider;

import android.bluetooth.BluetoothProfile;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import com.google.android.clockwork.settings.cellular.Utils;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.utils.SettingsCursor;
import java.util.function.Supplier;

/**
 * A helper class to retrieve and modify bluetooth properties.
 */
class BluetoothProperties extends PreferencesProperties {
    private static final int HEADSET_CLIENT_BIT = 1 << BluetoothProfile.HEADSET_CLIENT;
    private static final String TAG = "BluetoothProp";

    private final Supplier<ContentResolver> mResolver;

    BluetoothProperties(SharedPreferences prefs, Supplier<ContentResolver> resolver) {
        super(prefs, SettingsContract.BLUETOOTH_PATH);
        mResolver = resolver;
    }

    @Override
    public SettingsCursor query() {
        SettingsCursor c = new SettingsCursor();

        c.addRow(SettingsContract.KEY_COMPANION_ADDRESS,
                mPrefs.getString(SettingsContract.KEY_COMPANION_ADDRESS,""));
        c.addRow(SettingsContract.KEY_BLUETOOTH_MODE,
                mPrefs.getInt(SettingsContract.KEY_BLUETOOTH_MODE,
                        SettingsContract.BLUETOOTH_MODE_UNKNOWN));

        int userHfpClientSetting = SettingsContract.HFP_CLIENT_UNSET;
        if (mPrefs.contains(SettingsContract.KEY_USER_HFP_CLIENT_SETTING)) {
            userHfpClientSetting =
                    mPrefs.getBoolean(SettingsContract.KEY_USER_HFP_CLIENT_SETTING, false)
                    ? SettingsContract.HFP_CLIENT_ENABLED
                    : SettingsContract.HFP_CLIENT_DISABLED;
        }
        c.addRow(SettingsContract.KEY_USER_HFP_CLIENT_SETTING, userHfpClientSetting);
        c.addRow(SettingsContract.KEY_HFP_CLIENT_PROFILE_ENABLED,
                isHfpClientProfileEnabled() ? 1 : 0);

        return c;
    }

    @Override
    public int update(ContentValues values) {
        final boolean twinningEnabled = Utils.isCallTwinningEnabled(mResolver.get());

        if (values.containsKey(SettingsContract.KEY_COMPANION_ADDRESS)) {
            String existingAddress = mPrefs.getString(SettingsContract.KEY_COMPANION_ADDRESS, "");
            String companionAddress = values.getAsString(SettingsContract.KEY_COMPANION_ADDRESS);
            if (companionAddress == null || companionAddress.isEmpty()) {
                Log.d(TAG, "invalid companion address update: " + companionAddress);
                return 0;
            }
            if (companionAddress.equals(existingAddress)) {
                return 0;
            }

            mPrefs.edit().putString(SettingsContract.KEY_COMPANION_ADDRESS,
                    values.getAsString(SettingsContract.KEY_COMPANION_ADDRESS)).apply();

            return 1;
        }

        if (values.containsKey(SettingsContract.KEY_BLUETOOTH_MODE)) {
            int existingMode = mPrefs.getInt(SettingsContract.KEY_BLUETOOTH_MODE,
                    SettingsContract.BLUETOOTH_MODE_UNKNOWN);
            int bluetoothMode = PropertiesPreconditions.checkInt(
                    values, SettingsContract.KEY_BLUETOOTH_MODE);
            if (bluetoothMode != SettingsContract.BLUETOOTH_MODE_NON_ALT &&
                bluetoothMode != SettingsContract.BLUETOOTH_MODE_ALT) {
                Log.w(TAG, "invalid bluetooth mode update: " + bluetoothMode);
                return 0;
            }
            if (bluetoothMode == existingMode) {
                return 0;
            }

            mPrefs.edit().putInt(SettingsContract.KEY_BLUETOOTH_MODE, bluetoothMode).apply();
            return 1;
        } else if (values.containsKey(SettingsContract.KEY_USER_HFP_CLIENT_SETTING)) {
            if (twinningEnabled) {
                // cannot modify bluetooth headset as the user when twinning is enabled.
                return 0;
            }

            int enabled = PropertiesPreconditions.checkInt(
                    values, SettingsContract.KEY_USER_HFP_CLIENT_SETTING);

            switch (enabled) {
                case SettingsContract.HFP_CLIENT_ENABLED:
                    return enableHfpClientProfile(true, true);
                case SettingsContract.HFP_CLIENT_DISABLED:
                    return enableHfpClientProfile(false, true);
                default:
                    return 0;
            }
        } else if (values.containsKey(SettingsContract.KEY_HFP_CLIENT_PROFILE_ENABLED)) {
            // If twinning is enabled, always turn off HFP.
            boolean enable = twinningEnabled
                    ? false
                    : PropertiesPreconditions.checkBoolean(
                            values, SettingsContract.KEY_HFP_CLIENT_PROFILE_ENABLED);
            return enableHfpClientProfile(enable, false);
        }
        return 0;
    }

    private boolean isHfpClientProfileEnabled() {
        final long disabledProfileSetting = Settings.Global.getLong(
                mResolver.get(), Settings.Global.BLUETOOTH_DISABLED_PROFILES, 0);
        return (disabledProfileSetting & HEADSET_CLIENT_BIT) == 0;
    }

    private int enableHfpClientProfile(boolean enable, boolean fromUser) {
        final long disabledProfileSetting = Settings.Global.getLong(mResolver.get(),
                Settings.Global.BLUETOOTH_DISABLED_PROFILES, 0);
        long modifiedSetting = disabledProfileSetting;
        final boolean currentEnabled = isHfpClientProfileEnabled();

        if (enable && !currentEnabled) {
            modifiedSetting ^= HEADSET_CLIENT_BIT;
        } else if (!enable && currentEnabled) {
            modifiedSetting |= HEADSET_CLIENT_BIT;
        }

        if (modifiedSetting != disabledProfileSetting) {
            Settings.Global.putLong(mResolver.get(),
                    Settings.Global.BLUETOOTH_DISABLED_PROFILES, modifiedSetting);

            if (fromUser) {
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putBoolean(SettingsContract.KEY_USER_HFP_CLIENT_SETTING, enable);
                editor.commit();
            }

            return 1;
        }

        return 0;
    }

    @Override
    public void notifyChange(ContentResolver resolver, Uri uri) {
        // Notify any listeners that the data backing the content provider has changed
        resolver.notifyChange(SettingsContract.BLUETOOTH_URI, null);

        // For legacy references to bluetooth_mode_uri.
        resolver.notifyChange(SettingsContract.BLUETOOTH_MODE_URI, null);

        if (!SettingsContract.BLUETOOTH_URI.equals(uri)
                && !SettingsContract.BLUETOOTH_MODE_URI.equals(uri)) {
            Log.w(TAG, "unexpected URI matched for Bluetooth properties: " + uri);
            super.notifyChange(resolver, uri);
        }
    }
}
