package com.google.android.clockwork.settings.provider;

import android.content.SharedPreferences;
import com.google.android.clockwork.common.setup.wearable.Constants;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.Test;

@RunWith(ClockworkRobolectricTestRunner.class)
public class OemPropertiesTest {
    @Rule public ExpectedException thrown = ExpectedException.none();
    private SharedPreferences mPrefs;

    @Before
    public void setup() {
        mPrefs = ProviderTestUtils.getEmptyPrefs();
    }

    @Test
    public void testQuery_oemSetupCurrent_matching() {
        // GIVEN mPrefs with key with matching oem setup version
        mPrefs.edit().putInt(
                OemProperties.PREF_OEM_SETUP_VERSION, Constants.OEM_DATA_VERSION).apply();

        // WHEN properties is created
        SettingProperties props = new OemProperties(mPrefs);

        // THEN queried key should be true
        ProviderTestUtils.assertKeyValue(props, SettingsContract.KEY_OEM_SETUP_CURRENT, 1);
    }

    @Test
    public void testQuery_oemSetupCurrent_nonMatching() {
        // GIVEN empty mPrefs

        // WHEN properties is created
        SettingProperties props = new OemProperties(mPrefs);

        // THEN queried key should be false
        ProviderTestUtils.assertKeyValue(props, SettingsContract.KEY_OEM_SETUP_CURRENT, 0);
    }

    @Test
    public void testQuery_oemSetupVersion() {
        // GIVEN mPrefs with key with oem setup version
        mPrefs.edit().putInt(
                OemProperties.PREF_OEM_SETUP_VERSION, Constants.OEM_DATA_VERSION).apply();

        // WHEN properties is created
        SettingProperties props = new OemProperties(mPrefs);

        // THEN queried key should be oem setup version
        ProviderTestUtils.assertKeyValue(
                props, SettingsContract.KEY_OEM_SETUP_VERSION, Constants.OEM_DATA_VERSION);
    }

    @Test
    public void testUpdate_oemSetupVersion() {
        // GIVEN properties is created
        SettingProperties props = new OemProperties(mPrefs);

        // WHEN KEY_OEM_SETUP_VERSION is updated
        int rowsChanged = props.update(ProviderTestUtils.getContentValues(
                SettingsContract.KEY_OEM_SETUP_VERSION, Constants.OEM_DATA_VERSION));

        // THEN queried key should be oem version
        ProviderTestUtils.assertKeyValue(props,
                OemProperties.PREF_OEM_SETUP_VERSION, Constants.OEM_DATA_VERSION);
        // THEN rows changed should be 1
        Assert.assertEquals("rows changed should be 1", 1, rowsChanged);
    }
}
