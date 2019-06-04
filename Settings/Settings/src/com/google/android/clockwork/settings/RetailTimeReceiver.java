package com.google.android.clockwork.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.text.format.Time;

public class RetailTimeReceiver extends BroadcastReceiver {
    private static final String ACTION_FINISHED_RETAIL_DREAM =
            "com.google.android.clockwork.home.retail.action.FINISHED_RETAIL_DREAM";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_FINISHED_RETAIL_DREAM.equals(intent.getAction())) {
            setRetailTime(context);
        }
    }

    public static void setRetailTime(Context context) {
        context.startService(SettingsIntents.getSetTimeIntent(
                computeRetailTime(), SystemClock.elapsedRealtime()));
    }

    /**
     * Always set retail time to 10:10:00 on 2/2/17, in UTC to prevent Home app
     * from waiting for the correct time in retail mode.
     */
    private static long computeRetailTime() {
        Time time = new Time("UTC");
        time.set(0, 10, 10, 2, 1, 2017);
        return time.normalize(true);
    }
}
