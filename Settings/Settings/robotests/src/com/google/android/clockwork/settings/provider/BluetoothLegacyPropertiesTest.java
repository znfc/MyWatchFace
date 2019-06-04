package com.google.android.clockwork.settings.provider;

import android.content.ContentResolver;
import android.content.SharedPreferences;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(ClockworkRobolectricTestRunner.class)
public class BluetoothLegacyPropertiesTest {

    @Mock ContentResolver mMockResolver;
    SharedPreferences mPrefs;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mPrefs = ProviderTestUtils.getEmptyPrefs();
    }

    /** Test if properties path matches Bluetooth path. */
    @Test
    public void testPath() {
        BluetoothLegacyProperties properties =
                new BluetoothLegacyProperties(mPrefs, () -> mMockResolver);

        Assert.assertEquals("path should be legacy Bluetooth path",
                SettingsContract.BLUETOOTH_MODE_PATH, properties.getPath());
    }
}
