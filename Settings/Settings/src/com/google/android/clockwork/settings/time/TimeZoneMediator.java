package com.google.android.clockwork.settings.time;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;

import com.google.android.clockwork.common.suppliers.LazyContextSupplier;
import com.google.android.clockwork.phone.Utils;
import com.google.android.clockwork.settings.DateTimeConfig;
import com.google.android.clockwork.settings.DefaultDateTimeConfig;
import com.google.android.clockwork.settings.SettingsContract;

import java.util.concurrent.TimeUnit;
import java.util.TimeZone;

/**
 * Mediate time zone updates as they come in from different sources
 */
public class TimeZoneMediator {
    private static final String TAG = "TimeZoneMediator";

    public static final String EXTRA_TIME_AT_SEND = "extra-time-at-send";

    private final AlarmManager mAlarmManager;
    private final DateTimeConfig mDateTimeConfig;
    private Context mContext;

    private final boolean mCellularCapable;
    private final NetworkTimeZoneSyncer mNetworkTimeZoneSyncer;

    private boolean mIsCheckTimeZoneAlarmSet;
    // details about the last NITZ update received
    private TimeZoneUpdate mLastNitzUpdate;
    // details about the last phone update received
    private TimeZoneUpdate mLastPhoneUpdate;
    // amount of time we'll wait to check for phone updates before using NITZ
    private long mPhoneTimeUpdateBuffer = TimeUnit.MINUTES.toMillis(5);

    public static final LazyContextSupplier<TimeZoneMediator> INSTANCE =
            new LazyContextSupplier<>(
                    appContext -> new TimeZoneMediator(
                            appContext,
                            Utils.isCurrentDeviceCellCapable(appContext),
                            appContext.getSystemService(AlarmManager.class),
                            DefaultDateTimeConfig.INSTANCE.get(appContext)),
                    TAG);

    @VisibleForTesting
    TimeZoneMediator(
            Context context,
            boolean cellularCapable,
            AlarmManager alarmManager,
            DateTimeConfig dateTimeConfig) {
        mContext = context;
        mIsCheckTimeZoneAlarmSet = false;
        mCellularCapable = cellularCapable;
        mNetworkTimeZoneSyncer = new NetworkTimeZoneSyncer(context);
        mAlarmManager = alarmManager;
        mDateTimeConfig = dateTimeConfig;
    }

    /** Start service to get NITZ timezone update */
    public void startNitzService() {
        if (mCellularCapable) {
            mNetworkTimeZoneSyncer.startNitzService();
        }
    }

