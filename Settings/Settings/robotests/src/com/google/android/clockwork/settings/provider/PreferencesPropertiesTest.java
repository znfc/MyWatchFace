package com.google.android.clockwork.settings.provider;

import android.content.SharedPreferences;

import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.mockito.Mockito;

@RunWith(ClockworkRobolectricTestRunner.class)
public class PreferencesPropertiesTest {
    private static final String TEST_KEY = "test_key";
    private static final String TEST_PATH = "test_path";

    @Rule public ExpectedException thrown = ExpectedException.none();
    private SharedPreferences mPrefs;

    @Before
    public void setup() {
        mPrefs = ProviderTestUtils.getEmptyPrefs();
    }

    @Test
    public void testInit() {
        // GIVEN mPrefs

        // WHEN properties is created
        PreferencesProperties props = new PreferencesProperties(mPrefs, TEST_PATH);

        // THEN stored mPrefs should be the given mPrefs
        Assert.assertEquals(mPrefs, props.mPrefs);
    }

    @Test
    public void testQuery_addBoolean_basicFalseStatic() {
        // GIVEN mPrefs with key gives false
        mPrefs.edit().putBoolean(TEST_KEY, false).apply();

        // WHEN properties is created
        SettingProperties props = new PreferencesProperties(mPrefs, TEST_PATH)
                .addBoolean(TEST_KEY, true);

        // THEN queried key should be false
        ProviderTestUtils.assertKeyValue(props, TEST_KEY, 0);
    }

    @Test
    public void testQuery_addBoolean_basicTrueStatic() {
        // GIVEN mPrefs with key gives true
        mPrefs.edit().putBoolean(TEST_KEY, true).apply();

        // WHEN properties is created
        SettingProperties props = new PreferencesProperties(mPrefs, TEST_PATH)
                .addBoolean(TEST_KEY, false);

        // THEN queried key should be true
        ProviderTestUtils.assertKeyValue(props, TEST_KEY, 1);
    }

    @Test
    public void testQuery_addBoolean_defaultValueSupplier_calledIfNeeded() {
        // GIVEN TEST_KEY is not supplied in mPrefs
        // GIVEN defaultValueSupplier is supplied
        BooleanSupplier defaultValueSupplier = Mockito.mock(BooleanSupplier.class);
        // GIVEN defaultValueSupplier returns false
        Mockito.when(defaultValueSupplier.getAsBoolean()).thenReturn(true);

        // WHEN properties is created
        SettingProperties props = new PreferencesProperties(mPrefs, TEST_PATH)
                .addBoolean(TEST_KEY, defaultValueSupplier);

        // THEN queried key should be true
        ProviderTestUtils.assertKeyValue(props, TEST_KEY, 1);
        // THEN defaultValueSupplier should be called
        Mockito.verify(defaultValueSupplier).getAsBoolean();
    }

    @Test
    public void testQuery_addBoolean_defaultValueSupplier_notCalledIfNotNeeded() {
        // GIVEN TEST_KEY is not supplied in mPrefs
        // GIVEN mPrefs with TEST_KEY gives true
        mPrefs.edit().putBoolean(TEST_KEY, true).apply();
        // GIVEN defaultValueSupplier is supplied
        BooleanSupplier defaultValueSupplier = Mockito.mock(BooleanSupplier.class);
        // GIVEN defaultValueSupplier returns false
        Mockito.when(defaultValueSupplier.getAsBoolean()).thenReturn(false);

        // WHEN properties is created
        SettingProperties props = new PreferencesProperties(mPrefs, TEST_PATH)
                .addBoolean(TEST_KEY, defaultValueSupplier);

        // THEN queried key should be true
        ProviderTestUtils.assertKeyValue(props, TEST_KEY, 1);
        // THEN defaultValueSupplier should not be called
        Mockito.verify(defaultValueSupplier, Mockito.never()).getAsBoolean();
    }

    @Test
    public void testUpdate_addBoolean_noPreviousValue() {
        // GIVEN properties is created
        SettingProperties props = new PreferencesProperties(mPrefs, TEST_PATH)
                .addBoolean(TEST_KEY, false);

        // WHEN TEST_KEY is updated
        int rowsChanged = props.update(ProviderTestUtils.getContentValues(TEST_KEY, 1));

        // THEN queried key should be true
        ProviderTestUtils.assertKeyValue(props, TEST_KEY, 1);
        // THEN prefs key should be true
        ProviderTestUtils.assertKeyValue(mPrefs, TEST_KEY, true);
        // THEN rows changed should be 1
        Assert.assertEquals("rows changed should be 1", 1, rowsChanged);
    }

