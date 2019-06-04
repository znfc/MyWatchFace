package com.google.android.clockwork.settings.time;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Intent;

import com.google.android.clockwork.settings.SettingsIntents;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowApplication;

@RunWith(ClockworkRobolectricTestRunner.class)
public class TelephonyTimeZoneServiceTest {
    private static final String timezone = "America/New_York";

    @Test
    public void testOnHandleIntent_IntentForwarded() {
        Intent intent = TimeIntents.getSetNitzTimeZoneIntent(
                ShadowApplication.getInstance().getApplicationContext(), timezone);

        Robolectric.buildService(TestService.class, intent).startCommand(0, 0);

        Intent forwardedIntent = ShadowApplication.getInstance().getNextStartedService();
        Intent expectedIntent = SettingsIntents.getSetTimeZoneIntent(timezone);
        assertEquals(expectedIntent.getAction(), forwardedIntent.getAction());
        assertEquals(expectedIntent.getComponent(), forwardedIntent.getComponent());
        assertEquals(
                TimeIntents.TIME_ZONE_SOURCE_NITZ,
                forwardedIntent.getIntExtra(TimeIntents.EXTRA_TIME_ZONE_SOURCE, -1));
        assertEquals(timezone, forwardedIntent.getStringExtra(TimeIntents.EXTRA_TIMEZONE));
    }

    @Test
    public void testOnHandleIntent_IntentForwarded_NitzDefault() {
        Intent intent = new Intent(ShadowApplication.getInstance().getApplicationContext(),
                TelephonyTimeZoneService.class);
        intent.setAction(TimeIntents.ACTION_SET_TIMEZONE);
        intent.putExtra(TimeIntents.EXTRA_TIMEZONE, timezone);

        Robolectric.buildService(TestService.class, intent).startCommand(0, 0);

        Intent forwardedIntent = ShadowApplication.getInstance().getNextStartedService();
        Intent expectedIntent = SettingsIntents.getSetTimeZoneIntent(timezone);
        assertEquals(expectedIntent.getAction(), forwardedIntent.getAction());
        assertEquals(expectedIntent.getComponent(), forwardedIntent.getComponent());
        assertEquals(
                TimeIntents.TIME_ZONE_SOURCE_NITZ,
                forwardedIntent.getIntExtra(TimeIntents.EXTRA_TIME_ZONE_SOURCE, -1));
        assertEquals(
                timezone,
                forwardedIntent.getStringExtra(TimeIntents.EXTRA_TIMEZONE));
    }

    @Test
    public void testOnHandleIntent_NoopOnNoTimeZone() {
        Intent intent = new Intent(ShadowApplication.getInstance().getApplicationContext(),
                TelephonyTimeZoneService.class);
        intent.setAction(TimeIntents.ACTION_SET_TIMEZONE);

        Robolectric.buildService(TestService.class, intent).startCommand(0, 0);

        assertNull(ShadowApplication.getInstance().getNextStartedService());
    }

    private static class TestService extends TelephonyTimeZoneService {

        @Override
        public void onStart(Intent intent, int startId) {
            onHandleIntent(intent);
            stopSelf(startId);
        }
    }
}
