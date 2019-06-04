package com.google.android.clockwork.settings.time;

import android.content.Context;
import android.content.Intent;

import com.google.android.clockwork.settings.SettingsIntents;
import com.google.android.clockwork.settings.TimeService;

/**
 * This class provides actions and intents to be handled by {@link TimeService}
 * in order to maintain time and time zone syncing on the device.
 */
public class TimeIntents {
    public static final String ACTION_PHONE_TIME_REQUEST_TIMEOUT =
            "com.google.android.clockwork.settings.PhoneTimeSyncer.action.TIMEOUT";
    public static final String ACTION_POLL_PHONE_TIME =
            "com.google.android.clockwork.settings.TimeSyncManager.action.POLL";
    public static final String ACTION_CHECK_TIMEZONE =
            "com.google.android.clockwork.settings.TimeZoneMediator.action.CHECK_TIMEZONE";
    public static final String ACTION_NETWORK_TIME_REFRESH =
            "com.google.android.clockwork.settings.time.RefreshTimeJobService.action.REFRESH";
    public static final String ACTION_SET_NTP_TIME =
            "com.google.android.clockwork.settings.time.NetworkTimeSyncer.action.SET_TIME";
    public static final String ACTION_SET_TIMEZONE =
            "com.google.android.clockwork.settings.TelephonyTimeZoneService.action.SET_TIMEZONE";

    public static final String EXTRA_TIME_ZONE_SOURCE = "extra-time-zone-source";
    public static final String EXTRA_TIMEZONE = SettingsIntents.EXTRA_TIMEZONE;

    public static final int TIME_ZONE_SOURCE_PHONE = 0;
    public static final int TIME_ZONE_SOURCE_NITZ = 1;

    public static Intent getPhoneTimeRequestTimeoutIntent(Context context) {
        return new Intent(context, TimeService.class).setAction(ACTION_PHONE_TIME_REQUEST_TIMEOUT);
    }

    public static Intent getPollPhoneTimeIntent(Context context) {
        return new Intent(context, TimeService.class).setAction(ACTION_POLL_PHONE_TIME);
    }

    public static Intent getCheckTimeZoneIntent(Context context) {
        return new Intent(context, TimeService.class).setAction(ACTION_CHECK_TIMEZONE);
    }

    public static Intent getNetworkTimeRefreshIntent(Context context) {
        return new Intent(context, TimeService.class).setAction(ACTION_NETWORK_TIME_REFRESH);
    }

    public static Intent getSetNtpTimeIntent(Context context) {
        return new Intent(context, TimeService.class).setAction(ACTION_SET_NTP_TIME);
    }

    static Intent getSetNitzTimeZoneIntent(Context context, String timezone) {
        return new Intent(context, TelephonyTimeZoneService.class)
                .setAction(ACTION_SET_TIMEZONE)
                .putExtra(EXTRA_TIME_ZONE_SOURCE, TIME_ZONE_SOURCE_NITZ)
                .putExtra(EXTRA_TIMEZONE, timezone);
    }

    public static final int REQ_CHECK_TIME_ZONE = 0;
    public static final int REQ_POLL_PHONE_TIME = 1;
    public static final int REQ_TIME_REQUEST_TIMEOUT = 2;

    private TimeIntents() {}
}
