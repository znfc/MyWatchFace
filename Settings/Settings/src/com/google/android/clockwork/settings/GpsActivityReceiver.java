package com.google.android.clockwork.settings;

import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.List;

public class GpsActivityReceiver extends BroadcastReceiver {
    private static final String TAG = "GpsActivity";
    private static final int[] sHighPowerRequestAppOpArray
            = new int[] {AppOpsManager.OP_MONITOR_HIGH_POWER_LOCATION};

    private static final String ACTION =
            "com.google.android.clockwork.settings.action.GPS_ACTIVITY";

    private static final String EXTRA_ACTIVE = "active";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Received intent: " + intent);
        }
        Intent returnIntent = new Intent(ACTION);
        returnIntent.setPackage("com.google.android.wearable.app");
        returnIntent.putExtra(EXTRA_ACTIVE, false);

        AppOpsManager appOpsManager =
                (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        List<AppOpsManager.PackageOps> packages
                = appOpsManager.getPackagesForOps(sHighPowerRequestAppOpArray);
        // AppOpsManager can return null when there is no requested data.
        if (packages != null) {
            final int numPackages = packages.size();
            for (int packageInd = 0; packageInd < numPackages; packageInd++) {
                AppOpsManager.PackageOps packageOp = packages.get(packageInd);
                List<AppOpsManager.OpEntry> opEntries = packageOp.getOps();
                if (opEntries != null) {
                    final int numOps = opEntries.size();
                    for (int opInd = 0; opInd < numOps; opInd++) {
                        AppOpsManager.OpEntry opEntry = opEntries.get(opInd);
                        // AppOpsManager should only return OP_MONITOR_HIGH_POWER_LOCATION because
                        // of the sHighPowerRequestAppOpArray filter, but checking defensively.
                        if (opEntry.getOp() == AppOpsManager.OP_MONITOR_HIGH_POWER_LOCATION) {
                            if (opEntry.isRunning()) {
                                returnIntent.putExtra(EXTRA_ACTIVE, true);
                                context.sendBroadcast(returnIntent);
                                if (Log.isLoggable(TAG, Log.DEBUG)) {
                                    Log.d(TAG, "Sending gps active with intent: " + returnIntent);
                                }
                                return;
                            }
                        }
                    }
                }
            }
        }
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Sending gps not active with intent: " + returnIntent);
        }
        context.sendBroadcast(returnIntent);
    }
}