    /** Cancel all pending alarms and reset state trackers */
    public void cancelPendingTasks() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Resetting TimeZoneMediator");
        }
        if (mCellularCapable) {
            mAlarmManager.cancel(createTimeZoneCheckIntent(0));
            mIsCheckTimeZoneAlarmSet = false;
            mNetworkTimeZoneSyncer.stopNitzService();
        }
    }

    /**
     * Add a timezone update from the phone. This type of update is assumed to be accurate,
     * and the system timezone is immediately set from it.
     */
    public void addPhoneUpdate(String timeZone) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Adding phone update");
        }
        mLastPhoneUpdate = new TimeZoneUpdate(
                timeZone, SystemClock.elapsedRealtime(), System.currentTimeMillis());
        if (isAutoTimeZoneSyncingEnabled()) {
            Log.i(TAG, "Setting timezone from phone");
            setCurrentTimeZone(timeZone);
        }
    }

    /**
     * Add a NITZ update from the cellular provider. This is only set if phone time updates are
     * stale.
     */
    public void addNitzUpdate(String timeZone) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Adding NITZ update");
        }
        mLastNitzUpdate = new TimeZoneUpdate(
                timeZone, SystemClock.elapsedRealtime(), System.currentTimeMillis());
        if (isAutoTimeZoneSyncingEnabled()) {
            long currentTime = SystemClock.elapsedRealtime();
            if (isNitzUpdateNeeded(timeZone, currentTime - mPhoneTimeUpdateBuffer)) {
                // wait some time to see if a new update comes in from the phone before updating the
                // timezone with NITZ
                setTimeZoneCheckAlarm();
            }
        }
    }

    @VisibleForTesting TimeZone getCurrentTimeZone() {
        return TimeZone.getDefault();
    }

    private void setCurrentTimeZone(String timeZone) {
        mAlarmManager.setTimeZone(timeZone);
    }

    private boolean isNitzUpdateNeeded(String timeZoneId, long comparisonTime) {
        TimeZone newTimeZone = TimeZone.getTimeZone(timeZoneId);
        TimeZone currentTimeZone = getCurrentTimeZone();
        long currentTime = System.currentTimeMillis();

        // if the timezones are the same or have the same offset, there's no need to update
        boolean isSameTimeZone = currentTimeZone.getID().equals(timeZoneId) ||
                newTimeZone.getOffset(currentTime) == currentTimeZone.getOffset(currentTime);

        // if we've received a timezone update from the phone since the comparison time, assume that
        // it's fresh and there's no need to update
        boolean isPhoneUpdateFresh = mLastPhoneUpdate != null &&
                mLastPhoneUpdate.updateElapsedTime > comparisonTime;

        return !isSameTimeZone && !isPhoneUpdateFresh;
    }

    private void setTimeZoneCheckAlarm() {
        // if the alarm is already set, don't overwrite it
        if (!mIsCheckTimeZoneAlarmSet) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Checking timezone in " + mPhoneTimeUpdateBuffer + " ms");
            }
            long triggerAt = SystemClock.elapsedRealtime() + mPhoneTimeUpdateBuffer;
            PendingIntent intent = createTimeZoneCheckIntent(SystemClock.elapsedRealtime());
            mAlarmManager.set(AlarmManager.ELAPSED_REALTIME, triggerAt, intent);
            mIsCheckTimeZoneAlarmSet = true;
        }
    }

    private PendingIntent createTimeZoneCheckIntent(long systemTime) {
        Intent checkTimeZoneIntent = TimeIntents.getCheckTimeZoneIntent(mContext);
        checkTimeZoneIntent.putExtra(EXTRA_TIME_AT_SEND, systemTime);

        return PendingIntent.getService(
                mContext, TimeIntents.REQ_CHECK_TIME_ZONE, checkTimeZoneIntent, 0);
    }

    /** Handle a request to check the system time zone after a NITZ update */
    public void handleCheckTimeZone(long timeAtSend) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Checking timezone");
        }
        mIsCheckTimeZoneAlarmSet = false;
        if (isAutoTimeZoneSyncingEnabled() && mLastNitzUpdate != null) {
            // if we've received a timezone update from the phone since the alarm was set,
            // assume that it's fresh and there's no need to update
            if (isNitzUpdateNeeded(mLastNitzUpdate.timeZoneId, timeAtSend)) {
                Log.i(TAG, "Setting timezone from NITZ");
                setCurrentTimeZone(mLastNitzUpdate.timeZoneId);
            }
        }
    }

    private boolean isAutoTimeZoneSyncingEnabled() {
        return mDateTimeConfig.getClockworkAutoTimeZoneMode()
                != SettingsContract.AUTO_TIME_ZONE_OFF;
    }

    public void dump(IndentingPrintWriter pw) {
        pw.println("TimeZoneMediator");
        pw.increaseIndent();
        pw.print("mCellularCapable="); pw.println(mCellularCapable);
        pw.print("mIsCheckTimeZoneAlarmSet="); pw.println(mIsCheckTimeZoneAlarmSet);
        pw.print("mLastNitzUpdate="); pw.println(mLastNitzUpdate);
        pw.print("mLastPhoneUpdate="); pw.println(mLastPhoneUpdate);
        pw.print("mPhoneTimeUpdateBuffer="); pw.println(mPhoneTimeUpdateBuffer);
        pw.decreaseIndent();
        pw.println();
    }

    private class TimeZoneUpdate {
        // time zone Olson ID
        private String timeZoneId;
        // ms since system boot when the update was received
        private long updateElapsedTime;
        // system wall time when the update was received (unreliable for comparison)
        private long updateWallTime;

        private TimeZoneUpdate(String timeZoneId, long updateElapsedTime, long updateWallTime) {
            this.timeZoneId = timeZoneId;
            this.updateElapsedTime = updateElapsedTime;
            this.updateWallTime = updateWallTime;
        }

        @Override
        public String toString() {
            return "TimeZoneUpdate{" +
                    "timeZoneId='" + timeZoneId + '\'' +
                    ", updateElapsedTime=" + updateElapsedTime +
                    ", updateWallTime=" + updateWallTime +
                    '}';
        }
    }
}
