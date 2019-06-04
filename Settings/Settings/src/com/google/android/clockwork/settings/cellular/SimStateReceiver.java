package com.google.android.clockwork.settings.cellular;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.PhoneConstants;

public class SimStateReceiver extends BroadcastReceiver {
    private static final String TAG = SimStateNotification.TAG;

    @Override
    public void onReceive(Context context, Intent intent) {
        // Cancel any previous notifications
        SimStateNotification.cancelNotification(context);

        String iccState = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
        String lockedReason = intent.getStringExtra(IccCardConstants.INTENT_KEY_LOCKED_REASON);
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "ICC state: " + iccState);
            Log.d(TAG, "Locked reason: " + lockedReason);
        }

        final int simState = SimStateNotification.translateSimState(iccState, lockedReason);
        if (SimStateNotification.shouldCreateNotification(simState)) {
            SimStateNotification.createNotification(context, simState);
        }
    }
}
