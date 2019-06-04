package com.google.android.clockwork.settings.provider;

import android.content.SharedPreferences;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.Test;

@RunWith(ClockworkRobolectricTestRunner.class)
public class WristGesturesPrefExistsPropertiesTest {
    @Rule public ExpectedException thrown = ExpectedException.none();
    private SharedPreferences mPrefs;

    @Before
    public void setup() {
        mPrefs = ProviderTestUtils.getEmptyPrefs();
    }

    @Test
    public void testQuery_exists() {
        // GIVEN KEY_WRIST_GESTURES_ENABLED exists
        mPrefs.edit().putBoolean(SettingsContract.KEY_WRIST_GESTURES_ENABLED, false).apply();

        // WHEN properties is created with the given string
        SettingProperties props = new WristGesturesPrefExistsProperties(mPrefs);

        // THEN wrist geatures enabled pref exists should be true
        ProviderTestUtils.assertKeyValue(props,
                SettingsContract.KEY_WRIST_GESTURES_ENABLED_PREF_EXISTS, 1);
    }


    @Test
    public void testQuery_missing() {
        // GIVEN KEY_WRIST_GESTURES_ENABLED doess not exist

        // WHEN properties is created with the given string
        SettingProperties props = new WristGesturesPrefExistsProperties(mPrefs);

        // THEN wrist geatures enabled pref exists should be true
        ProviderTestUtils.assertKeyValue(props,
                SettingsContract.KEY_WRIST_GESTURES_ENABLED_PREF_EXISTS, 0);
    }

    @Test
    public void testUpdate() {
        // GIVEN properties is created and KEY_WRIST_GESTURES_ENABLED exists
        mPrefs.edit().putBoolean(SettingsContract.KEY_WRIST_GESTURES_ENABLED, false).apply();
        SettingProperties props = new WristGesturesPrefExistsProperties(mPrefs);

        // WHEN properties is updated
        props.update(ProviderTestUtils.getContentValues(
                SettingsContract.KEY_WRIST_GESTURES_ENABLED_PREF_EXISTS, 0));

        // THEN the value remains unchanged
        ProviderTestUtils.assertKeyValue(props,
                SettingsContract.KEY_WRIST_GESTURES_ENABLED_PREF_EXISTS, 1);
    }
}
