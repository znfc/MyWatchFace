package com.google.android.clockwork.settings.time;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.NtpTrustedTime;

import com.android.internal.util.IndentingPrintWriter;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.DateTimeConfig;
import com.google.android.clockwork.settings.DefaultSettingsContentResolver;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.common.JobIds;
import com.google.android.clockwork.settings.system.DateTimeSettingsHelper;

/**
 * Manages a job to get the network time from an SNTP server. If the system time is out of sync,
 * updates it.
 */
public class NetworkTimeSyncer {
    /*package*/ static final String TAG = "NetworkTimeSyncer";

    /*package*/ static final String FRESHNESS_THRESHOLD = "freshness-threshold";
    /*package*/ static final String DRIFT_THRESHOLD = "drift-threshold";

    private Context mContext;
    // System Clock drift threshold
    private final int mDriftThreshold;
    private final DateTimeConfig mDateTimeConfig;
    private final JobInfoBuilderFactory mJobInfoBuilderFactory;
    // The age threshold for the NTP cache before it is forced to update
    private final long mNtpFreshnessThreshold;
    // Try-again polling interval, in case the network request failed
    private final long mRetryBackoffMs;
    // last time that was set from a trusted time source
    private long mLastTimeFix;
    // callback for sync success/failure
    private TimeSyncerCallback mTimeSyncerCallback;
    private boolean systemRunning;

    NetworkTimeSyncer(
            Context context,
            int ntpFreshnessThreshold,
            int driftThreshold,
            DateTimeConfig dateTimeConfig,
            JobInfoBuilderFactory jobInfoBuilder,
            TimeSyncerCallback callback) {
        systemRunning = false;

        mContext = context;
        mDateTimeConfig = dateTimeConfig;
        mJobInfoBuilderFactory = jobInfoBuilder;
        mNtpFreshnessThreshold = ntpFreshnessThreshold;
        mDriftThreshold = driftThreshold;
        mRetryBackoffMs = mContext.getResources().getInteger(
                R.integer.ntp_polling_interval_shorter);
        mTimeSyncerCallback = callback;
    }

    /** Schedule a JobScheduler job to refresh the network time */
    public void scheduleRefreshTimeJob() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Scheduling RefreshTimeJob");
        }

        JobInfo.Builder builder = mJobInfoBuilderFactory.invoke();
        builder.setMinimumLatency(0);
        builder.setBackoffCriteria(mRetryBackoffMs, JobInfo.BACKOFF_POLICY_EXPONENTIAL);

        PersistableBundle extras = new PersistableBundle();
        extras.putLong(FRESHNESS_THRESHOLD, mNtpFreshnessThreshold);
        extras.putInt(DRIFT_THRESHOLD, mDriftThreshold);
        builder.setExtras(extras);

        JobScheduler jobScheduler = mContext.getSystemService(JobScheduler.class);
        jobScheduler.schedule(builder.build());
    }

    /** Cancel the job to refresh the network time */
    public void cancelRefreshTimeJob() {
        JobScheduler jobScheduler =
                (JobScheduler) mContext.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancel(JobIds.JOB_ID_NETWORK_TIME_SYNC);
    }

    /** Handle when the network time is successfully refreshed */
    /*package*/ void handleRefresh() {
        mTimeSyncerCallback.onSuccess();
    }

    /** Set the system clock to NTP time */
    /*package*/ void setNtpTime() {
        if (mDateTimeConfig.getClockworkAutoTimeMode() != SettingsContract.AUTO_TIME_OFF
                && !DateTimeSettingsHelper.isAltMode(
                        new DefaultSettingsContentResolver(mContext.getContentResolver()))) {
            // We only set NTP time on exit airplane mode for watches not paired with an iOS
            // companion
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Enabling NTP time updates.");
            }
            NtpTrustedTime trustedTime = NtpTrustedTime.getInstance(mContext);
            if (trustedTime.getCacheAge() >=
                    mContext.getResources().getInteger(R.integer.ntp_polling_interval)) {
                if (!trustedTime.forceRefresh()) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "NTP time sync failed");
                    }
                    return;
                }
            }
            long currentTime = trustedTime.currentTimeMillis();
            SystemClock.setCurrentTimeMillis(currentTime);
            mLastTimeFix = currentTime;
            mTimeSyncerCallback.onSuccess();
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "NTP time successfully set");
            }
        }
    }

    void dump(IndentingPrintWriter pw) {
        pw.println("NetworkTimeSyncer");
        pw.increaseIndent();
        pw.print("systemRunning="); pw.println(systemRunning);
        pw.print("mDriftThreshold="); pw.println(mDriftThreshold);
        pw.print("mLastTimeFix="); pw.println(mLastTimeFix);
        pw.print("mNtpFreshnessThreshold="); pw.println(mNtpFreshnessThreshold);
        pw.print("mRetryBackoffMs="); pw.println(mRetryBackoffMs);
        pw.decreaseIndent();
        pw.println();
    }

    static class JobInfoBuilderFactory {
        private Context mContext;

        JobInfoBuilderFactory(Context context) {
            mContext = context;
        }

        public JobInfo.Builder invoke() {
            JobInfo.Builder builder = new JobInfo.Builder(JobIds.JOB_ID_NETWORK_TIME_SYNC,
                    new ComponentName(mContext, RefreshTimeJobService.class));

            // require access to the Internet through WiFi or Cellular
            NetworkRequest.Builder networkBuilder = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);

            boolean isAltMode = DateTimeSettingsHelper.isAltMode(
                    new DefaultSettingsContentResolver(mContext.getContentResolver()));

            if (!isAltMode) {
                // iOS does not support sending datagram packets over bluetooth
                // see b/32663274
                networkBuilder.addTransportType(NetworkCapabilities.TRANSPORT_BLUETOOTH);
            }

            builder.setRequiredNetwork(networkBuilder.build());
            builder.setEstimatedNetworkBytes(48, 48); // each NTP packet is 48 bytes

            return builder;
        }
    }
}
