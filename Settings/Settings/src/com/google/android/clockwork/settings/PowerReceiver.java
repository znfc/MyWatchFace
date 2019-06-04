package com.google.android.clockwork.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.TextUtils;
import com.google.android.clockwork.battery.wear.PowerIntents;
import com.google.android.clockwork.power.PowerSettingsManager;

/**
 * Receiver to expose power functionality to home.
 */
public class PowerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (TextUtils.equals(PowerIntents.ACTION_SLEEP, intent.getAction())) {
            PowerManager powerManager = (PowerManager) context.getSystemService(
                    Context.POWER_SERVICE);
            powerManager.goToSleep(SystemClock.uptimeMillis());
        } else if (TextUtils.equals(PowerIntents.ACTION_ENABLE_MULTICORE, intent.getAction())) {
            PowerSettingsManager.getOrCreate(context).update(true);
        } else if (TextUtils.equals(PowerIntents.ACTION_DISABLE_MULTICORE, intent.getAction())) {
            PowerSettingsManager.getOrCreate(context).update(false);
        }
    }
}
