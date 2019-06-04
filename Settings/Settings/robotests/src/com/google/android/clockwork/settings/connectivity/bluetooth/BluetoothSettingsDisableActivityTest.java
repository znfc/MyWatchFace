package com.google.android.clockwork.settings.connectivity.bluetooth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowActivity;

/** Tests for {@link BluetoothSettingsDisableActivity}. */
@RunWith(ClockworkRobolectricTestRunner.class)
public class BluetoothSettingsDisableActivityTest {
    @Test
    public void testOnCreate() {
        // GIVEN the Bluetooth adapter is initially enabled
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothAdapter.enable();
        assertEquals(true, bluetoothAdapter.isEnabled());

        // WHEN a request to disable the Bluetooth adapter arrives
        BluetoothSettingsDisableActivity activity =
                Robolectric.buildActivity(
                    BluetoothSettingsDisableActivity.class, new Intent(BluetoothAdapter.ACTION_REQUEST_DISABLE))
                        .create()
                        .get();
        ShadowActivity shadowActivity = (ShadowActivity) Shadow.extract(activity);

        // THEN the activity returns OK and disables the Bluetooth adapter
        assertEquals(false, bluetoothAdapter.isEnabled());
        assertTrue(shadowActivity.isFinishing());
        assertEquals(Activity.RESULT_OK, shadowActivity.getResultCode());
    }
}
