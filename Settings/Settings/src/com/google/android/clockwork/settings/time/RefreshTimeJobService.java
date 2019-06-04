package com.google.android.clockwork.settings.time;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.net.Network;
import android.util.Log;
import android.util.NtpTrustedTime;

public class RefreshTimeJobService extends JobService {
    private static final String TAG = NetworkTimeSyncer.TAG;
    private Context mContext;
    private JobParameters mParams;
    private Thread mSyncTimeThread;

    @Override
    public boolean onStartJob(JobParameters params) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "job started");
        }

        mContext = this;
        mParams = params;

        mSyncTimeThread = createTimeSyncThread();
        mSyncTimeThread.start();

        // the spawned thread is responsible for finishing the job
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "stopping job");
        }
        if (mSyncTimeThread != null) {
            mSyncTimeThread.interrupt();
        }
        return true;
    }

    private Thread createTimeSyncThread() {
        final NtpTrustedTime trustedTime = NtpTrustedTime.getInstance(mContext);
        Network network = mParams.getNetwork();
        long freshnessThreshold = mParams.getExtras().getLong(
                NetworkTimeSyncer.FRESHNESS_THRESHOLD);
        int driftThreshold = mParams.getExtras().getInt(NetworkTimeSyncer.DRIFT_THRESHOLD);

        return new Thread(() -> {
            try {
                RefreshTimeCallable refreshTimeCallable = new RefreshTimeCallable(
                        DefaultClock.INSTANCE,
                        new TrustedTimeProxy(trustedTime),
                        network,
                        freshnessThreshold,
                        driftThreshold);

                if (refreshTimeCallable.call()) {
                    refreshSuccess();
                } else {
                    refreshFailure();
                }
            } catch (Exception e) {
                Log.w(TAG, "Exception syncing time: " + e.getMessage(), e);
                refreshFailure();
            }
        });
    }

    private void refreshSuccess() {
        jobFinished(mParams, false);
        mContext.startService(TimeIntents.getNetworkTimeRefreshIntent(mContext));
    }

    private void refreshFailure() {
        jobFinished(mParams, true);
    }
}
