package com.google.android.clockwork.settings.time;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.SystemClock;
import android.util.EventLog;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.common.suppliers.LazyContextSupplier;
import com.google.android.clockwork.phone.Utils;
import com.google.android.clockwork.settings.DefaultDateTimeConfig;
import com.google.android.clockwork.settings.EventLogTags;

import java.util.concurrent.TimeUnit;

/**
 * Manages automatic time syncing on the device. Maintains a poller that attempts to sync time every
 * {@link #POLL_PERIOD_MS}. Initially, time sync with {@link PhoneTimeSyncer} is attempted.
 * If this fails {@link #MAX_FAILURE_RETRY_COUNT} times, then syncing through
 * {@link NetworkTimeSyncer} and {@link NitzTimeSyncer} is attempted as well. Once one of the time
 * syncers succeeds, {@link NetworkTimeSyncer} and {@link NitzTimeSyncer} are stopped and
 * the {@link #POLL_PERIOD_MS} poller will be maintained.
 */
public class TimeSyncManager {
    private static final String TAG = "TimeSyncManager";

    private final Context mContext;
    private NetworkTimeSyncer mNetworkTimeSyncer;
    private NitzTimeSyncer mNitzTimeSyncer;
    private PhoneTimeSyncer mPhoneTimeSyncer;

    private static final long POLL_PERIOD_MS = TimeUnit.DAYS.toMillis(1);
    @VisibleForTesting
    static final int MAX_FAILURE_RETRY_COUNT = 5;

    private final AlarmManager mAlarmManager;
    private final PendingIntent mPendingPollIntent;

    private long mLastSyncLoopSuccess;
    private long mLastSyncLoopFailure;
    // number of syncs attempted without a success
    private int mFailureRetryCount;

    public static final LazyContextSupplier<TimeSyncManager> INSTANCE =
            new LazyContextSupplier<>(TimeSyncManager::new, TAG);

    @VisibleForTesting
    TimeSyncManager(Context context) {
        mContext = context;

        mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        mPendingPollIntent = PendingIntent.getService(
                mContext,
                TimeIntents.REQ_POLL_PHONE_TIME,
                TimeIntents.getPollPhoneTimeIntent(mContext),
                0);
        mFailureRetryCount = 0;

        int ntpFreshnessThreshold = context.getResources().getInteger(
                R.integer.ntp_freshness_threshold);
        int driftThreshold = context.getResources().getInteger(
                R.integer.ntp_threshold);

        mNetworkTimeSyncer = new NetworkTimeSyncer(
                context,
                ntpFreshnessThreshold,
                driftThreshold,
                DefaultDateTimeConfig.INSTANCE.get(context),
                new NetworkTimeSyncer.JobInfoBuilderFactory(context),
                mTimeSyncerCallback);

        mNitzTimeSyncer = new NitzTimeSyncer(context, Utils.isCurrentDeviceCellCapable(context));

        mPhoneTimeSyncer = new PhoneTimeSyncer(
                context,
                DefaultDateTimeConfig.INSTANCE.get(context),
                mTimeSyncerCallback);
    }

    /** Cancel all pending alarms and jobs and reset state trackers */
    public void cancelPendingTasks() {
        mFailureRetryCount = 0;
        mPhoneTimeSyncer.resetTimeLatencyTracking();
        cancelPollAlarm();
        mPhoneTimeSyncer.cancelTimeoutAlarm();
        stopFallbacks();
    }

    /** Handle when the network time is successfully refreshed */
    public void handleNetworkTimeRefresh() {
        mNetworkTimeSyncer.handleRefresh();
    }

    /** Handle when a request for the time from the phone times out */
    public void handlePhoneTimeRequestTimeout() {
        mPhoneTimeSyncer.handleRequestTimeout();
    }

    /** Set the system clock to NTP time */
    public void setNtpTime() {
        mNetworkTimeSyncer.setNtpTime();
    }

