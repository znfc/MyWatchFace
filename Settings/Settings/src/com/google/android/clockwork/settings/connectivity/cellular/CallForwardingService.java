package com.google.android.clockwork.settings.connectivity.cellular;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.format.DateUtils;
import android.util.Log;

import com.google.common.base.Preconditions;

/**
 * Helper service to set and get call forwarding settings.
 *
 * The cellular settings typically run in the phone process
 * and do not have access to the system settings which are
 * accessed via the system server process.
 */
public class CallForwardingService extends IntentService {
    private static final String TAG = CallForwardingService.class.getSimpleName();

    public static final String EXTRA_RESULT_RECEIVER = "result_receiver";
    public static final String EXTRA_BUNDLE = "bundle";

    public static final String ACTION_GET_CALL_FORWARDING_STATE
            = "com.google.android.clockwork.settings.ACTION_GET_CALL_FORWARDING_STATE";
    public static final String ACTION_SET_CALL_FORWARDING_STATE
            = "com.google.android.clockwork.settings.ACTION_SET_CALL_FORWARDING_STATE";
    public static final String CALL_FORWARDING_ACTION
            = "com.google.android.clockwork.settings.CALL_FORWARDING_ACTION";
    public static final String CALL_FORWARDING_DATE
            = "com.google.android.clockwork.settings.CALL_FORWARDING_DATE";

    public CallForwardingService() {
        super(TAG);
    }

     /**
     * Runs on worker thread.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        final ResultReceiver receiver = intent.getParcelableExtra(EXTRA_RESULT_RECEIVER);
        final Bundle b = intent.getParcelableExtra(EXTRA_BUNDLE);
        Preconditions.checkNotNull(b);
        switch (intent.getAction()) {
            case ACTION_GET_CALL_FORWARDING_STATE:
                b.putInt(CALL_FORWARDING_ACTION,
                        CallForwardingUtils.getLastRequestedForwardingAction(
                                getApplicationContext()));
                String date = DateUtils.formatDateTime(getApplicationContext(),
                        CallForwardingUtils.getLastRequestedForwardingTime(getApplicationContext()),
                        DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_NO_YEAR);
                b.putString(CALL_FORWARDING_DATE, date);
                break;
            case ACTION_SET_CALL_FORWARDING_STATE:
                final int action = b.getInt(CALL_FORWARDING_ACTION);
                CallForwardingUtils.setLastRequestedForwardingAction(getApplicationContext(),
                        action);
                break;
        }
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, " action " + intent.getAction() + " bundle: " + b);
        }
        if (receiver != null) {
            receiver.send(Activity.RESULT_OK, b);
        }
    }
 }
