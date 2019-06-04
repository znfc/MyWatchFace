package com.google.android.clockwork.settings.provider;

import android.content.Context;
import android.content.res.Resources;
import android.content.SharedPreferences;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(ClockworkRobolectricTestRunner.class)
public class SmartIlluminatePropertiesTest {
    @Mock private Resources mRes;
    private SharedPreferences mPrefs;
    private PropertiesMap mPropertiesMap;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mPrefs = ProviderTestUtils.getEmptyPrefs();
        mPropertiesMap = new PropertiesMap(this::getContext);
    }

    private Context getContext() {
        return RuntimeEnvironment.application;
    }

    @Test
    public void testQuery_missingGkeys() {
        // GIVEN Gkeys.SMART_ILLUMINATE_DEFAULT_SETTING is not defined

        // GIVEN overlay is defined
        Mockito.when(mRes.getBoolean(R.bool.config_defaultSmartIlluminateOn)).thenReturn(true);

        // WHEN properties is instantiated
        SettingProperties props = mPropertiesMap.initSmartIlluminateProperties(mPrefs, mRes);

        // THEN wifi power save should be defined value
        ProviderTestUtils.assertKeyValue(props, SettingsContract.KEY_SMART_ILLUMINATE_ENABLED, 1);
    }
}
