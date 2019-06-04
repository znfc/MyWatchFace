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
public class WifiPowerSavePropertiesTest {
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
        // GIVEN Gkeys.DEFAULT_OFF_CHARGER_WIFI_USAGE_LIMIT_MINUTES is not defined

        // GIVEN overlay is defined
        Mockito.when(mRes.getInteger(R.integer.config_defaultOffChargerWifiUsageLimitMinutes))
                .thenReturn(17);

        // WHEN properties is instantiated
        SettingProperties props = mPropertiesMap.initWifiPowerSaveProperties(mPrefs, mRes);

        // THEN wifi power save should be defined value
        ProviderTestUtils.assertKeyValue(props, SettingsContract.KEY_WIFI_POWER_SAVE, 17);
    }
}
