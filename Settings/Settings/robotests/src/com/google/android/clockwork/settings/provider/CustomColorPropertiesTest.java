package com.google.android.clockwork.settings.provider;

import android.content.SharedPreferences;
import android.graphics.Color;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.Test;

@RunWith(ClockworkRobolectricTestRunner.class)
public class CustomColorPropertiesTest {
    private SharedPreferences mPrefs;

    @Before
    public void setup() {
        mPrefs = ProviderTestUtils.getEmptyPrefs();
    }

    @Test
    public void testQuery_customColorForeground_null() {
        // GIVEN customColorForeground is missing

        // WHEN properties is instantiated
        SettingProperties props = new CustomColorProperties(mPrefs);

        // THEN customColorForeground should be null
        ProviderTestUtils.assertKeyValue(props, SettingsContract.KEY_CUSTOM_COLOR_FOREGROUND, null);
    }

    @Test
    public void testQuery_customColorBackground_null() {
        // GIVEN customColorBackground is missing

        // WHEN properties is instantiated
        SettingProperties props = new CustomColorProperties(mPrefs);

        // THEN customColorBackground should be null
        ProviderTestUtils.assertKeyValue(props, SettingsContract.KEY_CUSTOM_COLOR_BACKGROUND, null);
    }

    @Test
    public void testQuery_customColorForeground() {
        // GIVEN customColorForeground is red
        mPrefs.edit().putInt(SettingsContract.KEY_CUSTOM_COLOR_FOREGROUND, Color.RED).apply();

        // WHEN properties is instantiated
        SettingProperties props = new CustomColorProperties(mPrefs);

        // THEN customColorForeground should be red
        ProviderTestUtils.assertKeyValue(props,
                SettingsContract.KEY_CUSTOM_COLOR_FOREGROUND, Color.RED);
    }

    @Test
    public void testQuery_customColorBackground() {
        // GIVEN customColorBackground is red
        mPrefs.edit().putInt(SettingsContract.KEY_CUSTOM_COLOR_BACKGROUND, Color.RED).apply();

        // WHEN properties is instantiated
        SettingProperties props = new CustomColorProperties(mPrefs);

        // THEN customColorBackground should be red
        ProviderTestUtils.assertKeyValue(props,
                SettingsContract.KEY_CUSTOM_COLOR_BACKGROUND, Color.RED);
    }

    @Test
    public void testUpdate_customColorForeground_valueToValue() {
        // GIVEN flag KEY_CUSTOM_COLOR_FOREGROUND is red
        mPrefs.edit().putInt(SettingsContract.KEY_CUSTOM_COLOR_FOREGROUND, Color.RED).apply();
        // GIVEN properties is instantiated
        SettingProperties props = new CustomColorProperties(mPrefs);

        // WHEN KEY_CUSTOM_COLOR_FOREGROUND is updated to blue
        int rowsChanged = props.update(ProviderTestUtils.getContentValues(
                SettingsContract.KEY_CUSTOM_COLOR_FOREGROUND, Color.BLUE));

        // THEN queried key should be blue
        ProviderTestUtils.assertKeyValue(props, mPrefs,
                SettingsContract.KEY_CUSTOM_COLOR_FOREGROUND, Color.BLUE);
        // THEN rows changed should be 1
        Assert.assertEquals("rows changed should be 1", 1, rowsChanged);
    }

    @Test
    public void testUpdate_customColorBackground_valueToValue() {
        // GIVEN flag KEY_CUSTOM_COLOR_BACKGROUND is red
        mPrefs.edit().putInt(SettingsContract.KEY_CUSTOM_COLOR_BACKGROUND, Color.RED).apply();
        // GIVEN properties is instantiated
        SettingProperties props = new CustomColorProperties(mPrefs);

        // WHEN KEY_CUSTOM_COLOR_BACKGROUND is updated to blue
        int rowsChanged = props.update(ProviderTestUtils.getContentValues(
                SettingsContract.KEY_CUSTOM_COLOR_BACKGROUND, Color.BLUE));

        // THEN queried key should be blue
        ProviderTestUtils.assertKeyValue(props, mPrefs,
                SettingsContract.KEY_CUSTOM_COLOR_BACKGROUND, Color.BLUE);
        // THEN rows changed should be 1
        Assert.assertEquals("rows changed should be 1", 1, rowsChanged);
    }

