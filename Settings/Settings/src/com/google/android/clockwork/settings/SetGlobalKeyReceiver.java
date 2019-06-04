package com.google.android.clockwork.settings;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

/**
 * Sets use of the power key.
 */
public class SetGlobalKeyReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final boolean hasSideButton = CapabilitiesConfig.getInstance(context).hasSideButton();

        context.getPackageManager().setComponentEnabledSetting(new ComponentName(context,
                        "com.google.android.clockwork.settings.SetupSettingsActivity"),
                hasSideButton
                    ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    : PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);

        ContentValues values = new ContentValues(1);
        values.put(SettingsContract.KEY_BUTTON_SET, true /* value */);
        context.getContentResolver().update(SettingsContract.CAPABILITIES_URI,
                values, null, null);
    }
}
