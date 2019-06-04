package com.google.android.clockwork.settings;

import android.app.AlarmManager;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.google.android.clockwork.settings.time.TimeSyncManager;
import com.google.android.clockwork.settings.time.TimeZoneMediator;

/**
 * Service that updates the current system time and related properties (e.g timezone, 24h) in
 * response to intents received.
 *
 * See: go/wear-time
 */
public final class TimeService extends IntentService {
    private static final String TAG = "Clockwork.TimeService";
    private TimeServiceImpl mTimeService;

    public TimeService() {
        super("TimeService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mTimeService = new TimeServiceImpl(
            DefaultDateTimeConfig.INSTANCE.get(this),
            TimeSyncManager.INSTANCE.get(this),
            TimeZoneMediator.INSTANCE.get(this),
            (AlarmManager) getSystemService(Context.ALARM_SERVICE),
            getContentResolver());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mTimeService.onHandleIntent(this, intent);
    }
}
