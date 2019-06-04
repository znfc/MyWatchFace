package com.google.android.clockwork.settings.time;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.TimeUtils;

import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

import com.google.android.clockwork.settings.DateTimeConfig;
import com.google.android.clockwork.settings.DefaultDateTimeConfig;
import com.google.android.clockwork.settings.SettingsContract;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * A service used by NetworkTimeSyncer and NetworkTimeZoneSyncer to retrieve NITZ time updates from
 * the cellular provider and update the system time and time zone on the watch. This service must
 * run in the phone process in order to get the default phone object, access the commands interface
 * of the phone object, and set the handler to get NITZ time updates.
 *
 * There must be something running in the phone process to receive NITZ time updates, and must be
 * running all the time in order to keep the handler thread running to receive those updates.
 * This needed to be fixed for Nemo factory ROM; this service is the current solution (b/24730567).
 *
 * TODO: We may be able to use core Android's service state trackers (on which this service is
 * based) for setting the NITZ time and time zone. If that can be implemented, this service would
 * no longer be needed.
 */
public class NitzTimeUpdateService extends Service {
    private static final String TAG = "NitzTimeUpdateService";

    static final String EXTRA_SYNC_TIME = "extra-sync-time";
    static final int NITZ_TIME_OFF = 0;
    static final int NITZ_TIME_ON = 1;

    private static final int EVENT_NITZ_TIME = 1;
    private static final int NITZ_TIME_NOT_SET = -1;
    private static final long ONE_HOUR_MS = TimeUnit.HOURS.toMillis(1);

    private DateTimeConfig mDateTimeConfig;

    // Wake lock used when setting time of day
    private PowerManager.WakeLock mWakeLock;
    private static final String WAKELOCK_TAG = TAG;
    private int syncTime;

