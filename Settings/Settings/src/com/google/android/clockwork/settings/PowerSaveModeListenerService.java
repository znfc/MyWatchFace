package com.google.android.clockwork.settings;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.PowerManager;

/**
 * This service hosts a broadcast receiver that listens to changes in power save mode,
 * i.e. battery saver mode.
 */
public class PowerSaveModeListenerService extends Service {
    private static final String TAG = "PowerSaveModeListenerService";

    private BroadcastReceiver mPowerStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            if (PowerManager.ACTION_POWER_SAVE_MODE_CHANGED.equals(intent.getAction())) {
                PowerManager manager =
                        (PowerManager) getSystemService(Context.POWER_SERVICE);
                if (!manager.isPowerSaveMode()) {
                    return;
                }
                BatterySaverUtil.triggerLowBatteryShutdown(context);
            }
        }
    };

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        registerReceiver(mPowerStateReceiver,
                new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED));
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mPowerStateReceiver);
        super.onDestroy();
    }

}