    @Test
    public void testUpdate_addBoolean_differentPreviousValue() {
        // GIVEN mPrefs with key gives true
        mPrefs.edit().putBoolean(TEST_KEY, true).apply();

        // GIVEN properties is created
        SettingProperties props = new PreferencesProperties(mPrefs, TEST_PATH)
                .addBoolean(TEST_KEY, false);

        // WHEN TEST_KEY is updated
        int rowsChanged = props.update(ProviderTestUtils.getContentValues(TEST_KEY, 0));

        // THEN queried key should be false
        ProviderTestUtils.assertKeyValue(props, TEST_KEY, 0);
        // THEN prefs key should be false
        ProviderTestUtils.assertKeyValue(mPrefs, TEST_KEY, false);
        // THEN rows changed should be 1
        Assert.assertEquals("rows changed should be 1", 1, rowsChanged);
    }

    @Test
    public void testUpdate_addBoolean_sameValueAsDefault() {
        // GIVEN properties is created
        SettingProperties props = new PreferencesProperties(mPrefs, TEST_PATH)
                .addBoolean(TEST_KEY, true);

        // WHEN TEST_KEY is updated
        int rowsChanged = props.update(ProviderTestUtils.getContentValues(TEST_KEY, 1));

        // THEN queried key should be true
        ProviderTestUtils.assertKeyValue(props, TEST_KEY, 1);
        // THEN prefs key should be true
        ProviderTestUtils.assertKeyValue(mPrefs, TEST_KEY, true);
        // THEN rows changed should be 1
        Assert.assertEquals("rows changed should be 1", 1, rowsChanged);
    }

    @Test
    public void testUpdate_addBoolean_sameValue() {
        // GIVEN mPrefs with key gives true
        mPrefs.edit().putBoolean(TEST_KEY, true).apply();

        // GIVEN properties is created
        SettingProperties props = new PreferencesProperties(mPrefs, TEST_PATH)
                .addBoolean(TEST_KEY, true);

        // WHEN TEST_KEY is updated
        int rowsChanged = props.update(ProviderTestUtils.getContentValues(TEST_KEY, 1));

        // THEN queried key should be true
        ProviderTestUtils.assertKeyValue(props, TEST_KEY, 1);
        // THEN prefs key should be true
        ProviderTestUtils.assertKeyValue(mPrefs, TEST_KEY, true);
        // THEN rows changed should be 0
        Assert.assertEquals("rows changed should be 0", 0, rowsChanged);
    }

    @Test
    public void testQuery_addInt_basicStatic() {
        // GIVEN mPrefs with key gives 1234
        mPrefs.edit().putInt(TEST_KEY, 1234).apply();

        // WHEN properties is created
        SettingProperties props = new PreferencesProperties(mPrefs, TEST_PATH).addInt(TEST_KEY, 0);

        // THEN queried key should be 1234
        ProviderTestUtils.assertKeyValue(props, TEST_KEY, 1234);
    }

    @Test
    public void testQuery_addInt_defaultValue_valueMissing() {
        // GIVEN TEST_KEY is not supplied in mPrefs

        // WHEN properties is created
        SettingProperties props = new PreferencesProperties(mPrefs, TEST_PATH)
                .addInt(TEST_KEY, 444);

        // THEN queried key should be 444
        ProviderTestUtils.assertKeyValue(props, TEST_KEY, 444);
    }

    @Test
    public void testQuery_addInt_defaultValue_valuePresent() {
        // GIVEN mPrefs with TEST_KEY gives 151
        mPrefs.edit().putInt(TEST_KEY, 151).apply();

        // WHEN properties is created
        SettingProperties props = new PreferencesProperties(mPrefs, TEST_PATH)
                .addInt(TEST_KEY, 9542);

        // THEN queried key should be 151
        ProviderTestUtils.assertKeyValue(props, TEST_KEY, 151);
    }

    @Test
    public void testQuery_addInt_defaultValueSupplier_calledIfNeeded() {
        // GIVEN TEST_KEY is not supplied in mPrefs
        // GIVEN defaultValueSupplier is supplied
        IntSupplier defaultValueSupplier = Mockito.mock(IntSupplier.class);
        // GIVEN defaultValueSupplier returns false
        Mockito.when(defaultValueSupplier.getAsInt()).thenReturn(251);

        // WHEN properties is created
        SettingProperties props = new PreferencesProperties(mPrefs, TEST_PATH)
                .addInt(TEST_KEY, defaultValueSupplier);

        // THEN queried key should be 251
        ProviderTestUtils.assertKeyValue(props, TEST_KEY, 251);
        // THEN defaultValueSupplier should be called
        Mockito.verify(defaultValueSupplier).getAsInt();
    }

