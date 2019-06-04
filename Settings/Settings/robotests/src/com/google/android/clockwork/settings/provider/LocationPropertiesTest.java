package com.google.android.clockwork.settings.provider;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.text.TextUtils;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.utils.FeatureManager;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import java.util.function.BooleanSupplier;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(ClockworkRobolectricTestRunner.class)
public class LocationPropertiesTest {
    @Mock private PackageManager mPackageManager;
    private BooleanSupplier mLocationEnabledSupplier;
    private boolean mLocationEnabled;
    private SharedPreferences mPrefs;
    private PropertiesMap mPropertiesMap;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mPrefs = ProviderTestUtils.getEmptyPrefs();
        mLocationEnabledSupplier = () -> mLocationEnabled;
        ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.authority = SettingsContract.SETTINGS_AUTHORITY;
        mPropertiesMap = new PropertiesMap(
                this::getContext,
                getContext().getSharedPreferences("mock", Context.MODE_PRIVATE),
                getContext().getResources(),
                () -> getContext().getPackageManager(),
                FeatureManager.INSTANCE.get(getContext()),
                () -> getContext().getContentResolver(),
                mLocationEnabledSupplier,
                new DefaultServiceStarter(this::getContext));
    }
  
    private Context getContext() {
        return RuntimeEnvironment.application;
    }

    @Test
    public void testQuery_hasGps_locationDisabled() {
        // GIVEN flag KEY_OBTAIN_PAIRED_DEVICE_LOCATION is not defined in mPrefs
        // GIVEN device has GPS
        Mockito.when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS))
                .thenReturn(true);
        // GIVEN location disabled
        mLocationEnabled = false;

        // WHEN LocationProperties is instantiated
        SettingProperties props = mPropertiesMap.initLocationProperties(
                mPrefs, () -> mPackageManager, mLocationEnabledSupplier);

        // THEN ObtainPairedDeviceLocation should be false
        ProviderTestUtils.assertKeyValue(props,
                SettingsContract.KEY_OBTAIN_PAIRED_DEVICE_LOCATION,
                SettingsContract.VALUE_FALSE);
    }

    @Test
    public void testQuery_hasGps_locationEnabled() {
        // GIVEN flag KEY_OBTAIN_PAIRED_DEVICE_LOCATION is not defined
        // GIVEN device has GPS
        Mockito.when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS))
                .thenReturn(true);
        // GIVEN location enabled
        mLocationEnabled = true;

        // WHEN LocationProperties is instantiated
        SettingProperties props = mPropertiesMap.initLocationProperties(
                mPrefs, () -> mPackageManager, mLocationEnabledSupplier);

        // THEN ObtainPairedDeviceLocation should be true
        ProviderTestUtils.assertKeyValue(props,
                SettingsContract.KEY_OBTAIN_PAIRED_DEVICE_LOCATION,
                SettingsContract.VALUE_TRUE);
    }

    @Test
    public void testQuery_noGps_locationDisabled() {
        // GIVEN flag KEY_OBTAIN_PAIRED_DEVICE_LOCATION is not defined
        // GIVEN device has no GPS
        Mockito.when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS))
                .thenReturn(false);
        // GIVEN location disabled
        mLocationEnabled = false;

        // WHEN LocationProperties is instantiated
        SettingProperties props = mPropertiesMap.initLocationProperties(
                mPrefs, () -> mPackageManager, mLocationEnabledSupplier);

        // THEN ObtainPairedDeviceLocation should be false
        ProviderTestUtils.assertKeyValue(props,
                SettingsContract.KEY_OBTAIN_PAIRED_DEVICE_LOCATION,
                SettingsContract.VALUE_FALSE);
        // THEN location should be changed to device only
        // Mockito.verify(mLocationEnabledSupplier).setLocationEnabledForUser(true, Mockito.any());
    }

    @Test
    public void testQuery_noGps_locationEnabled() {
        // GIVEN flag KEY_OBTAIN_PAIRED_DEVICE_LOCATION is not defined
        // GIVEN device has no GPS
        Mockito.when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS))
                .thenReturn(false);
        // GIVEN location mode is sensors only
        mLocationEnabled = true;

        // WHEN LocationProperties is instantiated
        SettingProperties props = mPropertiesMap.initLocationProperties(
                mPrefs, () -> mPackageManager, mLocationEnabledSupplier);

        // THEN ObtainPairedDeviceLocation should be true
        ProviderTestUtils.assertKeyValue(props,
                SettingsContract.KEY_OBTAIN_PAIRED_DEVICE_LOCATION,
                SettingsContract.VALUE_TRUE);
        // THEN location should not be changed
        // Mockito.verify(mLocationEnabledSupplier).isLocationEnabled();
        // Mockito.verifyNoMoreInteractions(mLocationEnabledSupplier);
    }

    @Test
    public void testQuery_obtainPairedDeviceLocationTrue() {
        // GIVEN flag KEY_OBTAIN_PAIRED_DEVICE_LOCATION is true
        mPrefs.edit()
                .putInt(SettingsContract.KEY_OBTAIN_PAIRED_DEVICE_LOCATION,
                        SettingsContract.VALUE_TRUE)
                .apply();
        mLocationEnabled = false;

        // WHEN LocationProperties is instantiated
        SettingProperties props = mPropertiesMap.initLocationProperties(
                mPrefs, () -> mPackageManager, mLocationEnabledSupplier);

        // THEN ObtainPairedDeviceLocation should be true
        ProviderTestUtils.assertKeyValue(props,
                SettingsContract.KEY_OBTAIN_PAIRED_DEVICE_LOCATION,
                SettingsContract.VALUE_TRUE);
    }

    @Test
    public void testQuery_obtainPairedDeviceLocationFalse() {
        // GIVEN flag KEY_OBTAIN_PAIRED_DEVICE_LOCATION is false
        mPrefs.edit()
                .putInt(SettingsContract.KEY_OBTAIN_PAIRED_DEVICE_LOCATION,
                        SettingsContract.VALUE_FALSE)
                .apply();
        mLocationEnabled = false;

        // WHEN LocationProperties is instantiated
        SettingProperties props = mPropertiesMap.initLocationProperties(
                mPrefs, () -> mPackageManager, mLocationEnabledSupplier);

        // THEN ObtainPairedDeviceLocation should be false
        ProviderTestUtils.assertKeyValue(props,
                SettingsContract.KEY_OBTAIN_PAIRED_DEVICE_LOCATION,
                SettingsContract.VALUE_FALSE);
    }
}
