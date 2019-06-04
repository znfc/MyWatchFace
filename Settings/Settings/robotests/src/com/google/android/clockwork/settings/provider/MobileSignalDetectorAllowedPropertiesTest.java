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
public class MobileSignalDetectorAllowedPropertiesTest {
    @Mock private Resources mRes;
    private SharedPreferences mPrefs;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mPrefs = ProviderTestUtils.getEmptyPrefs();
    }

    /** Ensure false value is a string for legacy reasons. */
    @Test
    public void test_basicFalseStatic() {
        // GIVEN resources gives a default of true
        Mockito.when(mRes.getBoolean(R.bool.config_mobileSignalDetectorAllowed)).thenReturn(true);

        // GIVEN mPrefs with key gives false
        mPrefs.edit().putBoolean(SettingsContract.KEY_MOBILE_SIGNAL_DETECTOR, false).apply();

        // WHEN properties is created
        SettingProperties properties = new MobileSignalDetectorAllowedProperties(mPrefs, mRes);

        // THEN queried key should be false
        ProviderTestUtils.assertKeyValue(properties,
                SettingsContract.KEY_MOBILE_SIGNAL_DETECTOR, "false");
    }

    /** Ensure true value is a string for legacy reasons. */
    @Test
    public void test_basicTrueStatic() {
        // GIVEN resources gives a default of true
        Mockito.when(mRes.getBoolean(R.bool.config_mobileSignalDetectorAllowed)).thenReturn(true);

        // GIVEN mPrefs with key gives true
        mPrefs.edit().putBoolean(SettingsContract.KEY_MOBILE_SIGNAL_DETECTOR, true).apply();

        // WHEN properties is created
        SettingProperties properties = new MobileSignalDetectorAllowedProperties(mPrefs, mRes);

        // THEN queried key should be true
        ProviderTestUtils.assertKeyValue(properties,
                SettingsContract.KEY_MOBILE_SIGNAL_DETECTOR, "true");
    }

    @Test
    public void test_valueTrueParsedFromRes() {
        // GIVEN resources gives a default of true
        Mockito.when(mRes.getBoolean(R.bool.config_mobileSignalDetectorAllowed)).thenReturn(true);

        // WHEN properties is created
        SettingProperties properties = new MobileSignalDetectorAllowedProperties(mPrefs, mRes);

        // THEN queried key should be true
        ProviderTestUtils.assertKeyValue(properties,
                SettingsContract.KEY_MOBILE_SIGNAL_DETECTOR, "true");
    }

    @Test
    public void test_valueFalseParsedFromRes() {
        // GIVEN resources gives a default of false
        Mockito.when(mRes.getBoolean(R.bool.config_mobileSignalDetectorAllowed)).thenReturn(false);

        // WHEN properties is created
        SettingProperties properties = new MobileSignalDetectorAllowedProperties(mPrefs, mRes);

        // THEN queried key should be false
        ProviderTestUtils.assertKeyValue(properties,
                SettingsContract.KEY_MOBILE_SIGNAL_DETECTOR, "false");
    }
}
