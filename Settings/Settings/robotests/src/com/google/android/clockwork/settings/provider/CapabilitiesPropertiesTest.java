package com.google.android.clockwork.settings.provider;

import android.content.res.Resources;
import android.content.SharedPreferences;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

@RunWith(ClockworkRobolectricTestRunner.class)
public class CapabilitiesPropertiesTest {
    @Mock Resources mResources;
    private SharedPreferences mPrefs;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mPrefs = ProviderTestUtils.getEmptyPrefs();
    }

    @Test
    public void testQuery_sideButtonTrue() {
        // GIVEN sideButton is true
        Mockito.when(mResources.getBoolean(R.bool.side_button_present)).thenReturn(true);

        // WHEN properties is instantiated
        SettingProperties props = new CapabilitiesProperties(mPrefs, mResources);

        // THEN side button should be true
        ProviderTestUtils.assertKeyValue(props, SettingsContract.KEY_SIDE_BUTTON, 1);
    }

    @Test
    public void testQuery_sideButtonFalse() {
        // GIVEN sideButton is false
        Mockito.when(mResources.getBoolean(R.bool.side_button_present)).thenReturn(false);

        // WHEN properties is instantiated
        SettingProperties props = new CapabilitiesProperties(mPrefs, mResources);

        // THEN side button should be false
        ProviderTestUtils.assertKeyValue(props, SettingsContract.KEY_SIDE_BUTTON, 0);
    }

    @Test
    public void testQuery_buttonSet_true() {
        // GIVEN flag KEY_BUTTON_SET is true
        mPrefs.edit().putBoolean(SettingsContract.KEY_BUTTON_SET, true).apply();

        // WHEN properties is instantiated
        SettingProperties props = new CapabilitiesProperties(mPrefs, mResources);

        // THEN button set should be true
        ProviderTestUtils.assertKeyValue(props, SettingsContract.KEY_BUTTON_SET, 1);
    }

    @Test
    public void testQuery_buttonSet_false() {
        // GIVEN flag KEY_BUTTON_SET is false
        mPrefs.edit().putBoolean(SettingsContract.KEY_BUTTON_SET, false).apply();

        // WHEN properties is instantiated
        SettingProperties props = new CapabilitiesProperties(mPrefs, mResources);

        // THEN button set should be false
        ProviderTestUtils.assertKeyValue(props, SettingsContract.KEY_BUTTON_SET, 0);
    }

    @Test
    public void testQuery_buttonSet_default() {
        // WHEN properties is instantiated
        SettingProperties props = new CapabilitiesProperties(mPrefs, mResources);

        // THEN button set should be false
        ProviderTestUtils.assertKeyValue(props, SettingsContract.KEY_BUTTON_SET, 0);
    }

    @Test
    public void testUpdate_buttonSet_trueToFalse() {
        // GIVEN flag KEY_BUTTON_SET is true
        mPrefs.edit().putBoolean(SettingsContract.KEY_BUTTON_SET, true).apply();

        // GIVEN properties is instantiated
        SettingProperties props = new CapabilitiesProperties(mPrefs, mResources);

        // WHEN KEY_BUTTON_SET is updated to false
        int rowsChanged = props.update(ProviderTestUtils.getContentValues(
                SettingsContract.KEY_BUTTON_SET, 0));

        // THEN queried key should be false
        ProviderTestUtils.assertKeyValue(props, SettingsContract.KEY_BUTTON_SET, 0);
        // THEN rows changed should be 1
        Assert.assertEquals("rows changed should be 1", 1, rowsChanged);
    }

    @Test
    public void testUpdate_buttonSet_falseToTrue() {
        // GIVEN flag KEY_BUTTON_SET is false
        mPrefs.edit().putBoolean(SettingsContract.KEY_BUTTON_SET, false).apply();

        // GIVEN properties is instantiated
        SettingProperties props = new CapabilitiesProperties(mPrefs, mResources);

        // WHEN KEY_BUTTON_SET is updated to true
        int rowsChanged = props.update(ProviderTestUtils.getContentValues(
                SettingsContract.KEY_BUTTON_SET, 1));

        // THEN queried key should be true
        ProviderTestUtils.assertKeyValue(props, SettingsContract.KEY_BUTTON_SET, 1);
        // THEN rows changed should be 1
        Assert.assertEquals("rows changed should be 1", 1, rowsChanged);
    }

    @Test
    public void testUpdate_buttonSet_same() {
        // GIVEN flag KEY_BUTTON_SET is true
        mPrefs.edit().putBoolean(SettingsContract.KEY_BUTTON_SET, true).apply();

        // GIVEN properties is instantiated
        SettingProperties props = new CapabilitiesProperties(mPrefs, mResources);

        // WHEN KEY_BUTTON_SET is updated to true
        int rowsChanged = props.update(ProviderTestUtils.getContentValues(
                SettingsContract.KEY_BUTTON_SET, 1));

        // THEN queried key should be true
        ProviderTestUtils.assertKeyValue(props, SettingsContract.KEY_BUTTON_SET, 1);
        // THEN prefs button set should be true
        ProviderTestUtils.assertKeyValue(mPrefs, SettingsContract.KEY_BUTTON_SET, true);
        // THEN rows changed should be 1
        Assert.assertEquals("rows changed should be 0", 0, rowsChanged);
    }
}
