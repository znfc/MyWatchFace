package com.google.android.clockwork.settings.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.SharedPreferences;
import com.google.android.clockwork.settings.SettingsContract;
import java.util.function.Supplier;

/**
 * A wrapper class to support the legacy bluetooth URI.
 */
class BluetoothLegacyProperties extends BluetoothProperties {
    BluetoothLegacyProperties(SharedPreferences prefs, Supplier<ContentResolver> resolver) {
        super(prefs, resolver);
    }

    @Override
    public String getPath() {
        return SettingsContract.BLUETOOTH_MODE_PATH;
    }
}
