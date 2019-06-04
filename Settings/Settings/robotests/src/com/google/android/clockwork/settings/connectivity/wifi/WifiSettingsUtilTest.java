package com.google.android.clockwork.settings.connectivity.wifi;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.wifi.WifiConfiguration;
import android.provider.Settings;
import com.google.android.clockwork.settings.common.PackageManagerProxy;
import com.google.android.clockwork.settings.enterprise.DevicePolicyManagerWrapper;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(ClockworkRobolectricTestRunner.class)
public class WifiSettingsUtilTest {

  private static final int USER_ID = 1;

  @Mock ComponentName mComponentName;
  @Mock DevicePolicyManagerWrapper mDevicePolicyManager;
  @Mock PackageManagerProxy mPackageManager;
  ContentResolver mContentResolver;
  WifiConfiguration mWifiConfig;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    mWifiConfig = new WifiConfiguration();
    mWifiConfig.creatorUid = USER_ID;
    mContentResolver = RuntimeEnvironment.application.getContentResolver();
  }

  @Test
  public void testCanModifyNetworkUnlockedConfig() throws NameNotFoundException {
    Settings.Global.putInt(mContentResolver, Settings.Global.WIFI_DEVICE_OWNER_CONFIGS_LOCKDOWN, 0);

    when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser()).thenReturn(mComponentName);
    when(mDevicePolicyManager.getDeviceOwnerUserId()).thenReturn(USER_ID);
    when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_DEVICE_ADMIN)).thenReturn(true);
    when(mPackageManager.getPackageUidAsUser(isNull(), anyInt())).thenReturn(USER_ID);

    assertTrue(WifiSettingsUtil.canModifyNetwork(
        mDevicePolicyManager, mPackageManager, mContentResolver, mWifiConfig));
  }

  @Test
  public void testCannotModifyNetworkLockedConfig() throws NameNotFoundException {
    Settings.Global.putInt(mContentResolver, Settings.Global.WIFI_DEVICE_OWNER_CONFIGS_LOCKDOWN, 1);

    when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser()).thenReturn(mComponentName);
    when(mDevicePolicyManager.getDeviceOwnerUserId()).thenReturn(USER_ID);
    when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_DEVICE_ADMIN)).thenReturn(true);
    when(mPackageManager.getPackageUidAsUser(isNull(), anyInt())).thenReturn(USER_ID);

    assertFalse(WifiSettingsUtil.canModifyNetwork(
        mDevicePolicyManager, mPackageManager, mContentResolver, mWifiConfig));
  }
}
