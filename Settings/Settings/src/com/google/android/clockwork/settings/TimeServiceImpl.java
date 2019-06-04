package com.google.android.clockwork.settings;

import android.app.AlarmManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import com.google.android.clockwork.settings.time.TimeIntents;
import com.google.android.clockwork.settings.time.TimeSyncManager;
import com.google.android.clockwork.settings.time.TimeZoneMediator;

/**
 * A controller for TimeService that extracts the logic in that IntentService into a testable class.
 */
public class TimeServiceImpl {
  private static final String TAG = "TimeService";

  // These values map to Intent.EXTRA_TIME_PREF_VALUE_USE_xx_HOUR
  public static final int EXTRA_TIME_PREF_VALUE_USE_12_HOUR = 0;
  public static final int EXTRA_TIME_PREF_VALUE_USE_24_HOUR = 1;

  // Possible values for android.provider.Settings.System.TIME_12_24
  public static final String HOURS_12 = "12";
  public static final String HOURS_24 = "24";

  private final DateTimeConfig mDateTimeConfig;
  private final TimeSyncManager mTimeSyncManager;
  private final TimeZoneMediator mTimeZoneMediator;
  private final AlarmManager mAlarmManager;
  private final ContentResolver mContentResolver;

  /*package*/ TimeServiceImpl(
      DateTimeConfig config,
      TimeSyncManager timeSyncManager,
      TimeZoneMediator timeZoneMediator,
      AlarmManager alarmManager,
      ContentResolver contentResolver) {
    mDateTimeConfig = config;
    mTimeSyncManager = timeSyncManager;
    mTimeZoneMediator = timeZoneMediator;
    mAlarmManager = alarmManager;
    mContentResolver = contentResolver;
  }

  /*package*/ void onHandleIntent(Context context, Intent intent) {
    final String action = intent.getAction();
    switch (action) {
      case SettingsIntents.ACTION_SET_TIME:
        setTime(intent.getLongExtra(SettingsIntents.EXTRA_CURRENT_TIME_MILLIS, 0),
                intent.getLongExtra(SettingsIntents.EXTRA_SENT_AT_TIME, 0));
        break;
      case SettingsIntents.ACTION_SET_TIMEZONE:
        handleTimeZoneUpdate(
                intent.getStringExtra(SettingsIntents.EXTRA_TIMEZONE),
                // Default to phone as the time zone source for backwards compatibility
                // with the Home API contract.
                intent.getIntExtra(
                        TimeIntents.EXTRA_TIME_ZONE_SOURCE,
                        TimeIntents.TIME_ZONE_SOURCE_PHONE));
        break;
      case SettingsIntents.ACTION_SET_24HOUR:
        setIs24Hour(context, intent.hasExtra(SettingsIntents.EXTRA_IS_24_HOUR)
                ? intent.getBooleanExtra(SettingsIntents.EXTRA_IS_24_HOUR, false) : null);
        break;
      case SettingsIntents.ACTION_SET_HOME_TIME:
        setPhoneTimeSyncerHomeTime(intent.getLongExtra(SettingsIntents.EXTRA_HOME_TIME, 0),
                intent.getLongExtra(SettingsIntents.EXTRA_COMPANION_TIME, 0));
        break;
      case SettingsIntents.ACTION_REQUEST_TIME_SYNCER_UPDATE:
        requestPhoneTimeSyncerUpdate();
        break;
      case SettingsIntents.ACTION_EVALUATE_TIME_SYNCING:
        evaluateTimeSyncing(context);
        break;
      case SettingsIntents.ACTION_EVALUATE_TIME_ZONE_SYNCING:
        evaluateTimeZoneSyncing(context);
        break;
      case TimeIntents.ACTION_PHONE_TIME_REQUEST_TIMEOUT:
        handlePhoneTimeRequestTimeout();
        break;
      case TimeIntents.ACTION_CHECK_TIMEZONE:
        handleCheckTimezone(intent.getLongExtra(TimeZoneMediator.EXTRA_TIME_AT_SEND, 0));
        break;
      case TimeIntents.ACTION_POLL_PHONE_TIME:
        requestPhoneTimeSyncerUpdate();
        break;
      case TimeIntents.ACTION_NETWORK_TIME_REFRESH:
        handleNetworkTimeRefresh();
        break;
      case TimeIntents.ACTION_SET_NTP_TIME:
        setNtpTime();
        break;
    }
  }

  /** Handle a request to check the system time zone after a NITZ update */
  private void handleCheckTimezone(long timeAtSend) {
    mTimeZoneMediator.handleCheckTimeZone(timeAtSend);
  }

  /** Handle when the network time is successfully refreshed */
  private void handleNetworkTimeRefresh() {
    mTimeSyncManager.handleNetworkTimeRefresh();
  }

