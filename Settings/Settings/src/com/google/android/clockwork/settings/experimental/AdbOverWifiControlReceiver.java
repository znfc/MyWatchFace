package com.google.android.clockwork.settings.experimental;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.google.android.clockwork.settings.AdbUtil;

/**
 * Experimental control of adb over wifi setting.
 */
public class AdbOverWifiControlReceiver extends BroadcastReceiver {
    private static final String TAG = "settings";

    private static final String ACTION_ENABLE_ADB_OVER_WIFI =
            "com.google.android.clockwork.settings.experimental.action.ENABLE_ADB_OVER_WIFI";
    private static final String ACTION_DISABLE_ADB_OVER_WIFI =
            "com.google.android.clockwork.settings.experimental.action.DISABLE_ADB_OVER_WIFI";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!isBuildTypeUserdebugOrEng()) {
            throw new SecurityException("Experimental control for adb over wifi is only supported on eng and userdebug");
        }
        if (ACTION_ENABLE_ADB_OVER_WIFI.equals(intent.getAction())) {
            Log.d(TAG, "AdbOverWifiControlReceiver: enable adb over wifi");
            AdbUtil.toggleWifiDebugging(context, true);
        } else if (ACTION_DISABLE_ADB_OVER_WIFI.equals(intent.getAction())) {
            Log.d(TAG, "AdbOverWifiControlReceiver: disable adb over wifi");
            AdbUtil.toggleWifiDebugging(context, false);
        }
    }

    private static boolean isBuildTypeUserdebugOrEng() {
        return Build.TYPE.equals("userdebug") || Build.TYPE.equals("eng");
    }
}