    @Test
    public void testQuery_addInt_defaultValueSupplier_notCalledIfNotNeeded() {
        // GIVEN mPrefs with TEST_KEY gives 222
        mPrefs.edit().putInt(TEST_KEY, 222).apply();
        // GIVEN defaultValueSupplier is supplied
        IntSupplier defaultValueSupplier = Mockito.mock(IntSupplier.class);
        // GIVEN defaultValueSupplier returns false
        Mockito.when(defaultValueSupplier.getAsInt()).thenReturn(111);

        // WHEN properties is created
        SettingProperties props = new PreferencesProperties(mPrefs, TEST_PATH)
                .addInt(TEST_KEY, defaultValueSupplier);

        // THEN queried key should be 222
        ProviderTestUtils.assertKeyValue(props, mPrefs, TEST_KEY, 222);
        // THEN defaultValueSupplier should not be called
        Mockito.verify(defaultValueSupplier, Mockito.never()).getAsInt();
    }

    @Test
    public void testUpdate_addInt_noValidValuesSpecified() {
        // GIVEN properties is created
        SettingProperties props = new PreferencesProperties(mPrefs, TEST_PATH)
                .addInt(TEST_KEY, 111);

        // WHEN TEST_KEY is updated
        int rowsChanged = props.update(ProviderTestUtils.getContentValues(
                TEST_KEY, 555));

        // THEN queried key should be 555
        ProviderTestUtils.assertKeyValue(props, mPrefs, TEST_KEY, 555);
        // THEN rows changed should be 1
        Assert.assertEquals("rows changed should be 1", 1, rowsChanged);
    }

    @Test
    public void testUpdate_addInt_validValuesSpecified() {
        // GIVEN properties is created
        SettingProperties props = new PreferencesProperties(mPrefs, TEST_PATH)
                .addInt(TEST_KEY, 167,
                        // valid values:
                        241, 167);

        // WHEN TEST_KEY is updated
        int rowsChanged = props.update(ProviderTestUtils.getContentValues(
                TEST_KEY, 241));

        // THEN queried key should be 241
        ProviderTestUtils.assertKeyValue(props, TEST_KEY, 241);
        // THEN rows changed should be 1
        Assert.assertEquals("rows changed should be 1", 1, rowsChanged);
    }

    @Test
    public void testUpdate_addInt_validValuesSpecified_wrongValue() {
        // GIVEN mPrefs with TEST_KEY gives 167
        mPrefs.edit().putInt(TEST_KEY, 167).apply();
        // GIVEN properties is created
        SettingProperties props = new PreferencesProperties(mPrefs, TEST_PATH)
                .addInt(TEST_KEY, 0,
                        // valid values:
                        167);

        // THROWS IllegalArgumentException
        thrown.expect(IllegalArgumentException.class);
        // WHEN TEST_KEY is updated
        int rowsChanged = props.update(ProviderTestUtils.getContentValues(
                TEST_KEY, 47));
    }

    @Test
    public void testUpdate_addInt_sameValueAsDefault() {
        // GIVEN properties is created
        SettingProperties props = new PreferencesProperties(mPrefs, TEST_PATH)
                .addInt(TEST_KEY, 575);

        // WHEN TEST_KEY is updated
        int rowsChanged = props.update(ProviderTestUtils.getContentValues(
                TEST_KEY, 575));

        // THEN queried key should be 575
        ProviderTestUtils.assertKeyValue(props, mPrefs, TEST_KEY, 575);
        // THEN rows changed should be 1
        Assert.assertEquals("rows changed should be 1", 1, rowsChanged);
    }

    @Test
    public void testUpdate_addInt_sameValue() {
        // GIVEN mPrefs with key gives 882
        mPrefs.edit().putInt(TEST_KEY, 882).apply();

        // GIVEN properties is created
        SettingProperties props = new PreferencesProperties(mPrefs, TEST_PATH)
                .addInt(TEST_KEY, 882);

        // WHEN TEST_KEY is updated
        int rowsChanged = props.update(ProviderTestUtils.getContentValues(
                TEST_KEY, 882));

        // THEN queried key should be 882
        ProviderTestUtils.assertKeyValue(props, mPrefs, TEST_KEY, 882);
        // THEN rows changed should be 0
        Assert.assertEquals("rows changed should be 0", 0, rowsChanged);
    }

