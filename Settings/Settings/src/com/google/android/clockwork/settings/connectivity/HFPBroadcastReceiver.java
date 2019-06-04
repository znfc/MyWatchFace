package com.google.android.clockwork.settings.connectivity;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.SettingsIntents;

/**
 * This receiver handles external requests to enable/disable the HFP client profile.
 */
public class HFPBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "HFPBroadcastReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onReceive: " + intent);
        }
        if (SettingsIntents.ACTION_ENABLE_HFP.equals(intent.getAction())
                && intent.hasExtra(SettingsIntents.EXTRA_HFP_ENABLED)) {
            ContentValues values = new ContentValues();
            final boolean enabled =
                    intent.getBooleanExtra(SettingsIntents.EXTRA_HFP_ENABLED, false);

            if (intent.getBooleanExtra(SettingsIntents.EXTRA_SET_BY_USER, false)) {
                final int setting = enabled
                        ? SettingsContract.HFP_CLIENT_ENABLED
                        : SettingsContract.HFP_CLIENT_DISABLED;

                values.put(SettingsContract.KEY_USER_HFP_CLIENT_SETTING, setting);
            } else {
                values.put(SettingsContract.KEY_HFP_CLIENT_PROFILE_ENABLED, enabled);

            }
            context.getContentResolver().update(
                    SettingsContract.BLUETOOTH_URI, values, null /*where*/, null /*selectionArgs*/);
        }
    }
}
