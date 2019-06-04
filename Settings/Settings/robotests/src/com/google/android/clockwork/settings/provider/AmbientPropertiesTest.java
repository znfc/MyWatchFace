package com.google.android.clockwork.settings.provider;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.provider.Settings;

import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(ClockworkRobolectricTestRunner.class)
public class AmbientPropertiesTest {
    private SharedPreferences mPrefs;
    @Mock Resources mResources;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mPrefs = ProviderTestUtils.getEmptyPrefs();
        mContext = RuntimeEnvironment.application;
        when(mResources.getBoolean(anyInt())).thenReturn(true);
    }

    @Test
    public void testQuery_forceWhenDocked() {
        // WHEN ambient properties is instantiated
        SettingProperties props = new AmbientProperties(mContext, mPrefs, mResources);

        // THEN properties has force when docked value
        ProviderTestUtils.assertKeyExists(props, SettingsContract.KEY_AMBIENT_FORCE_WHEN_DOCKED);
    }

    @Test
    public void testQuery_gestureSensorId() {
        // WHEN ambient properties is instantiated
        SettingProperties props = new AmbientProperties(mContext, mPrefs, mResources);

        // THEN properties has gesture sensor id
        ProviderTestUtils.assertKeyExists(props, SettingsContract.KEY_AMBIENT_GESTURE_SENSOR_ID);
    }

    @Test
    public void testQuery_lowBitEnabled() {
        // WHEN ambient properties is instantiated
        SettingProperties props = new AmbientProperties(mContext, mPrefs, mResources);

        // THEN properties has low bit enabled
        ProviderTestUtils.assertKeyExists(props, SettingsContract.KEY_AMBIENT_LOW_BIT_ENABLED);
    }

    @Test
    public void testQuery_pluggedTimeoutMin() {
        // WHEN ambient properties is instantiated
        SettingProperties props = new AmbientProperties(mContext, mPrefs, mResources);

        // THEN properties has plugged timeout min
        ProviderTestUtils.assertKeyExists(props, SettingsContract.KEY_AMBIENT_PLUGGED_TIMEOUT_MIN);
    }

    @Test
    public void testQuery_ambientEnabled() {
        // WHEN ambient properties is instantiated
        SettingProperties props = new AmbientProperties(mContext, mPrefs, mResources);

        // THEN properties has ambient enabled
        ProviderTestUtils.assertKeyExists(props, SettingsContract.KEY_AMBIENT_ENABLED);
    }

    @Test
    public void testQuery_lowBitEnabledDevTrue() {
        // GIVEN flag KEY_AMBIENT_LOW_BIT_ENABLED_DEV is true
        mPrefs.edit().putBoolean(SettingsContract.KEY_AMBIENT_LOW_BIT_ENABLED_DEV, true).apply();

        // WHEN ambient properties is instantiated
        SettingProperties props = new AmbientProperties(mContext, mPrefs, mResources);

        // THEN properties ambient low bit dev should be true
        ProviderTestUtils.assertKeyValue(props,
                SettingsContract.KEY_AMBIENT_LOW_BIT_ENABLED_DEV, 1);
    }

    @Test
    public void testQuery_lowBitEnabledDevFalse() {
        // GIVEN flag KEY_AMBIENT_LOW_BIT_ENABLED_DEV is false
        mPrefs.edit().putBoolean(SettingsContract.KEY_AMBIENT_LOW_BIT_ENABLED_DEV, false).apply();

        // WHEN ambient properties is instantiated
        SettingProperties props = new AmbientProperties(mContext, mPrefs, mResources);

        // THEN properties ambient low bit dev should be false
        ProviderTestUtils.assertKeyValue(props,
                SettingsContract.KEY_AMBIENT_LOW_BIT_ENABLED_DEV, 0);
    }

    @Test
    public void testQuery_lowBitEnabledDevDefault() {
        // GIVEN flag KEY_AMBIENT_LOW_BIT_ENABLED_DEV is not defined

        // WHEN ambient properties is instantiated
        SettingProperties props = new AmbientProperties(mContext, mPrefs, mResources);

        // THEN properties ambient low bit dev should be false
        ProviderTestUtils.assertKeyValue(props,
                SettingsContract.KEY_AMBIENT_LOW_BIT_ENABLED_DEV, 0);
    }

    @Test
    public void testQuery_tiltToWakeTrue() {
        // GIVEN flag KEY_AMBIENT_TILT_TO_WAKE is true
        mPrefs.edit().putBoolean(SettingsContract.KEY_AMBIENT_TILT_TO_WAKE, true).apply();

        // WHEN ambient properties is instantiated
        SettingProperties props = new AmbientProperties(mContext, mPrefs, mResources);

        // THEN properties ambient tilt to wake should be true
        ProviderTestUtils.assertKeyValue(props,
                SettingsContract.KEY_AMBIENT_TILT_TO_WAKE, 1);
    }

    @Test
    public void testQuery_tiltToWakeFalse() {
        // GIVEN flag KEY_AMBIENT_TILT_TO_WAKE is false
        mPrefs.edit().putBoolean(SettingsContract.KEY_AMBIENT_TILT_TO_WAKE, false).apply();

        // WHEN ambient properties is instantiated
        SettingProperties props = new AmbientProperties(mContext, mPrefs, mResources);

        // THEN properties ambient tilt to wake should be false
        ProviderTestUtils.assertKeyValue(props,
                SettingsContract.KEY_AMBIENT_TILT_TO_WAKE, 0);
    }

    @Test
    public void testQuery_tiltToWakeDefault() {
        // GIVEN flag KEY_AMBIENT_LOW_BIT_ENABLED_DEV is not defined

        // WHEN ambient properties is instantiated
        SettingProperties props = new AmbientProperties(mContext, mPrefs, mResources);

        // THEN properties ambient tilt to wake should be true
        ProviderTestUtils.assertKeyValue(props,
                SettingsContract.KEY_AMBIENT_TILT_TO_WAKE, 1);
    }

    @Test
    public void testUpdate_ambientTiltToWake_trueToFalse() {
        // GIVEN flag KEY_AMBIENT_TILT_TO_WAKE is true
        mPrefs.edit().putBoolean(SettingsContract.KEY_AMBIENT_TILT_TO_WAKE, true).apply();
        // GIVEN ambient properties
        SettingProperties props = new AmbientProperties(mContext, mPrefs, mResources);

        // WHEN KEY_AMBIENT_TILT_TO_WAKE is updated to false
        int rowsChanged = props.update(ProviderTestUtils.getContentValues(
                SettingsContract.KEY_AMBIENT_TILT_TO_WAKE, 0));

        // THEN properties ambient tilt to wake should be false
        ProviderTestUtils.assertKeyValue(props,
                SettingsContract.KEY_AMBIENT_TILT_TO_WAKE, 0);
        // THEN prefs ambient tilt to wake should be false
        ProviderTestUtils.assertKeyValue(mPrefs,
                SettingsContract.KEY_AMBIENT_TILT_TO_WAKE, false);
        // THEN rows changed should be 1
        Assert.assertEquals("rows changed should be 1", 1, rowsChanged);
    }

    @Test
    public void testUpdate_ambientTiltToWake_falseToTrue() {
        // GIVEN flag KEY_AMBIENT_TILT_TO_WAKE is false
        mPrefs.edit().putBoolean(SettingsContract.KEY_AMBIENT_TILT_TO_WAKE, false).apply();
        // GIVEN ambient properties
        SettingProperties props = new AmbientProperties(mContext, mPrefs, mResources);

        // WHEN KEY_AMBIENT_TILT_TO_WAKE is updated to true
        int rowsChanged = props.update(ProviderTestUtils.getContentValues(
                SettingsContract.KEY_AMBIENT_TILT_TO_WAKE, 1));

        // THEN properties ambient tilt to wake should be true
        ProviderTestUtils.assertKeyValue(props,
                SettingsContract.KEY_AMBIENT_TILT_TO_WAKE, 1);
        // THEN prefs ambient tilt to wake should be true
        ProviderTestUtils.assertKeyValue(mPrefs,
                SettingsContract.KEY_AMBIENT_TILT_TO_WAKE, true);
        // THEN rows changed should be 1
        Assert.assertEquals("rows changed should be 1", 1, rowsChanged);
    }

    @Test
    public void testUpdate_ambientLowBitEnabledDev_trueToFalse() {
        // GIVEN flag KEY_AMBIENT_LOW_BIT_ENABLED_DEV is true
        mPrefs.edit().putBoolean(SettingsContract.KEY_AMBIENT_LOW_BIT_ENABLED_DEV, true).apply();
        // GIVEN ambient properties
        SettingProperties props = new AmbientProperties(mContext, mPrefs, mResources);

        // WHEN KEY_AMBIENT_LOW_BIT_ENABLED_DEV is updated to false
        int rowsChanged = props.update(ProviderTestUtils.getContentValues(
                SettingsContract.KEY_AMBIENT_LOW_BIT_ENABLED_DEV, 0));

        // THEN properties ambient low bit enabled dev should be false
        ProviderTestUtils.assertKeyValue(props,
                SettingsContract.KEY_AMBIENT_LOW_BIT_ENABLED_DEV, 0);
        // THEN prefs ambient low bit enabled dev should be false
        ProviderTestUtils.assertKeyValue(mPrefs,
                SettingsContract.KEY_AMBIENT_LOW_BIT_ENABLED_DEV, false);
        // THEN rows changed should be 1
        Assert.assertEquals("rows changed should be 1", 1, rowsChanged);
    }

    @Test
    public void testUpdate_ambientLowBitEnabledDev_falseToTrue() {
        // GIVEN flag KEY_AMBIENT_LOW_BIT_ENABLED_DEV is false
        mPrefs.edit().putBoolean(SettingsContract.KEY_AMBIENT_LOW_BIT_ENABLED_DEV, false).apply();
        // GIVEN ambient properties
        SettingProperties props = new AmbientProperties(mContext, mPrefs, mResources);

        // WHEN KEY_AMBIENT_LOW_BIT_ENABLED_DEV is updated to true
        int rowsChanged = props.update(ProviderTestUtils.getContentValues(
                SettingsContract.KEY_AMBIENT_LOW_BIT_ENABLED_DEV, 1));

        // THEN properties ambient low bit enabled dev should be true
        ProviderTestUtils.assertKeyValue(props,
                SettingsContract.KEY_AMBIENT_LOW_BIT_ENABLED_DEV, 1);
        // THEN prefs ambient low bit enabled dev should be true
        ProviderTestUtils.assertKeyValue(mPrefs,
                SettingsContract.KEY_AMBIENT_LOW_BIT_ENABLED_DEV, true);
        // THEN rows changed should be 1
        Assert.assertEquals("rows changed should be 1", 1, rowsChanged);
    }

    @Test
    public void testUpdate_ambientEnabled() {
        // GIVEN ambient properties
        SettingProperties props = new AmbientProperties(mContext, mPrefs, mResources);

        // WHEN KEY_AMBIENT_ENABLED is updated to false
        int rowsChanged = props.update(ProviderTestUtils.getContentValues(
                SettingsContract.KEY_AMBIENT_ENABLED, 0));

        // THEN service should have been started
        Assert.assertEquals(
                0,
                Settings.Secure.getInt(
                        mContext.getContentResolver(),
                        Settings.Secure.DOZE_ENABLED,
                        1));
        // THEN rows changed should be 1
        Assert.assertEquals("rows changed should be 1", 1, rowsChanged);
    }
}
