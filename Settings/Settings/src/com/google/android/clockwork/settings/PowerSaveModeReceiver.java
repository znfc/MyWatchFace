package com.google.android.clockwork.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import com.google.android.clockwork.battery.wear.BatteryIntents;

/**
 * This receiver captures when home wants to enable/disable battery saver.
 */
public class PowerSaveModeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (BatteryIntents.ACTION_ENABLE_POWER_SAVE_MODE.equals(intent.getAction())) {
            PowerManager powerManager =
                    (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            BatterySaverUtil.startBatterySaver(
                    !powerManager.isPowerSaveMode(), context, powerManager);
        }
    }
}
