package com.google.android.clockwork.settings.connectivity;

import static org.junit.Assert.assertEquals;

import android.net.Uri;

import com.android.clockwork.cellular.WearCellularConstants;
import com.google.android.clockwork.settings.SettingsContract;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class WearCellularConstantsTest {
    @Test
    public void testCellularWearBluetoothConstantsAreSynced() {
        assertEquals(
                SettingsContract.SETTINGS_AUTHORITY,
                WearCellularConstants.WEARABLE_SETTINGS_AUTHORITY);
        assertEquals(
                SettingsContract.MOBILE_SIGNAL_DETECTOR_PATH,
                WearCellularConstants.MOBILE_SIGNAL_DETECTOR_PATH);
        assertEquals(
                SettingsContract.KEY_MOBILE_SIGNAL_DETECTOR,
                WearCellularConstants.KEY_MOBILE_SIGNAL_DETECTOR);
        assertEquals(
                SettingsContract.MOBILE_SIGNAL_DETECTOR_URI,
                new Uri.Builder().scheme("content").authority(
                        WearCellularConstants.WEARABLE_SETTINGS_AUTHORITY).path(
                        WearCellularConstants.MOBILE_SIGNAL_DETECTOR_PATH).build());
    }
}
