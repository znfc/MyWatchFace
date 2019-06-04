package com.google.android.clockwork.settings.provider;

import android.content.SharedPreferences;
import android.graphics.Color;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.Test;

@RunWith(ClockworkRobolectricTestRunner.class)
public class DisplayShapePropertiesTest {
    @Rule public ExpectedException thrown = ExpectedException.none();
    private SharedPreferences mPrefs;

    @Before
    public void setup() {
        mPrefs = ProviderTestUtils.getEmptyPrefs();
    }

    @Test
    public void testQuery_empty() {
        // GIVEN empty preferences

        // WHEN properties is instantiated
        SettingProperties props = new DisplayShapeProperties(mPrefs);

        // THEN contains KEY_DISPLAY_SHAPE
        ProviderTestUtils.assertKeyExists(props, SettingsContract.KEY_DISPLAY_SHAPE);
        // THEN contains KEY_BOTTOM_OFFSET
        ProviderTestUtils.assertKeyExists(props, SettingsContract.KEY_BOTTOM_OFFSET);
    }

    @Test
    public void testQuery_displayShape() {
        // GIVEN display shape is round
        mPrefs.edit().putInt(
                SettingsContract.KEY_DISPLAY_SHAPE, SettingsContract.DISPLAY_SHAPE_ROUND).apply();

        // WHEN properties is instantiated
        SettingProperties props = new DisplayShapeProperties(mPrefs);

        // THEN display shape should be round
        ProviderTestUtils.assertKeyValue(props,
                SettingsContract.KEY_DISPLAY_SHAPE, SettingsContract.DISPLAY_SHAPE_ROUND);
    }

    @Test
    public void testQuery_bottomOffset() {
        // GIVEN bottom offset is 47
        mPrefs.edit().putInt(SettingsContract.KEY_BOTTOM_OFFSET, 47).apply();

        // WHEN properties is instantiated
        SettingProperties props = new DisplayShapeProperties(mPrefs);

        // THEN bottom offset should be 47
        ProviderTestUtils.assertKeyValue(props, SettingsContract.KEY_BOTTOM_OFFSET, 47);
    }

    @Test
    public void testUpdate_displayShape() {
        // GIVEN flag KEY_DISPLAY_SHAPE is square
        mPrefs.edit().putInt(
                SettingsContract.KEY_DISPLAY_SHAPE, SettingsContract.DISPLAY_SHAPE_SQUARE).apply();
        // GIVEN properties is instantiated
        SettingProperties props = new DisplayShapeProperties(mPrefs);

        // WHEN KEY_DISPLAY_SHAPE is updated to round
        int rowsChanged = props.update(ProviderTestUtils.getContentValues(
                SettingsContract.KEY_DISPLAY_SHAPE, SettingsContract.DISPLAY_SHAPE_ROUND));

        // THEN queried key should be round
        ProviderTestUtils.assertKeyValue(props, mPrefs,
                SettingsContract.KEY_DISPLAY_SHAPE, SettingsContract.DISPLAY_SHAPE_ROUND);
        // THEN rows changed should be 1
        Assert.assertEquals("rows changed should be 1", 1, rowsChanged);
    }

    @Test
    public void testUpdate_displayShape_invalid() {
        // GIVEN flag KEY_DISPLAY_SHAPE is square
        mPrefs.edit().putInt(
                SettingsContract.KEY_DISPLAY_SHAPE, SettingsContract.DISPLAY_SHAPE_SQUARE).apply();
        // GIVEN properties is instantiated
        SettingProperties props = new DisplayShapeProperties(mPrefs);

        // THROWS IllegalArgumentException
        thrown.expect(IllegalArgumentException.class);
        // WHEN KEY_DISPLAY_SHAPE is updated to invlid value
        props.update(ProviderTestUtils.getContentValues(
                SettingsContract.KEY_DISPLAY_SHAPE, Integer.MAX_VALUE));
    }

    @Test
    public void testUpdate_bottomOffset() {
        // GIVEN flag KEY_BOTTOM_OFFSET is 49
        mPrefs.edit().putInt(SettingsContract.KEY_BOTTOM_OFFSET, 49).apply();
        // GIVEN properties is instantiated
        SettingProperties props = new DisplayShapeProperties(mPrefs);

        // WHEN KEY_BOTTOM_OFFSET is updated to 51
        int rowsChanged = props.update(ProviderTestUtils.getContentValues(
                SettingsContract.KEY_BOTTOM_OFFSET, 51));

        // THEN queried key should be round
        ProviderTestUtils.assertKeyValue(props, mPrefs, SettingsContract.KEY_BOTTOM_OFFSET, 51);
        // THEN rows changed should be 1
        Assert.assertEquals("rows changed should be 1", 1, rowsChanged);
    }
}
