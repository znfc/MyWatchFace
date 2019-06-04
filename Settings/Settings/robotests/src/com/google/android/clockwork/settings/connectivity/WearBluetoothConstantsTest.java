package com.google.android.clockwork.settings.connectivity;

import static org.junit.Assert.assertEquals;

import android.net.Uri;

import com.android.clockwork.bluetooth.WearBluetoothConstants;
import com.google.android.clockwork.settings.SettingsContract;

import org.junit.runner.RunWith;
import org.junit.Test;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class WearBluetoothConstantsTest {
    @Test
    public void testBluetoothWearBluetoothConstantsAreSynced() {
        assertEquals(
            SettingsContract.SETTINGS_AUTHORITY,
            WearBluetoothConstants.WEARABLE_SETTINGS_AUTHORITY);
        assertEquals(SettingsContract.BLUETOOTH_PATH, WearBluetoothConstants.BLUETOOTH_PATH);
        assertEquals(
            SettingsContract.KEY_COMPANION_ADDRESS, WearBluetoothConstants.KEY_COMPANION_ADDRESS);
        assertEquals(
            SettingsContract.BLUETOOTH_URI,
            new Uri.Builder().scheme("content").authority(
                WearBluetoothConstants.WEARABLE_SETTINGS_AUTHORITY).path(
                WearBluetoothConstants.BLUETOOTH_PATH).build());
    }
}
