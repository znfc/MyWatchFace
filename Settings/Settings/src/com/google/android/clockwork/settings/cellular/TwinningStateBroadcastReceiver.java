package com.google.android.clockwork.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.clockwork.phone.common.Constants;
import com.google.android.clockwork.settings.cellular.Utils;

/**
 * Receives twinning state broadcasts from twinning apps
 */
public class TwinningStateBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "TwinningReceiver";

    private static final String EXTRA_LEGACY_TWINNING = "state";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Constants.TWINNING_STATE_CHANGED.equals(intent.getAction())) {
            Log.w(TAG, "We do not handle: " + intent.getAction());
            return;
        }

        Log.d(TAG, "Twinning state changed");

        int calls = intent.getIntExtra(Constants.EXTRA_CALL_TWINNING, Constants.STATE_OFF);
        int texts = intent.getIntExtra(Constants.EXTRA_TEXT_TWINNING, Constants.STATE_OFF);
        int bridging = intent.getIntExtra(Constants.EXTRA_TEXT_BRIDGING, Constants.STATE_ON);

        // Backwards compatability for the old broadcast
        if (intent.hasExtra(EXTRA_LEGACY_TWINNING)) {
            calls = intent.getIntExtra(EXTRA_LEGACY_TWINNING, Constants.STATE_OFF);
            texts = calls;
            bridging = calls == Constants.STATE_ON ? Constants.STATE_OFF : Constants.STATE_ON;
        }

        Utils.setCallTwinningState(
                context,
                calls,
                intent.getStringExtra(Constants.EXTRA_OPERATOR),
                intent.getStringExtra(Constants.EXTRA_VOICEMAIL_NUMBER)
        );

        Utils.setTextTwinningState(context, texts);
        Utils.setTextBridgingState(context, bridging);
    }
}
