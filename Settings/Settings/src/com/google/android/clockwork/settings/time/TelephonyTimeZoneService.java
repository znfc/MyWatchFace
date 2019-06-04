package com.google.android.clockwork.settings.time;

import android.app.IntentService;
import android.content.Intent;

import com.google.android.clockwork.settings.SettingsIntents;

/**
 * This class is a proxy for {@link com.google.android.clockwork.settings.TimeService}
 * to receive time zone updates from {@link NitzTimeUpdateService}. {@link NitzTimeUpdateService}
 * must run in the phone process to receive NITZ updates and doesn't have access to Settings
 * permissions
 */
public class TelephonyTimeZoneService extends IntentService {

    public TelephonyTimeZoneService() {
        super("TelephonyTimeZoneService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (TimeIntents.ACTION_SET_TIMEZONE.equals(intent.getAction())) {
            String tz = intent.getStringExtra(TimeIntents.EXTRA_TIMEZONE);

            if (tz != null) {
                int source = intent.getIntExtra(
                        TimeIntents.EXTRA_TIME_ZONE_SOURCE,
                        TimeIntents.TIME_ZONE_SOURCE_NITZ);
                // forward the timezone update to TimeService
                Intent timeZoneIntent = SettingsIntents.getSetTimeZoneIntent(tz);
                timeZoneIntent.putExtra(TimeIntents.EXTRA_TIME_ZONE_SOURCE, source);
                this.startService(timeZoneIntent);
            }
        }
    }
}
