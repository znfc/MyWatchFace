package com.google.android.clockwork.settings.connectivity.wifi;

import static android.os.UserManager.DISALLOW_CONFIG_WIFI;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.RuntimeEnvironment.application;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.wearable.preference.WearableDialogPreference;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

/**
 * Test for {@link WifiSettingsImpl}
 **/
@RunWith(ClockworkRobolectricTestRunner.class)
public class WifiSettingsImplTest {

  private final Context mContext = application.getApplicationContext();
  private final SharedPreferences mSharedPreferences =
      PreferenceManager.getDefaultSharedPreferences(mContext);
  private WifiSettingsImpl mWifiSettingsImpl;

  @Mock ConnectivityManager mMockConnectivityManager;
  @Mock WifiManager mMockWifiManager;
  @Mock UserManager mMockUserManager;
  @Mock PreferenceScreen mMockPrefScreen;
  @Mock WifiSettingsImpl.WifiConfigsListener mMockWifiConfigsListener;
  @Mock SavedNetworkPreference.ForgetNetworkListener mMockForgetNetworkListener;

  @Mock PreferenceScreen mMockCurrentNetworkScreen;
  @Mock PreferenceScreen mMockAddNetworkScreen;
  @Mock PreferenceScreen mMockSavedNetworksScreen;
  @Mock Preference mMockIpPref;
  @Mock Preference mMockMacPref;

  @Before
  public void setup() {
    initMocks(this);
    mWifiSettingsImpl = new WifiSettingsImpl(
        mContext,
        mSharedPreferences,
        mMockConnectivityManager,
        mMockWifiManager,
        mMockUserManager,
        mMockPrefScreen,
        mMockWifiConfigsListener,
        mMockForgetNetworkListener);
  }

  @Test
  public void testInitAboutWifiAutomatic() {
    WearableDialogPreference mockWearableDialogPreference = mock(WearableDialogPreference.class);
    when(mMockPrefScreen.findPreference(WifiSettingsImpl.KEY_PREF_WIFI_ABOUT))
        .thenReturn(mockWearableDialogPreference);

    mWifiSettingsImpl.initAboutWifiAutomatic();

    verify(mockWearableDialogPreference).setDialogMessage(any());
  }

  // ENABLED-DISABLED TOGGLE

  @Test
  public void testInitWifiToggle() {
    SwitchPreference mockSwitchPreference = mock(SwitchPreference.class);
    Preference.OnPreferenceChangeListener mockPreferenceChangeListener =
        mock(Preference.OnPreferenceChangeListener.class);
    WifiSettingsUtil.setWearWifiEnabled(mContext, true);
    when(mMockPrefScreen.findPreference(WifiSettingsImpl.KEY_PREF_WIFI_TOGGLE))
        .thenReturn(mockSwitchPreference);

    mWifiSettingsImpl.initWifiToggle(mockPreferenceChangeListener);

    verify(mockSwitchPreference).setTitle(R.string.wifi_activity_title);
    verify(mockSwitchPreference).setSummaryOn(R.string.wifi_connection_action_automatic);
    verify(mockSwitchPreference).setSummaryOff(R.string.wifi_connection_action_off);
    verify(mockSwitchPreference).setChecked(true);
    verify(mockSwitchPreference).setOnPreferenceChangeListener(mockPreferenceChangeListener);
  }

  // CURRENT NETWORK

  @Test
  public void testInitCurrentNetworkWifiDisabled() {
    WifiSettingsUtil.setWearWifiEnabled(mContext, false);
    when(mMockPrefScreen.findPreference(WifiSettingsImpl.KEY_PREF_WIFI_CURRENT_NETWORK))
        .thenReturn(mMockCurrentNetworkScreen);

    mWifiSettingsImpl.initCurrentNetwork(null);

    verify(mMockPrefScreen).removePreference(mMockCurrentNetworkScreen);
  }

  @Test
  public void testInitCurrentNetworkNoCurrentNetwork() {
    WifiSettingsUtil.setWearWifiEnabled(mContext, true);
    when(mMockPrefScreen.findPreference(WifiSettingsImpl.KEY_PREF_WIFI_CURRENT_NETWORK))
        .thenReturn(mMockCurrentNetworkScreen);

    mWifiSettingsImpl.initCurrentNetwork(null);

    verify(mMockPrefScreen).removePreference(mMockCurrentNetworkScreen);
  }

  @Test
  public void testPopulateCurrentNetwork() {
    WifiSettingsUtil.setWearWifiEnabled(mContext, true);
    when(mMockPrefScreen.findPreference(WifiSettingsImpl.KEY_PREF_WIFI_IP_ADDRESS))
        .thenReturn(mMockIpPref);
    when(mMockPrefScreen.findPreference(WifiSettingsImpl.KEY_PREF_WIFI_MAC_ADDRESS))
        .thenReturn(mMockMacPref);

    mWifiSettingsImpl.populateCurrentNetwork(mMockCurrentNetworkScreen, true, true, "ssid", true,
            null);

    verify(mMockCurrentNetworkScreen).setTitle("ssid");
    verify(mMockCurrentNetworkScreen).setSummary(R.string.wifi_current_connection_connected);
    verify(mMockCurrentNetworkScreen).setIcon(R.drawable.ic_cc_settings_wifi_secure_4_nobg);
    verify(mMockCurrentNetworkScreen).setOrder(1);
    verify(mMockCurrentNetworkScreen).setSelectable(true);
  }

  // ADD NETWORK

  @Test
  public void testInitAddNetworkUserRestricted() {
    WifiSettingsUtil.setWearWifiEnabled(mContext, true);
    when(mMockPrefScreen.findPreference(WifiSettingsImpl.KEY_PREF_WIFI_ADD_NETWORK))
        .thenReturn(mMockAddNetworkScreen);
    when(mMockUserManager.hasUserRestriction(DISALLOW_CONFIG_WIFI)).thenReturn(true);

    mWifiSettingsImpl.initAddNetwork();

    verify(mMockPrefScreen).removePreference(mMockAddNetworkScreen);
  }

  // SAVED NETWORKS

  @Test
  public void testInitSavedAndCurrentNetworksWifiDisabled() {
    WifiSettingsUtil.setWearWifiEnabled(mContext, false);
    when(mMockPrefScreen.findPreference(WifiSettingsImpl.KEY_PREF_WIFI_SAVED_NETWORKS))
        .thenReturn(mMockSavedNetworksScreen);
    when(mMockPrefScreen.findPreference(WifiSettingsImpl.KEY_PREF_WIFI_CURRENT_NETWORK))
        .thenReturn(mMockCurrentNetworkScreen);

    mWifiSettingsImpl.initSavedAndCurrentNetworks(null);

    verify(mMockPrefScreen).removePreference(mMockSavedNetworksScreen);
    verify(mMockPrefScreen).removePreference(mMockCurrentNetworkScreen);
  }
}
