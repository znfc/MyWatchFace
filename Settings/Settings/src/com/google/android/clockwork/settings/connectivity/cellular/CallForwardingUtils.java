package com.google.android.clockwork.settings.connectivity.cellular;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.clockwork.common.content.CwPrefs;
import com.google.android.clockwork.settings.DefaultSettingsContentResolver;
import com.google.android.clockwork.settings.SettingsContentResolver;
import com.google.android.clockwork.settings.SettingsContract;

public class CallForwardingUtils {
    private static final String CALL_FORWARDING_PREFERENCES =
            "com.google.android.clockwork.settings.cellular.callforwarding";
    private static final String SHARED_PREF_LAST_CALL_FORWARD_ACTION_TIME =
            "last_call_forward_action_time";

    public static int getLastRequestedForwardingAction(Context context) {
        SettingsContentResolver settings = new DefaultSettingsContentResolver(
                context.getContentResolver());
        return settings.getIntValueForKey(SettingsContract.LAST_CALL_FORWARD_ACTION_URI,
                SettingsContract.KEY_LAST_CALL_FORWARD_ACTION,
                SettingsContract.CALL_FORWARD_NO_LAST_ACTION);

    }

    public static long getLastRequestedForwardingTime(Context context) {
        SharedPreferences prefs = CwPrefs.wrap(context, CALL_FORWARDING_PREFERENCES);
        return prefs.getLong(SHARED_PREF_LAST_CALL_FORWARD_ACTION_TIME, 0);
    }

    public static void setLastRequestedForwardingAction(Context context, int action) {
        SettingsContentResolver settings = new DefaultSettingsContentResolver(
                context.getContentResolver());

        SharedPreferences prefs = CwPrefs.wrap(context, CALL_FORWARDING_PREFERENCES);
        SharedPreferences.Editor editor = prefs.edit();
        settings.putIntValueForKey(SettingsContract.LAST_CALL_FORWARD_ACTION_URI,
                SettingsContract.KEY_LAST_CALL_FORWARD_ACTION, action);
        editor.putLong(SHARED_PREF_LAST_CALL_FORWARD_ACTION_TIME,
                System.currentTimeMillis());
        editor.apply();
    }
}
