package com.google.android.clockwork.settings.provider;

import android.content.ContentResolver;
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
public class WristGesturesPropertiesTest {
    @Mock ContentResolver mResolver;
    @Mock Resources mRes;
    private SharedPreferences mPrefs;

    @Before
    public void setup() {
        mPrefs = ProviderTestUtils.getEmptyPrefs();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testQuery() {
        // GIVEN resources gives a default of true
        Mockito.when(mRes.getBoolean(R.bool.config_wristGesturesDefaultEnabled)).thenReturn(true);

        // WHEN properties is instantiated
        SettingProperties props = new WristGesturesProperties(mPrefs, mRes, () -> mResolver);

        // THEN queried key should be true
        ProviderTestUtils.assertKeyValue(props,
                SettingsContract.KEY_WRIST_GESTURES_ENABLED, 1);
    }

    @Test
    public void testUpdate_noPreviousValue() {
        // GIVEN resources gives a default of false
        Mockito.when(mRes.getBoolean(R.bool.config_wristGesturesDefaultEnabled)).thenReturn(false);
        // GIVEN properties is created
        SettingProperties props = new WristGesturesProperties(mPrefs, mRes, () -> mResolver);

        // WHEN SettingsContract.KEY_WRIST_GESTURES_ENABLED is updated
        int rowsChanged = props.update(
                ProviderTestUtils.getContentValues(SettingsContract.KEY_WRIST_GESTURES_ENABLED, 1));

        // THEN queried key should be true
        ProviderTestUtils.assertKeyValue(props, SettingsContract.KEY_WRIST_GESTURES_ENABLED, 1);
        // THEN prefs key should be true
        ProviderTestUtils.assertKeyValue(mPrefs, SettingsContract.KEY_WRIST_GESTURES_ENABLED, true);
        // THEN rows changed should be 1
        Assert.assertEquals("rows changed should be 1", 1, rowsChanged);
        // THEN content resolver should be called
        Mockito.verify(mResolver).notifyChange(
                Mockito.eq(SettingsContract.WRIST_GESTURES_ENABLED_PREF_EXISTS_URI), Mockito.any());
    }

    @Test
    public void testUpdate_sameValueAsDefault() {
        // GIVEN resources gives a default of true
        Mockito.when(mRes.getBoolean(R.bool.config_wristGesturesDefaultEnabled)).thenReturn(true);
        // GIVEN properties is created
        SettingProperties props = new WristGesturesProperties(mPrefs, mRes, () -> mResolver);

        // WHEN SettingsContract.KEY_WRIST_GESTURES_ENABLED is updated
        int rowsChanged = props.update(
                ProviderTestUtils.getContentValues(SettingsContract.KEY_WRIST_GESTURES_ENABLED, 1));

        // THEN queried key should be true
        ProviderTestUtils.assertKeyValue(props, SettingsContract.KEY_WRIST_GESTURES_ENABLED, 1);
        // THEN prefs key should be true
        ProviderTestUtils.assertKeyValue(mPrefs, SettingsContract.KEY_WRIST_GESTURES_ENABLED, true);
        // THEN rows changed should be 1
        Assert.assertEquals("rows changed should be 1", 1, rowsChanged);
        // THEN content resolver should be called
        Mockito.verify(mResolver).notifyChange(
                Mockito.eq(SettingsContract.WRIST_GESTURES_ENABLED_PREF_EXISTS_URI), Mockito.any());
    }

    @Test
    public void testUpdate_sameValue() {
        // GIVEN mPrefs with key gives true
        mPrefs.edit().putBoolean(SettingsContract.KEY_WRIST_GESTURES_ENABLED, true).apply();

        // GIVEN properties is created
        SettingProperties props = new WristGesturesProperties(mPrefs, mRes, () -> mResolver);

        // WHEN SettingsContract.KEY_WRIST_GESTURES_ENABLED is updated
        int rowsChanged = props.update(
                ProviderTestUtils.getContentValues(SettingsContract.KEY_WRIST_GESTURES_ENABLED, 1));

        // THEN queried key should be true
        ProviderTestUtils.assertKeyValue(props, SettingsContract.KEY_WRIST_GESTURES_ENABLED, 1);
        // THEN prefs key should be true
        ProviderTestUtils.assertKeyValue(mPrefs, SettingsContract.KEY_WRIST_GESTURES_ENABLED, true);
        // THEN rows changed should be 0
        Assert.assertEquals("rows changed should be 0", 0, rowsChanged);
        // THEN content resolver should not be called
        Mockito.verify(mResolver, Mockito.never()).notifyChange(
                Mockito.eq(SettingsContract.WRIST_GESTURES_ENABLED_PREF_EXISTS_URI), Mockito.any());
        Mockito.verifyNoMoreInteractions(mResolver);
    }

}