    @Test
    public void testUpdate_customColorForeground_valueToNull() {
        // GIVEN flag KEY_CUSTOM_COLOR_FOREGROUND is red
        mPrefs.edit().putInt(SettingsContract.KEY_CUSTOM_COLOR_FOREGROUND, Color.RED).apply();
        // GIVEN properties is instantiated
        SettingProperties props = new CustomColorProperties(mPrefs);

        // WHEN KEY_CUSTOM_COLOR_FOREGROUND is updated to null
        int rowsChanged = props.update(ProviderTestUtils.getContentValues(
                SettingsContract.KEY_CUSTOM_COLOR_FOREGROUND, null));

        // THEN queried key should be null
        ProviderTestUtils.assertKeyValue(props,
                SettingsContract.KEY_CUSTOM_COLOR_FOREGROUND, null);
        // THEN mPrefs should be not have key
        ProviderTestUtils.assertKeyNotExists(mPrefs,
                SettingsContract.KEY_CUSTOM_COLOR_FOREGROUND);
        // THEN rows changed should be 1
        Assert.assertEquals("rows changed should be 1", 1, rowsChanged);
    }

    @Test
    public void testUpdate_customColorBackground_valueToNull() {
        // GIVEN flag KEY_CUSTOM_COLOR_BACKGROUND is red
        mPrefs.edit().putInt(SettingsContract.KEY_CUSTOM_COLOR_BACKGROUND, Color.RED).apply();
        // GIVEN properties is instantiated
        SettingProperties props = new CustomColorProperties(mPrefs);

        // WHEN KEY_CUSTOM_COLOR_BACKGROUND is updated to null
        int rowsChanged = props.update(ProviderTestUtils.getContentValues(
                SettingsContract.KEY_CUSTOM_COLOR_BACKGROUND, null));

        // THEN queried key should be null
        ProviderTestUtils.assertKeyValue(props,
                SettingsContract.KEY_CUSTOM_COLOR_BACKGROUND, null);
        // THEN mPrefs should be not have key
        ProviderTestUtils.assertKeyNotExists(mPrefs,
                SettingsContract.KEY_CUSTOM_COLOR_BACKGROUND);
        // THEN rows changed should be 1
        Assert.assertEquals("rows changed should be 1", 1, rowsChanged);
    }

    @Test
    public void testUpdate_customColorForeground_nullToValue() {
        // GIVEN flag KEY_CUSTOM_COLOR_FOREGROUND is missing

        // GIVEN properties is instantiated
        SettingProperties props = new CustomColorProperties(mPrefs);

        // WHEN KEY_CUSTOM_COLOR_FOREGROUND is updated to blue
        int rowsChanged = props.update(ProviderTestUtils.getContentValues(
                SettingsContract.KEY_CUSTOM_COLOR_FOREGROUND, Color.BLUE));

        // THEN queried key should be blue
        ProviderTestUtils.assertKeyValue(props, mPrefs,
                SettingsContract.KEY_CUSTOM_COLOR_FOREGROUND, Color.BLUE);
        // THEN rows changed should be 1
        Assert.assertEquals("rows changed should be 1", 1, rowsChanged);
    }

    @Test
    public void testUpdate_customColorBackground_nullToValue() {
        // GIVEN flag KEY_CUSTOM_COLOR_BACKGROUND is missing

        // GIVEN properties is instantiated
        SettingProperties props = new CustomColorProperties(mPrefs);

        // WHEN KEY_CUSTOM_COLOR_BACKGROUND is updated to blue
        int rowsChanged = props.update(ProviderTestUtils.getContentValues(
                SettingsContract.KEY_CUSTOM_COLOR_BACKGROUND, Color.BLUE));

        // THEN queried key should be blue
        ProviderTestUtils.assertKeyValue(props, mPrefs,
                SettingsContract.KEY_CUSTOM_COLOR_BACKGROUND, Color.BLUE);
        // THEN rows changed should be 1
        Assert.assertEquals("rows changed should be 1", 1, rowsChanged);
    }
}
