package com.google.android.clockwork.settings.time;

import static com.google.android.clockwork.companionrelay.Constants.RESULT_OK;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import com.android.internal.util.IndentingPrintWriter;

import com.google.android.clockwork.companionrelay.ICompanionRelayCallback;
import com.google.android.clockwork.companionrelay.Intents;
import com.google.android.clockwork.host.GKeys;
import com.google.android.clockwork.settings.Constants;
import com.google.android.clockwork.settings.DateTimeConfig;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.SettingsIntents;

import java.util.concurrent.TimeUnit;

/**
 * Synchronizes the system time between phone and watch.
 * After a success, this will try to improve on the initial result,
 * by issuing a request and accepting a response that has lower latency.
 * This continues until no latency improvement can be made within
 * {@link GKeys#GSERVICES_KEY_TIME_SYNC_IMPROVEMENT_ATTEMPTS} attempts from the last improvement,
 * or until the target {@link GKeys#GSERVICES_KEY_TIME_SYNC_CLOSE_ENOUGH_MS} latency is reached.
 */
public class PhoneTimeSyncer {
    private static final String TAG = "PhoneTimeSyncer";

    private static final long RPC_TIMEOUT = TimeUnit.SECONDS.toMillis(30);
    // If the watch is off by this much or more, correct it immediately
    private static final long VERY_INACCURATE_CLOCK_MS = TimeUnit.MINUTES.toMillis(2);

    // Allow this much difference between watch and phone clocks.
    private long mCloseEnoughMs;

    private final Context mContext;
    private final DateTimeConfig mDateTimeConfig;

    private final AlarmManager mAlarmManager;
    private final PendingIntent mTimeoutPendingIntent;

    // Best request/response roundtrip latency so far. This is the best measure of the accuracy
    // of a clock-correction offset.
    private long mBestTimeSyncLatency;

    // Highest-quality offset we have discovered so far, that has not already been applied.
    // A zero means we should not do anything.
    private long mOffsetForBestResponse;

    private boolean mAwaitingTimeSyncResponse;
    private int mImprovementAttemptCount;
    // last time that was set from the phone
    private long mLastTimeFix;

    // callback for sync success/failure
    private TimeSyncerCallback mTimeSyncerCallback;

