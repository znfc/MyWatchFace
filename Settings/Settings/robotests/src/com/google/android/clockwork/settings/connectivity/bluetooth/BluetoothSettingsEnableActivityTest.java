package com.google.android.clockwork.settings.connectivity.bluetooth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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

/** Tests for {@link BluetoothSettingsEnableActivity}. */
@RunWith(ClockworkRobolectricTestRunner.class)
public class BluetoothSettingsEnableActivityTest {
    private static final String PACKAGE_NAME = "com.my.package";
    private ShadowApplication mShadowApplication;
    private Context mContext;
    private BluetoothAdapter mBluetoothAdapter;
    private Intent mEnableIntent;
    private Intent mStateEnabled;

    @Before
    public void setUp() {
        mShadowApplication = ShadowApplication.getInstance();
        mContext = mShadowApplication.getApplicationContext();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mEnableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        mEnableIntent.putExtra(Intent.EXTRA_PACKAGE_NAME, PACKAGE_NAME);

        mStateEnabled = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        mStateEnabled.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_ON);

        DefaultPermissionReviewModeUtils.INSTANCE.setTestInstance(
                new FakePermissionReviewModeUtils(false));
    }

    @After
    public void tearDown() {
        DefaultPermissionReviewModeUtils.INSTANCE.clearTestInstance();
    }

    @Test
    public void testUserAccepts() {
        // GIVEN the Bluetooth adapter is initially disabled
        mBluetoothAdapter.disable();
        assertFalse(mBluetoothAdapter.isEnabled());

        // WHEN a request to enable the Bluetooth adapter arrives
        BluetoothSettingsEnableActivity activity =
                Robolectric.buildActivity(
                    BluetoothSettingsEnableActivity.class, mEnableIntent)
                        .create()
                        .get();
        ShadowActivity shadowActivity = (ShadowActivity) Shadow.extract(activity);

        // THEN a consent dialog is shown
        ShadowAcceptDenyDialog diag = ShadowAcceptDenyDialog.getLatestAcceptDenyDialog(activity);
        assertNotNull(diag);

        // WHEN the user accepts the Bluetooth enabling
        diag.getPositiveButton().onClick(null, 0);

        // THEN the Bluetooth adapter is enabled
        assertTrue(mBluetoothAdapter.isEnabled());

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
        // GIVEN the Bluetooth adapter is initially disabled
        mBluetoothAdapter.disable();
        assertFalse(mBluetoothAdapter.isEnabled());

        // WHEN a request to enable the Bluetooth adapter arrives
        BluetoothSettingsEnableActivity activity =
                Robolectric.buildActivity(BluetoothSettingsEnableActivity.class, mEnableIntent)
                        .create()
                        .get();
        ShadowActivity shadowActivity = (ShadowActivity) Shadow.extract(activity);

        // THEN a consent dialog is shown
        ShadowAcceptDenyDialog diag = ShadowAcceptDenyDialog.getLatestAcceptDenyDialog(activity);
        assertNotNull(diag);

        // WHEN the user denies the Bluetooth enabling
        diag.getNegativeButton().onClick(null, 0);

        // THEN the Bluetooth adapter stays disabled and activity returns CANCELED
        assertFalse(mBluetoothAdapter.isEnabled());
        assertTrue(shadowActivity.isFinishing());
        assertEquals(Activity.RESULT_CANCELED, shadowActivity.getResultCode());
    }

    @Test
    public void testCmiitWhitelisted() {
        // GIVEN the Bluetooth adapter is initially disabled
        mBluetoothAdapter.disable();
        assertFalse(mBluetoothAdapter.isEnabled());

        // GIVEN the device is under permission review mode
        FakePermissionReviewModeUtils permReviewUtils = new FakePermissionReviewModeUtils(true);
        DefaultPermissionReviewModeUtils.INSTANCE.setTestInstance(permReviewUtils);

        // GIVEN the enable request is from a whitelisted package
        String whitelistedPackage = "com.package.cmiit.whitelisted";
        permReviewUtils.addCmiitWhitelistedPackage(whitelistedPackage);
        Intent mEnableIntentFromWhitelistedPkg = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        mEnableIntentFromWhitelistedPkg.putExtra(Intent.EXTRA_PACKAGE_NAME, whitelistedPackage);

        // WHEN a request to enable the Bluetooth adapter arrives
        BluetoothSettingsEnableActivity activity =
                Robolectric.buildActivity(
                    BluetoothSettingsEnableActivity.class, mEnableIntentFromWhitelistedPkg)
                        .create()
                        .get();
        ShadowActivity shadowActivity = (ShadowActivity) Shadow.extract(activity);

        // THEN the bluetooth adapter is enabled and activity returns OK
        assertTrue(mBluetoothAdapter.isEnabled());
        assertTrue(shadowActivity.isFinishing());
        assertEquals(Activity.RESULT_OK, shadowActivity.getResultCode());
    }
}
