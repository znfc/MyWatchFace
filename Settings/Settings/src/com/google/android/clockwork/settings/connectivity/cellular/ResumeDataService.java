package com.google.android.clockwork.settings.connectivity.cellular;

import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.INetworkPolicyManager;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.util.Log;

import com.google.common.base.Preconditions;

/**
 * Helper service to resume cellular data service.
 *
 * Many cellular settings run in the phone process and do
 *  not have proper resource access, e.g. system settings,
 *  or granted proper permissions, e.g. network policy.
 *  This intent service is used to offload proper functionality
 *  from cellular settings running in the phone process.
 */
public class ResumeDataService extends IntentService {
    private static final String TAG = ResumeDataService.class.getSimpleName();

    public static final String EXTRA_BUNDLE = "bundle";
    public static final String EXTRA_RESULT_RECEIVER = "result_receiver";

    public static final String SUBSCRIBER_ID = "subscriber_id";
    public static final String MERGED_SUBSCRIBER_IDS = "merged_subscriber_ids";

    public static final String ACTION_RESUME_DATA
            = "com.google.android.clockwork.settings.connectivity.cellular.ACTION_RESUME_DATA";

    public static final int RESUME_DATA_FAIL = Activity.RESULT_FIRST_USER + 1;

    public ResumeDataService() {
        super(TAG);
    }

     /**
     * Runs on worker thread.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        final ResultReceiver receiver = intent.getParcelableExtra(EXTRA_RESULT_RECEIVER);
        final Bundle b = intent.getParcelableExtra(EXTRA_BUNDLE);
        int resultCode = Activity.RESULT_OK;
        Preconditions.checkNotNull(b);
        switch (intent.getAction()) {
            case ACTION_RESUME_DATA:
                final String subscriberId = b.getString(ResumeDataService.SUBSCRIBER_ID);
                final String[] mergedSubscriberIds = b.getStringArray(
                        ResumeDataService.MERGED_SUBSCRIBER_IDS);

                final INetworkPolicyManager policyService =
                    INetworkPolicyManager.Stub.asInterface(
                            ServiceManager.getService(Context.NETWORK_POLICY_SERVICE));
                final NetworkTemplate template = NetworkTemplate.normalize(
                        NetworkTemplate.buildTemplateMobileAll(subscriberId), mergedSubscriberIds);
                try {
                    policyService.snoozeLimit(template);
                } catch (RemoteException e) {
                    Log.w(TAG, "problem resuming data", e);
                    resultCode = RESUME_DATA_FAIL;
                }
                break;
        }
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, " action " + intent.getAction() + " bundle: " + b);
        }
        if (receiver != null) {
            receiver.send(resultCode, b);
        }
    }
 }
