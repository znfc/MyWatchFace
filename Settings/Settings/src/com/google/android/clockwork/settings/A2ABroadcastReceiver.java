package com.google.android.clockwork.settings;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.clockwork.settings.utils.A2AHelper;

public class A2ABroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "A2ABroadcast";

    public static final String ACTION_A2A_RESULT_INTERNAL =
        "com.google.android.clockwork.A2AResult.internal";

    private static final String SETUPWIZARD_PACKAGE =
        "com.google.android.wearable.setupwizard";

    @Override
    public void onReceive(Context context, Intent intent) {
        // If setup isn't done, forward to the setup receiver
        if (Settings.System.getInt(
                context.getContentResolver(),
                Settings.System.SETUP_WIZARD_HAS_RUN,
                0) == 0) {
            Log.d(TAG, "forwarding to setup");
            intent.setComponent(null);
            intent.setPackage(SETUPWIZARD_PACKAGE);
            context.sendBroadcast(intent);
            return;
        }

        if (A2AHelper.ACTION_A2A_PAIRING.equals(intent.getAction())) {
            intent.setClass(context, A2AConfirmCodeActivity.class);
            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            context.startActivity(intent);
        } else if (A2AHelper.ACTION_A2A_RESULT.equals(intent.getAction())) {
            // Forward this to the active confirm activity
            intent.setAction(ACTION_A2A_RESULT_INTERNAL);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }
    }
}