  /** Set the system clock to NTP time */
  private void setNtpTime() {
    mTimeSyncManager.setNtpTime();
  }

  /** Handle when a request for the time from the phone times out */
  private void handlePhoneTimeRequestTimeout() {
    mTimeSyncManager.handlePhoneTimeRequestTimeout();
  }

  private void handleTimeZoneUpdate(String timezone, int source) {
    if (source == TimeIntents.TIME_ZONE_SOURCE_PHONE) {
      mTimeZoneMediator.addPhoneUpdate(timezone);
    } else {
      mTimeZoneMediator.addNitzUpdate(timezone);
    }
  }

  private void setTime(long currentTimeMillis, long sentAtTime) {
    if (currentTimeMillis == 0 || sentAtTime == 0) {
      throw new IllegalArgumentException(
          "both currentTimeMillis and sentAtTime must be nonzero");
    }
    final long time = currentTimeMillis + (SystemClock.elapsedRealtime() - sentAtTime);
    Log.i(TAG, "Setting time to " + time);
    mAlarmManager.setTime(time);
  }

  private void setIs24Hour(Context context, Boolean is24Hour) {
    logDebug("Setting is24Hour to " + is24Hour);
    String is24HourString = is24Hour == null ? null : is24Hour ? HOURS_24 : HOURS_12;
    Settings.System.putString(mContentResolver, Settings.System.TIME_12_24, is24HourString);
    // Update the Clockwork version of the time format setting as well to keep it in sync with
    // the phone's time format setting when we are syncing from the phone
    mDateTimeConfig.set24HourMode(is24Hour == null ? false : is24Hour);
    // Notify any interested listeners that the time format has changed
    Intent timeChanged = new Intent(Intent.ACTION_TIME_CHANGED);
    timeChanged.addFlags(Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
    // The settings UI currently only supports a choice between "use 12 hour format" and
    // "use 24 hour format", with the the "use locale default" being interpreted as one of those
    // two. In future it might support "use locale default" explicitly.
    int timeFormatPreference = is24Hour == null ? -1 : is24Hour
        ? EXTRA_TIME_PREF_VALUE_USE_24_HOUR
        : EXTRA_TIME_PREF_VALUE_USE_12_HOUR;
    timeChanged.putExtra(Intent.EXTRA_TIME_PREF_24_HOUR_FORMAT, timeFormatPreference);
    context.sendBroadcast(timeChanged);
  }

  private void setPhoneTimeSyncerHomeTime(final long requestTicks, final long companionTime) {
    logDebug("Setting home time through PhoneTimeSyncer");
    mTimeSyncManager.handlePhoneTimeSyncResponse(requestTicks, companionTime);
  }

  private void requestPhoneTimeSyncerUpdate() {
    logDebug("Starting a PhoneTimeSyncer update");
    mTimeSyncManager.startPhoneTimeUpdate();
  }

  /**
   * Ensure that the appropriate time syncer is running based on Clockwork's auto time mode.
   * If the time syncer should be running, start it.
   */
  private void evaluateTimeSyncing(Context context) {
    // sync from phone and network were combined in b/35318277,
    // so upgrade and standardize on SYNC_TIME_FROM_PHONE
    if (mDateTimeConfig.getClockworkAutoTimeMode() == SettingsContract.SYNC_TIME_FROM_NETWORK) {
      mDateTimeConfig.setAutoTime(SettingsContract.SYNC_TIME_FROM_PHONE);
    }

    if (mDateTimeConfig.getClockworkAutoTimeMode() == SettingsContract.AUTO_TIME_OFF) {
      mTimeSyncManager.cancelPendingTasks();
    } else {
      requestPhoneTimeSyncerUpdate();
      context.startService(SettingsIntents.getReevaluatePhone24HrFormatIntent());
    }
  }

  /**
   * Ensure that the appropriate time zone syncer is running based on Clockwork's auto time zone
   * mode. If the timezone mediator should be running, start it.
   */
  private void evaluateTimeZoneSyncing(Context context) {
    // sync from phone and network were combined in b/35318281,
    // so upgrade and standardize on SYNC_TIME_ZONE_FROM_PHONE
    if (mDateTimeConfig.getClockworkAutoTimeZoneMode()
            == SettingsContract.SYNC_TIME_ZONE_FROM_NETWORK) {
      mDateTimeConfig.setAutoTimeZone(SettingsContract.SYNC_TIME_ZONE_FROM_PHONE);
    }

    if (mDateTimeConfig.getClockworkAutoTimeZoneMode() == SettingsContract.AUTO_TIME_ZONE_OFF) {
      mTimeZoneMediator.cancelPendingTasks();
    } else {
      context.startService(SettingsIntents.getReevaluatePhoneTimeZoneIntent());
      mTimeZoneMediator.startNitzService();
    }
  }

  private void logDebug(String message) {
    if (Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(TAG, message);
    }
  }
}
