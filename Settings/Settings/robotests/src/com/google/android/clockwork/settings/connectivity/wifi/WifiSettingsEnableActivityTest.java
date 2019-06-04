package com.google.android.clockwork.settings.connectivity.wifi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import com.google.android.clockwork.settings.ShadowAcceptDenyDialog;
import com.google.android.clockwork.settings.connectivity.DefaultPermissionReviewModeUtils;
import com.google.android.clockwork.settings.connectivity.FakePermissionReviewModeUtils;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowApplication;

/** Tests for {@link WifiSettingsEnableActivity}. */
@RunWith(ClockworkRobolectricTestRunner.class)
public class WifiSettingsEnableActivityTest {
    private static final String PACKAGE_NAME = "com.my.package";
    private ShadowApplication mShadowApplication;
    private Context mContext;
    private WifiManager mWifiManager;
    private Intent mEnableIntent;
    private Intent mStateEnabled;

    @Before
    public void setUp() {
        mShadowApplication = ShadowApplication.getInstance();
        mContext = mShadowApplication.getApplicationContext();
        mWifiManager = mContext.getSystemService(WifiManager.class);

        mEnableIntent = new Intent(WifiManager.ACTION_REQUEST_ENABLE);
        mEnableIntent.putExtra(Intent.EXTRA_PACKAGE_NAME, PACKAGE_NAME);

        mStateEnabled = new Intent(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mStateEnabled.putExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_ENABLED);

        DefaultPermissionReviewModeUtils.INSTANCE.setTestInstance(
                new FakePermissionReviewModeUtils(false));
    }

    @After
    public void tearDown() {
        DefaultPermissionReviewModeUtils.INSTANCE.clearTestInstance();
    }

    @Test
    public void testUserAccepts() {
        // GIVEN the Wifi adapter is initially disabled
        mWifiManager.setWifiEnabled(false);
        assertFalse(mWifiManager.isWifiEnabled());

        // WHEN a request to enable the Wifi adapter arrives
        WifiSettingsEnableActivity activity =
                Robolectric.buildActivity(WifiSettingsEnableActivity.class, mEnableIntent)
                        .create()
                        .get();
        ShadowActivity shadowActivity = (ShadowActivity) Shadow.extract(activity);

        // THEN a consent dialog is shown
        ShadowAcceptDenyDialog diag = ShadowAcceptDenyDialog.getLatestAcceptDenyDialog(activity);
        assertNotNull(diag);

        // WHEN the user accepts the Wifi enabling
        diag.getPositiveButton().onClick(null, 0);

        // THEN the Wifi adapter is enabled
        assertEquals(true, mWifiManager.isWifiEnabled());

        // WHEN the state changing broadcast follows the adapter enabling
        for (BroadcastReceiver receiver : mShadowApplication.getReceiversForIntent(mStateEnabled)) {
            receiver.onReceive(mContext, mStateEnabled);
        }

        // THEN the activity returns OK
        assertTrue(shadowActivity.isFinishing());
        assertEquals(Activity.RESULT_OK, shadowActivity.getResultCode());
    }

    @Test
    public void testUserDenies() {
        // GIVEN the Wifi adapter is initially disabled
        mWifiManager.setWifiEnabled(false);
        assertFalse(mWifiManager.isWifiEnabled());

        // WHEN a request to enable the Wifi adapter arrives
        WifiSettingsEnableActivity activity =
                Robolectric.buildActivity(WifiSettingsEnableActivity.class, mEnableIntent)
                        .create()
                        .get();
        ShadowActivity shadowActivity = (ShadowActivity) Shadow.extract(activity);

        // THEN a consent dialog is shown
        ShadowAcceptDenyDialog diag = ShadowAcceptDenyDialog.getLatestAcceptDenyDialog(activity);
        assertNotNull(diag);

        // WHEN the user accepts the Wifi enabling
        diag.getNegativeButton().onClick(null, 0);

        // THEN the Wifi adapter stays disabled and the activity returns CANCELED
        assertFalse(mWifiManager.isWifiEnabled());
        assertTrue(shadowActivity.isFinishing());
        assertEquals(Activity.RESULT_CANCELED, shadowActivity.getResultCode());
    }

    @Test
    public void testCmiitWhitelisted() {
        // GIVEN the WiFi is initially disabled
        mWifiManager.setWifiEnabled(false);
        assertFalse(mWifiManager.isWifiEnabled());

        // GIVEN the device is under permission review mode
        FakePermissionReviewModeUtils permReviewUtils = new FakePermissionReviewModeUtils(true);
        DefaultPermissionReviewModeUtils.INSTANCE.setTestInstance(permReviewUtils);

        // GIVEN the enable request is from a whitelisted package
        String whitelistedPackage = "com.package.cmiit.whitelisted";
        permReviewUtils.addCmiitWhitelistedPackage(whitelistedPackage);
        Intent mEnableIntentFromWhitelistedPkg = new Intent(WifiManager.ACTION_REQUEST_ENABLE);
        mEnableIntentFromWhitelistedPkg.putExtra(Intent.EXTRA_PACKAGE_NAME, whitelistedPackage);

        // WHEN a request to enable the WiFi arrives
        WifiSettingsEnableActivity activity =
                Robolectric.buildActivity(
                    WifiSettingsEnableActivity.class, mEnableIntentFromWhitelistedPkg)
                        .create()
                        .get();
        ShadowActivity shadowActivity = (ShadowActivity) Shadow.extract(activity);

        // THEN the WiFi is enabled and activity returns OK
        assertTrue(mWifiManager.isWifiEnabled());
        assertTrue(shadowActivity.isFinishing());
        assertEquals(Activity.RESULT_OK, shadowActivity.getResultCode());
    }
}
