package com.google.android.clockwork.settings.connectivity.wifi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowApplication;

/** Tests for {@link WifiSettingsDisableActivity}. */
@RunWith(ClockworkRobolectricTestRunner.class)
public class WifiSettingsDisableActivityTest {
    @Test
    public void testOnCreate() {
        // GIVEN the Wifi adapter is initially enabled
        Context context = ShadowApplication.getInstance().getApplicationContext();
        WifiManager wifiManager = context.getSystemService(WifiManager.class);
        wifiManager.setWifiEnabled(true);
        assertEquals(true, wifiManager.isWifiEnabled());

        // WHEN a request to disable the Wifi adapter arrives
        WifiSettingsDisableActivity activity =
                Robolectric.buildActivity(
                    WifiSettingsDisableActivity.class, new Intent(WifiManager.ACTION_REQUEST_DISABLE))
                        .create()
                        .get();
        ShadowActivity shadowActivity = (ShadowActivity) Shadow.extract(activity);

        // THEN the activity returns OK and disables the Wifi adapter
        assertEquals(false, wifiManager.isWifiEnabled());
        assertTrue(shadowActivity.isFinishing());
        assertEquals(Activity.RESULT_OK, shadowActivity.getResultCode());
    }
}