    @Test
    public void testQuery_addLong_basicStatic() {
        // GIVEN mPrefs with key gives 1234
        mPrefs.edit().putLong(TEST_KEY, 1234L).apply();

        // WHEN properties is created
        PreferencesProperties props = new PreferencesProperties(mPrefs, TEST_PATH)
                .addLong(TEST_KEY, 1234L);

        // THEN queried key should be 1234
        ProviderTestUtils.assertKeyValue(props, TEST_KEY, 1234L);
    }

    @Test
    public void testQuery_addLong_defaultValue_valueMissing() {
        // GIVEN TEST_KEY is not supplied in mPrefs

        // WHEN properties is created
        PreferencesProperties props = new PreferencesProperties(mPrefs, TEST_PATH)
                .addLong(TEST_KEY, 444L);

        // THEN queried key should be 444
        ProviderTestUtils.assertKeyValue(props, TEST_KEY, 444L);
    }

    @Test
    public void testQuery_addLong_defaultValue_valuePresent() {
        // GIVEN mPrefs with TEST_KEY gives 151
        mPrefs.edit().putLong(TEST_KEY, 151L).apply();

        // WHEN properties is created
        PreferencesProperties props = new PreferencesProperties(mPrefs, TEST_PATH)
                .addLong(TEST_KEY, 9542L);

        // THEN queried key should be 151
        ProviderTestUtils.assertKeyValue(props, TEST_KEY, 151L);
    }

    @Test
    public void testUpdate_addLong() {
        // GIVEN properties is created
        PreferencesProperties props = new PreferencesProperties(mPrefs, TEST_PATH)
                .addLong(TEST_KEY, 111L);

        // WHEN TEST_KEY is updated
        int rowsChanged = props.update(ProviderTestUtils.getContentValues(TEST_KEY, 555L));

        // THEN queried key should be 555
        ProviderTestUtils.assertKeyValue(props, mPrefs, TEST_KEY, 555L);
        // THEN rows changed should be 1
        Assert.assertEquals("rows changed should be 1", 1, rowsChanged);
    }

    @Test
    public void testUpdate_addLong_sameValueAsDefault() {
        // GIVEN properties is created
        PreferencesProperties props = new PreferencesProperties(mPrefs, TEST_PATH)
                .addLong(TEST_KEY, 575L);

        // WHEN TEST_KEY is updated
        int rowsChanged = props.update(ProviderTestUtils.getContentValues(TEST_KEY, 575L));

        // THEN queried key should be 575
        ProviderTestUtils.assertKeyValue(props, mPrefs, TEST_KEY, 575L);
        // THEN rows changed should be 1
        Assert.assertEquals("rows changed should be 1", 1, rowsChanged);
    }

    @Test
    public void testUpdate_addLong_sameValue() {
        // GIVEN mPrefs with key gives 882
        mPrefs.edit().putLong(TEST_KEY, 882L).apply();
        // GIVEN properties is created
        PreferencesProperties props = new PreferencesProperties(mPrefs, TEST_PATH)
                .addLong(TEST_KEY, 882L);

        // WHEN TEST_KEY is updated
        int rowsChanged = props.update(ProviderTestUtils.getContentValues(TEST_KEY, 882L));

        // THEN queried key should be 882
        ProviderTestUtils.assertKeyValue(props, mPrefs, TEST_KEY, 882L);
        // THEN rows changed should be 0
        Assert.assertEquals("rows changed should be 0", 0, rowsChanged);
    }


    /** Persist a basic string. */
    @Test
    public void testQuery_addImmutable_basic() {
        // GIVEN a String
        String s = "a given string";

        // WHEN properties is created with the given string
        SettingProperties props = new PreferencesProperties(mPrefs, TEST_PATH)
                .addImmutable(TEST_KEY, s);

        // THEN queried key should match given string
        ProviderTestUtils.assertKeyValue(props, TEST_KEY, s);
    }

    /** Value is immutable, throw an exception if something attempts to alter value. */
    @Test
    public void test_updateFails() {
        // GIVEN properties is created and mPrefs with key gives 882
        mPrefs.edit().putLong(TEST_KEY, 882L).apply();
        SettingProperties props = new PreferencesProperties(mPrefs, TEST_PATH)
                .addImmutable(TEST_KEY, 882L);

        // WHEN properties is updated
        props.update(ProviderTestUtils.getContentValues(TEST_KEY, 100L));

        // THEN the value remains unchanged
        ProviderTestUtils.assertKeyValue(props, mPrefs, TEST_KEY, 882L);
    }
}
