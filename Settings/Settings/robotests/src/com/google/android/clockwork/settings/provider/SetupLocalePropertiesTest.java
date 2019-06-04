package com.google.android.clockwork.settings.provider;

import android.content.SharedPreferences;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.utils.FeatureManager;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

@RunWith(ClockworkRobolectricTestRunner.class)
public class SetupLocalePropertiesTest {
    private static final String TEST_LOCALE = "fr-CA";
    @Mock FeatureManager mFeatureManager;
    private SharedPreferences mPrefs;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mPrefs = ProviderTestUtils.getEmptyPrefs();
    }

    @Test
    public void testQuery_enableAllLanguages() {
        // GIVEN device is not local edition
        Mockito.when(mFeatureManager.isLocalEditionDevice()).thenReturn(false);

        // WHEN properties is instantiated
        SettingProperties props = new SetupLocaleProperties(mPrefs, mFeatureManager);

        // THEN all languages should be enabled
        ProviderTestUtils.assertKeyValue(props, SettingsContract.KEY_ENABLE_ALL_LANGUAGES, 1);
    }

    @Test
    public void testQuery_setupLocale_default() {
        // WHEN properties is instantiated
        SettingProperties props = new SetupLocaleProperties(mPrefs, mFeatureManager);

        // THEN setup locale should exist
        ProviderTestUtils.assertKeyExists(props, SettingsContract.KEY_SETUP_LOCALE);
    }

    @Test
    public void testQuery_setupLocale_exists() {
        // GIVEN locale defined in mPrefs
        mPrefs.edit().putString(SettingsContract.KEY_SETUP_LOCALE, TEST_LOCALE).apply();

        // WHEN properties is instantiated
        SettingProperties props = new SetupLocaleProperties(mPrefs, mFeatureManager);

        // THEN setup locale should exist
        ProviderTestUtils.assertKeyValue(props, SettingsContract.KEY_SETUP_LOCALE, TEST_LOCALE);
    }

    @Test
    public void testUpdate_setupLocale_noPreviousValue() {
        // GIVEN properties is created
        SettingProperties props = new SetupLocaleProperties(mPrefs, mFeatureManager);

        // WHEN KEY_SETUP_LOCALE is updated
        int rowsChanged = props.update(
                ProviderTestUtils.getContentValues(SettingsContract.KEY_SETUP_LOCALE, TEST_LOCALE));

        // THEN queried key should be same as test locale
        ProviderTestUtils.assertKeyValue(props, SettingsContract.KEY_SETUP_LOCALE, TEST_LOCALE);
        // THEN prefs key should be same as test locale
        ProviderTestUtils.assertKeyValue(mPrefs, SettingsContract.KEY_SETUP_LOCALE, TEST_LOCALE);
        // THEN rows changed should be 1
        Assert.assertEquals("rows changed should be 1", 1, rowsChanged);
    }

    @Test
    public void testUpdate_setupLocale_differentPreviousValue() {
        // GIVEN mPrefs with another locale
        mPrefs.edit().putString(SettingsContract.KEY_SETUP_LOCALE, "ja").apply();

        // GIVEN properties is created
        SettingProperties props = new SetupLocaleProperties(mPrefs, mFeatureManager);

        // WHEN KEY_SETUP_LOCALE is updated
        int rowsChanged = props.update(
                ProviderTestUtils.getContentValues(SettingsContract.KEY_SETUP_LOCALE, TEST_LOCALE));

        // THEN queried key should be same as test locale
        ProviderTestUtils.assertKeyValue(props, SettingsContract.KEY_SETUP_LOCALE, TEST_LOCALE);
        // THEN prefs key should be same as test locale
        ProviderTestUtils.assertKeyValue(mPrefs, SettingsContract.KEY_SETUP_LOCALE, TEST_LOCALE);
        // THEN rows changed should be 1
        Assert.assertEquals("rows changed should be 1", 1, rowsChanged);
    }

    @Test
    public void testUpdate_setupLocale_sameValue() {
        // GIVEN locale defined in mPrefs
        mPrefs.edit().putString(SettingsContract.KEY_SETUP_LOCALE, TEST_LOCALE).apply();

        // GIVEN properties is created
        SettingProperties props = new SetupLocaleProperties(mPrefs, mFeatureManager);

        // WHEN KEY_SETUP_LOCALE is updated
        int rowsChanged = props.update(ProviderTestUtils.getContentValues(
                SettingsContract.KEY_SETUP_LOCALE, TEST_LOCALE));

        // THEN queried key should be same as test locale
        ProviderTestUtils.assertKeyValue(props, SettingsContract.KEY_SETUP_LOCALE, TEST_LOCALE);
        // THEN prefs key should be same as test locale
        ProviderTestUtils.assertKeyValue(mPrefs, SettingsContract.KEY_SETUP_LOCALE, TEST_LOCALE);
        // THEN rows changed should be 0
        Assert.assertEquals("rows changed should be 0", 0, rowsChanged);
    }
}
