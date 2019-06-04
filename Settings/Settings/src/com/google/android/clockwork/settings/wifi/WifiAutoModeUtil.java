package com.google.android.clockwork.settings.wifi;

import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.utils.FeatureManager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

public class WifiAutoModeUtil {

    private WifiAutoModeUtil() {}

    public static boolean getAutoWifiSetting(Context context) {
        Cursor cursor = context.getContentResolver().query(SettingsContract.AUTO_WIFI_URI,
                null, null, null, null);
        if (cursor != null) {
            try {
                while(cursor.moveToNext()) {
                    if (SettingsContract.KEY_AUTO_WIFI.equals(cursor.getString(0))) {
                        boolean isEnabled = cursor.getInt(1) == SettingsContract.AUTO_WIFI_ENABLED;
                        return isEnabled;
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return true; // Default value is Enabled.
    }

    public static void setAutoWifiSetting(Context context, boolean newValue) {
        // ignore calls which try to turn on autowifi if the override is enabled
        if (newValue && FeatureManager.INSTANCE.get(context).isAutoOverrideEnabled()) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put(SettingsContract.KEY_AUTO_WIFI, newValue ?
                SettingsContract.AUTO_WIFI_ENABLED : SettingsContract.AUTO_WIFI_DISABLED);
        context.getContentResolver().update(SettingsContract.AUTO_WIFI_URI, values, null, null);
    }
}