    /** Initiate a time update from the phone */
    public void startPhoneTimeUpdate() {
        mPhoneTimeSyncer.startUpdate();
    }

    /** Handle incoming time information from the phone */
    public void handlePhoneTimeSyncResponse(final long requestTicks, final long companionTime) {
        mPhoneTimeSyncer.handleTimeSyncResponse(requestTicks, companionTime);
    }

    @VisibleForTesting
    void setPhoneTimeSyncer(PhoneTimeSyncer phoneTimeSyncer) {
        mPhoneTimeSyncer = phoneTimeSyncer;
    }

    @VisibleForTesting
    void setNetworkTimeSyncer(NetworkTimeSyncer networkTimeSyncer) {
        mNetworkTimeSyncer = networkTimeSyncer;
    }

    @VisibleForTesting
    void setNitzTimeSyncer(NitzTimeSyncer nitzTimeSyncer) {
        mNitzTimeSyncer = nitzTimeSyncer;
    }

    /**
     * Cancel alarm to poll the phone time and schedule a new one
     *
     * @param interval in milliseconds when to trigger the alarm, starting from now
     */
    private void resetAlarm(long interval) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Resetting alarm for " + interval + " ms from now");
        }
        cancelPollAlarm();
        long now = SystemClock.elapsedRealtime();
        long next = now + interval;
        mAlarmManager.set(AlarmManager.ELAPSED_REALTIME, next, mPendingPollIntent);
    }

    private void cancelPollAlarm() {
        mAlarmManager.cancel(mPendingPollIntent);
    }

    private void startFallbacks() {
        mNetworkTimeSyncer.scheduleRefreshTimeJob();
        mNitzTimeSyncer.startNitzService();
    }

    private void stopFallbacks() {
        mNetworkTimeSyncer.cancelRefreshTimeJob();
        mNitzTimeSyncer.stopNitzService();
    }

    @VisibleForTesting
    TimeSyncerCallback mTimeSyncerCallback = new TimeSyncerCallback() {
        @Override
        public void onSuccess() {
            // when syncing succeeds, either via the phone or network, default back to syncing
            // from the phone
            mLastSyncLoopSuccess = System.currentTimeMillis();
            mFailureRetryCount = 0;
            mPhoneTimeSyncer.resetTimeLatencyTracking();
            resetAlarm(POLL_PERIOD_MS);
            stopFallbacks();
        }

        @Override
        public void onFailure() {
            EventLog.writeEvent(EventLogTags.TIME_SYNC_FAILURE, mFailureRetryCount + 1);
            mLastSyncLoopFailure = System.currentTimeMillis();
            if (mFailureRetryCount < MAX_FAILURE_RETRY_COUNT) {
                // Delay by 3*2^n minutes (retry at 3, 6, 12, 24, 48 minutes)
                final long delayMs =
                        TimeUnit.MINUTES.toMillis(3) * (long) Math.pow(2, mFailureRetryCount);
                Log.i(TAG, "rescheduling failed time sync for " + delayMs + " ms from now");
                resetAlarm(delayMs);
                mFailureRetryCount++;
            } else {
                Log.e(TAG, "Too many failure retries: " + mFailureRetryCount + " - stop retrying.");
                mFailureRetryCount = 0;
                mPhoneTimeSyncer.resetTimeLatencyTracking();
                resetAlarm(POLL_PERIOD_MS);
                startFallbacks();
            }
        }
    };

    public void dump(IndentingPrintWriter pw) {
        pw.println("TimeSyncManager");
        pw.increaseIndent();
        pw.print("mFailureRetryCount="); pw.println(mFailureRetryCount);
        pw.print("mLastSyncLoopFailure="); pw.println(mLastSyncLoopFailure);
        pw.print("mLastSyncLoopSuccess="); pw.println(mLastSyncLoopSuccess);
        pw.decreaseIndent();
        pw.println();
        mPhoneTimeSyncer.dump(pw);
        mNetworkTimeSyncer.dump(pw);
        mNitzTimeSyncer.dump(pw);
    }
}