    ICompanionRelayCallback.Stub mCompanionRelayCallback = new ICompanionRelayCallback.Stub() {
        public void onResult(int resultCode) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onResult: " + resultCode);
            }
            if (resultCode != RESULT_OK) {
                mTimeSyncerCallback.onFailure();
            } else {
                // even on success, the RPC could have failed.
                // Set a timeout and retry if it is exceeded.
                mAwaitingTimeSyncResponse = true;
                resetAlarm(RPC_TIMEOUT);
            }
        }
    };

    PhoneTimeSyncer(Context context, DateTimeConfig config, TimeSyncerCallback callback) {
        mContext = context;
        mDateTimeConfig = config;
        mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        mTimeoutPendingIntent = PendingIntent.getService(
                context,
                TimeIntents.REQ_TIME_REQUEST_TIMEOUT,
                TimeIntents.getPhoneTimeRequestTimeoutIntent(mContext),
                0);
        mTimeSyncerCallback = callback;
        resetTimeLatencyTracking();
    }

    /** Request start time sync */
    public void startUpdate() {
        sendGetCompanionTime();
    }

    /** Handle when a request for the time from the phone times out */
    /*package*/ void handleRequestTimeout() {
        if (mAwaitingTimeSyncResponse) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Timed out waiting for time sync response");
            }
            mAwaitingTimeSyncResponse = false;
            mTimeSyncerCallback.onFailure();
        }
    }

    /** Reset the improvement latency and all the counters **/
    /*package*/ void resetTimeLatencyTracking() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Resetting Time Syncer improvement latency/counters");
        }
        mAwaitingTimeSyncResponse = false;
        mBestTimeSyncLatency = GKeys.GSERVICES_KEY_TIME_SYNC_MAX_LATENCY_MS.get();
        mCloseEnoughMs = GKeys.GSERVICES_KEY_TIME_SYNC_CLOSE_ENOUGH_MS.get();
        mOffsetForBestResponse = 0;
        mImprovementAttemptCount = 0;
    }

    /** Cancel timeout alarm for phone time RPC */
    /*package*/ void cancelTimeoutAlarm() {
        mAlarmManager.cancel(mTimeoutPendingIntent);
    }

    /**
     * Cancel old alarm and start a new one for the input interval.
     *
     * @param interval in milliseconds when to trigger the alarm, starting from now.
     */
    private void resetAlarm(long interval) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Resetting alarm for " + interval + " ms from now");
        }
        cancelTimeoutAlarm();
        long now = SystemClock.elapsedRealtime();
        long next = now + interval;
        mAlarmManager.set(AlarmManager.ELAPSED_REALTIME, next, mTimeoutPendingIntent);
    }

    /**
     * Send get companion time request to Home, which will relay the message to the
     * Companion app and then relay the response back to us
     */
    private void sendGetCompanionTime() {
        Bundle data = new Bundle();
        data.putInt(Constants.FIELD_COMMAND, Constants.RPC_GET_COMPANION_TIME);
        data.putLong(Constants.FIELD_HOME_TIME, SystemClock.elapsedRealtime());

        Bundle callback = new Bundle();
        callback.putBinder(Intents.KEY_CALLBACK_BINDER, mCompanionRelayCallback);

        Intent relayIntent =
                Intents.getRelayRpcIntent(Constants.PATH_RPC_WITH_FEATURE, data, callback);
        mContext.startService(relayIntent);
    }

    /**
     * If this is the first success this session, or an improvement over earlier
     * results this session, then update the time.
     * Maybe retry failures, and maybe try to improve on successes.
     */
    public void handleTimeSyncResponse(final long requestTicks, final long companionTime) {
        mAwaitingTimeSyncResponse = false;
        cancelTimeoutAlarm();

        if (mDateTimeConfig.getClockworkAutoTimeMode() == SettingsContract.AUTO_TIME_OFF) {
            return;
        }

        final long currentTicks = SystemClock.elapsedRealtime();
        final long latency = currentTicks - requestTicks;
        final long offset = companionTime + latency / 2 - System.currentTimeMillis();
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "received response - "
                    + "companionTime: " + companionTime
                    + ", requestTicks: " + requestTicks
                    + ", currentTicks: " + currentTicks
                    + ", latency ms: " + latency
                    + ", offset ms: " + offset);
        }
        final long failureLatency = GKeys.GSERVICES_KEY_TIME_SYNC_MAX_LATENCY_MS.get();
        if (latency > failureLatency) {
            // Grossly excessive latency is considered to be an RPC failure.
            Log.w(TAG, "latency " + latency + " greater than max " + failureLatency);
            mTimeSyncerCallback.onFailure();
        } else {
            // This was not an error or a grossly delayed response.
            if (latency < mBestTimeSyncLatency) {
                // Only accept further improvements.
                mBestTimeSyncLatency = latency;
                // Whenever we improve, reset the improvement counter.
                mImprovementAttemptCount = 0;
                mOffsetForBestResponse = offset;

                if (Math.abs(mOffsetForBestResponse) > VERY_INACCURATE_CLOCK_MS) {
                    // The time is very inaccurate, so update it immediately, without waiting for
                    // the final drift correction. That way the user will be happier with their
                    // wearable a few seconds sooner.
                    Log.i(TAG, "Immediately correcting big offset ms: " + mOffsetForBestResponse);
                    updateTime();
                }
            }
            if (shouldMakeTimeSyncRequest()) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Attempting to improve sync latency");
                }
                mImprovementAttemptCount++;
                sendGetCompanionTime();
            } else {
                // We are done trying to improve. Accept the best offset found so far,
                // unless it is too small to bother with.
                if (Math.abs(mOffsetForBestResponse) > mCloseEnoughMs) {
                    updateTime();
                } else {
                    Log.i(TAG, "Not correcting small offset ms: " + mOffsetForBestResponse);
                }
                mTimeSyncerCallback.onSuccess();
            }
        }
    }

    /**
     * Updates the system clock, and broadcasts that to any interested listeners.
     */
    private void updateTime() {
        if (mDateTimeConfig.getClockworkAutoTimeMode() != SettingsContract.AUTO_TIME_OFF) {
            final long newTime = System.currentTimeMillis() + mOffsetForBestResponse;
            mContext.startService(SettingsIntents.getSetTimeIntent(
                    newTime, SystemClock.elapsedRealtime()));
            Log.i(TAG, "UPDATED TIME to ms " + newTime
                    + ", offset ms: " + mOffsetForBestResponse
                    + ", latency ms: " + mBestTimeSyncLatency);
            mOffsetForBestResponse = 0;
            mLastTimeFix = newTime;
        }
    }

    /**
     * Returns true if we are giving ourselves more chances to improve, and we have not yet hit
     * the target round-trip latency of {@link #mCloseEnoughMs}.
     */
    private boolean shouldMakeTimeSyncRequest() {
        return mImprovementAttemptCount < GKeys.GSERVICES_KEY_TIME_SYNC_IMPROVEMENT_ATTEMPTS.get()
                && mBestTimeSyncLatency > mCloseEnoughMs;
    }

    void dump(IndentingPrintWriter pw) {
        pw.println("PhoneTimeSyncer");
        pw.increaseIndent();
        pw.print("mAwaitingTimeSyncResponse="); pw.println(mAwaitingTimeSyncResponse);
        pw.print("mBestTimeSyncLatency="); pw.println(mBestTimeSyncLatency);
        pw.print("mImprovementAttemptCount="); pw.println(mImprovementAttemptCount);
        pw.print("mLastTimeFix="); pw.println(mLastTimeFix);
        pw.print("mOffsetForBestResponse="); pw.println(mOffsetForBestResponse);
        pw.decreaseIndent();
        pw.println();
    }
}