    private CommandsInterface mRilCommandsInterface = null;
    private Handler mHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        mDateTimeConfig = DefaultDateTimeConfig.INSTANCE.get(this);
        syncTime = NITZ_TIME_OFF;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Started NITZ time update service");
        }
        Phone phone = PhoneFactory.getDefaultPhone();
        mRilCommandsInterface = phone.mCi;

        if (mWakeLock == null) {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG);
        }

        if (mHandler == null) {
            HandlerThread thread = new HandlerThread(TAG);
            thread.start();
            mHandler = new NitzTimeHandler(thread.getLooper());
        }

        mRilCommandsInterface.setOnNITZTime(mHandler, EVENT_NITZ_TIME, null);

        int callerSyncTime = intent.getIntExtra(EXTRA_SYNC_TIME, NITZ_TIME_NOT_SET);
        if (callerSyncTime > NITZ_TIME_NOT_SET) {
            syncTime = callerSyncTime;
        }

        boolean needTimeUpdates =
                mDateTimeConfig.getClockworkAutoTimeMode() != SettingsContract.AUTO_TIME_OFF
                        && syncTime == NITZ_TIME_ON;
        boolean needTimeZoneUpdates =
                mDateTimeConfig.getClockworkAutoTimeZoneMode()
                        != SettingsContract.AUTO_TIME_ZONE_OFF;

        if (!needTimeUpdates && !needTimeZoneUpdates) {
            // This service itself decides if it needs to stop based on the values of clockwork's
            // auto time mode and auto time zone mode. NetworkTimeSyncer and NetworkTimeZoneSyncer
            // can request this service to start or stop at any time, but we do not want to always
            // actually stop the service, because there are situations where when one syncer stops,
            // the other syncer will still need this service to be running.
            stopSelf();
        }

        // Run the service until it is explicitly stopped
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't want anyone actually binding to the service
        return null;
    }

    @Override
    public void onDestroy() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Stopping NITZ time update service");
        }
        mRilCommandsInterface.unSetOnNITZTime(mHandler);
        if (mHandler != null) {
            mHandler.getLooper().quitSafely();
            mHandler = null;
        }
        super.onDestroy();
    }

    /**
     * Sets the system time from the NITZ string provided by the cellular provider.
     *
     * nitzReceiveTime is time_t that the NITZ time was posted
     */
    private void setTimeFromNITZString (String nitz, long nitzReceiveTime) {
        // "yy/mm/dd,hh:mm:ss(+/-)tz"
        // tz is in number of quarter-hours
        long start = SystemClock.elapsedRealtime();
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "NITZ: " + nitz + "," + nitzReceiveTime +
                    " start=" + start + " delay=" + (start - nitzReceiveTime));
        }

        try {
            // NITZ time (hour:min:sec) will be in UTC but it supplies the timezone offset as well
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

            c.clear();
            c.set(Calendar.DST_OFFSET, 0);

            String[] nitzSubs = nitz.split("[/:,+-]");

            int year = 2000 + Integer.parseInt(nitzSubs[0]);
            c.set(Calendar.YEAR, year);

            // month is 0 based!
            int month = Integer.parseInt(nitzSubs[1]) - 1;
            c.set(Calendar.MONTH, month);

            int date = Integer.parseInt(nitzSubs[2]);
            c.set(Calendar.DATE, date);

            int hour = Integer.parseInt(nitzSubs[3]);
            c.set(Calendar.HOUR, hour);

            int minute = Integer.parseInt(nitzSubs[4]);
            c.set(Calendar.MINUTE, minute);

            int second = Integer.parseInt(nitzSubs[5]);
            c.set(Calendar.SECOND, second);

            if (mDateTimeConfig.getClockworkAutoTimeZoneMode()
                    != SettingsContract.AUTO_TIME_ZONE_OFF) {
                boolean sign = (nitz.indexOf('-') == -1);
                int tzOffset = Integer.parseInt(nitzSubs[6]);
                int dst = (nitzSubs.length >= 8) ? Integer.parseInt(nitzSubs[7]) : 0;
                String tzName = null;
                // As a special extension, the Android emulator appends the name of
                // the host computer's timezone to the nitz string. this is zoneinfo
                // timezone name of the form Area!Location or Area!Location!SubLocation
                // so we need to convert the ! into /
                if (nitzSubs.length >= 9) {
                    tzName = nitzSubs[8].replace('!', '/');
                }
                setTimeZoneFromSplitNitzString(tzOffset, dst, sign, c, tzName);
            }

            if (mDateTimeConfig.getClockworkAutoTimeMode() == SettingsContract.AUTO_TIME_OFF
                    || syncTime != NITZ_TIME_ON) {
                return; // We are only syncing time zone from network, so don't set the time
            }

            try {
                mWakeLock.acquire();

                long millisSinceNitzReceived = SystemClock.elapsedRealtime() - nitzReceiveTime;

                if (millisSinceNitzReceived < 0) {
                    // Sanity check: something is wrong
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "NITZ: not setting time, clock has rolled "
                                + "backwards since NITZ time was received, "
                                + nitz);
                    }
                    return;
                }

                if (millisSinceNitzReceived > Integer.MAX_VALUE) {
                    // If the time is this far off, something is wrong > 24 days!
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "NITZ: not setting time, time that was to be set is greater " +
                                "than Integer.MAX_VALUE");
                    }
                    return;
                }

                // Note: with range checks above, cast to int is safe
                c.add(Calendar.MILLISECOND, (int) millisSinceNitzReceived);

                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "NITZ: Setting time of day to " + c.getTime()
                            + " NITZ receive delay(ms): " + millisSinceNitzReceived
                            + " gained(ms): "
                            + (c.getTimeInMillis() - System.currentTimeMillis())
                            + " from " + nitz);
                }

                SystemClock.setCurrentTimeMillis(c.getTimeInMillis());
            } finally {
                mWakeLock.release();
            }
        } catch (RuntimeException ex) {
            Log.e(TAG, "NITZ: Error parsing NITZ time " + nitz, ex);
        }
    }

    private void setTimeZoneFromSplitNitzString(int tzOffset, int dst, boolean sign, Calendar c,
                                                String tzName) {
        TimeZone zone = null;

        // The zone offset received from NITZ is for current local time, so DST correction
        // is already applied.  Don't add it again.
        //
        // tzOffset += dst * 4;
        //
        // We could unapply it if we wanted the raw offset.
        tzOffset = (sign ? 1 : -1) * tzOffset * 15 * 60 * 1000;

        if (tzName != null) {
            zone = TimeZone.getTimeZone(tzName);
        }

        Phone phone = PhoneFactory.getDefaultPhone();
        String iso = ((TelephonyManager) phone.getContext()
                .getSystemService(Context.TELEPHONY_SERVICE))
                .getNetworkCountryIsoForPhone(phone.getPhoneId());

        if (zone == null) {
            if (iso != null && iso.length() > 0) {
                zone = TimeUtils.getTimeZone(tzOffset, dst != 0, c.getTimeInMillis(), iso);
            } else {
                zone = getNitzTimeZone(tzOffset, dst != 0, c.getTimeInMillis());
            }
        }

        if (zone != null) {
            Intent timeZoneIntent = TimeIntents.getSetNitzTimeZoneIntent(this, zone.getID());
            this.startService(timeZoneIntent);
        }
    }

    /**
     * Returns a TimeZone object based only on parameters from the NITZ string.
     */
    private TimeZone getNitzTimeZone(int offset, boolean dst, long when) {
        TimeZone guess = findTimeZone(offset, dst, when);
        if (guess == null) {
            // Couldn't find a proper timezone.  Perhaps the DST data is wrong.
            guess = findTimeZone(offset, !dst, when);
        }
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "getNitzTimeZone returning " + (guess == null ? guess : guess.getID()));
        }
        return guess;
    }

    private TimeZone findTimeZone(int offset, boolean dst, long when) {
        int rawOffset = offset;
        if (dst) {
            rawOffset -= ONE_HOUR_MS;
        }
        String[] zones = TimeZone.getAvailableIDs(rawOffset);
        TimeZone guess = null;
        Date d = new Date(when);
        for (String zone : zones) {
            TimeZone tz = TimeZone.getTimeZone(zone);
            if (tz.getOffset(when) == offset && tz.inDaylightTime(d) == dst) {
                guess = tz;
                break;
            }
        }
        return guess;
    }

    public class NitzTimeHandler extends Handler {
        public NitzTimeHandler(Looper l) {
            super(l);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_NITZ_TIME:
                    AsyncResult ar = (AsyncResult) msg.obj;

                    String nitzString = (String)((Object[])ar.result)[0];
                    long nitzReceiveTime = ((Long)((Object[])ar.result)[1]).longValue();

                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Received NITZ time: " + nitzString);
                    }

                    setTimeFromNITZString(nitzString, nitzReceiveTime);
                    break;
            }
        }
    }
}
