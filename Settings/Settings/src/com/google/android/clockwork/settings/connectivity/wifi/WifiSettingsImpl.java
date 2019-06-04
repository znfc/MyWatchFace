package com.google.android.clockwork.settings.connectivity.wifi;

import static android.os.UserManager.DISALLOW_CONFIG_WIFI;

import android.annotation.Nullable;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkUtils;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.wearable.preference.WearableDialogPreference;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.AlignmentSpan;
import android.widget.BaseAdapter;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.common.concurrent.AbstractCwRunnable;
import com.google.android.clockwork.common.concurrent.Executors;
import com.google.common.annotations.VisibleForTesting;
import java.util.List;

/**
 * This testable class implements the logic that backs WifiSettingsFragment.
 */
public class WifiSettingsImpl {

    /**
     * valid values for this key: "on", "off", "off_airplane"
     */
    static final String WIFI_SETTING_KEY = "clockwork_wifi_setting";
    static final String ON = "on";
    static final String OFF = "off";
    static final String OFF_AIRPLANE = "off_airplane";
    static final String IN_WIFI_SETTINGS_KEY = "clockwork_in_wifi_settings";

    static final String KEY_PREF_WIFI_TOGGLE = "pref_wifi_toggle";
    static final String KEY_PREF_WIFI_ABOUT = "pref_wifi_about";
    static final String KEY_PREF_WIFI_CURRENT_NETWORK = "pref_wifi_current_network";
    static final String KEY_PREF_WIFI_ADD_NETWORK = "pref_wifi_add_network";
    static final String KEY_PREF_WIFI_SAVED_NETWORKS = "pref_wifi_saved_networks";
    static final String KEY_PREF_WIFI_IP_ADDRESS = "pref_wifi_view_ip_address";
    static final String KEY_PREF_WIFI_MAC_ADDRESS = "pref_wifi_view_mac_address";

    private static final String KEY_LAST_USED_WIFI_SSID = "last_used_wifi_ssid";
    private static final String KEY_LAST_USED_WIFI_SECURE = "last_used_wifi_secure";

    private final Context mContext;
    private final ConnectivityManager mConnectivityManager;
    private final WifiManager mWifiManager;
    private final UserManager mUserManager;
    private final SharedPreferences mSharedPreferences;
    private final SavedNetworkPreference.ForgetNetworkListener mForgetNetworkListener;
    private WifiConfigsListener mWifiConfigsListener;
    private List<WifiConfiguration> mWifiConfigs;

    private final PreferenceScreen mPrefScreen;
    private final PreferenceScreen mAddNetworkScreen;
    private final PreferenceScreen mSavedNetworksScreen;
    private final PreferenceScreen mCurrentNetworkScreen;

    WifiSettingsImpl(
            Context context,
            SharedPreferences sharedPreferences,
            ConnectivityManager connectivityManager,
            WifiManager wifiManager,
            UserManager userManager,
            PreferenceScreen prefScreen,
            WifiConfigsListener wifiConfigsListener,
            SavedNetworkPreference.ForgetNetworkListener forgetNetworkListener) {
        mContext = context;
        mSharedPreferences = sharedPreferences;
        mConnectivityManager = connectivityManager;
        mWifiManager = wifiManager;
        mUserManager = userManager;
        mPrefScreen = prefScreen;
        mAddNetworkScreen = (PreferenceScreen) prefScreen.findPreference(KEY_PREF_WIFI_ADD_NETWORK);
        mSavedNetworksScreen =
                (PreferenceScreen) prefScreen.findPreference(KEY_PREF_WIFI_SAVED_NETWORKS);
        mCurrentNetworkScreen =
                (PreferenceScreen) prefScreen.findPreference(KEY_PREF_WIFI_CURRENT_NETWORK);
        mWifiConfigsListener = wifiConfigsListener;
        mForgetNetworkListener = forgetNetworkListener;
    }

    /**
     * A listener for notifying WifiSettingsFragment when a call to
     * WifiManager#getConfiguredNetworks returns. WifiManager#getConfiguredNetworks must be run on
     * a background thread because it takes some time to retrieve the configs.
     */
    public interface WifiConfigsListener {

        public void onWifiConfigsAvailable(List<WifiConfiguration> wifiConfigs);
    }

    /**
     * The default layout for dialogs on Wear is to have centered text.
     * We want the About WiFi Automatic page to be aligned normally.
     */
    void initAboutWifiAutomatic() {
        WearableDialogPreference dialogPref =
                (WearableDialogPreference) mPrefScreen.findPreference(KEY_PREF_WIFI_ABOUT);
        String aboutText = mContext.getString(R.string.wifi_about_automatic_text);
        SpannableStringBuilder ss = new SpannableStringBuilder(aboutText);
        ss.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_NORMAL),
                0, aboutText.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        dialogPref.setDialogMessage(ss);
    }

    // ENABLED-DISABLED TOGGLE

    void initWifiToggle(Preference.OnPreferenceChangeListener listener) {
        SwitchPreference pref = (SwitchPreference) mPrefScreen.findPreference(KEY_PREF_WIFI_TOGGLE);
        final boolean enabled = WifiSettingsUtil.getWearWifiEnabled(mContext);

        pref.setTitle(R.string.wifi_activity_title);
        pref.setSummaryOn(R.string.wifi_connection_action_automatic);
        pref.setSummaryOff(R.string.wifi_connection_action_off);
        pref.setChecked(enabled);
        pref.setOnPreferenceChangeListener(listener);
    }

    void maybeUpdateWifiToggle() {
        final SwitchPreference pref
            = (SwitchPreference) mPrefScreen.findPreference(KEY_PREF_WIFI_TOGGLE);
        final boolean enabled = WifiSettingsUtil.getWearWifiEnabled(mContext);
        if (pref.isChecked() != enabled) {
            pref.setChecked(enabled);
            ((BaseAdapter) mPrefScreen.getRootAdapter()).notifyDataSetChanged();
        }
    }

    // CURRENT NETWORK
    @Nullable
    private final WifiInfo getCurrentConnectedWifiInfo() {
        WifiInfo wifiInfo = null;
        NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            wifiInfo = mWifiManager.getConnectionInfo();
        }
        return wifiInfo;
    }

    /**
     * Initialize the current wifi network {@link PreferenceScreen} field.
     *
     * The preference is implemented as a {@link PreferenceScreen} in order to allow information
     * about the current network, if any, to be presented.
     *
     * @param config The {@link WifiConfiguration} currently active or NULL if no current active
     * network.
     */
    @VisibleForTesting void initCurrentNetwork(@Nullable final WifiConfiguration config) {
        PreferenceScreen pref =
                (PreferenceScreen) mPrefScreen.findPreference(KEY_PREF_WIFI_CURRENT_NETWORK);
        if (!WifiSettingsUtil.getWearWifiEnabled(mContext)) {
            removeCurrentNetworkPref(pref);
            return;
        }

        final WifiInfo wifiInfo = getCurrentConnectedWifiInfo();

        boolean addPrefToScreen = false;

        if (pref == null) {
            // findPreference returns null once a preference has been removed from the screen
            // Retrieve the cached "Current Network" preference
            pref = mCurrentNetworkScreen;
            addPrefToScreen = true;
        }

        if (config == null || wifiInfo == null || wifiInfo.getNetworkId() != config.networkId
                || (config.selfAdded && config.numAssociation == 0)) {
            // There is no current network or the current WifiInfo object doesn't match the
            // WifiConfiguration with the current network. Show the last used AP.
            String lastUsedSsid = mSharedPreferences.getString(KEY_LAST_USED_WIFI_SSID, null);
            boolean lastUsedSecure = mSharedPreferences.getBoolean(KEY_LAST_USED_WIFI_SECURE, true);
            populateCurrentNetwork(
                    pref, addPrefToScreen, false, lastUsedSsid, lastUsedSecure, wifiInfo);
        } else {
            // There is a current network. Show the currently connected AP.
            // See AccessPoint#getSecurity
            final boolean secure = (config.allowedKeyManagement.get(KeyMgmt.WPA_PSK)
                    || config.allowedKeyManagement.get(KeyMgmt.WPA_EAP)
                    || config.allowedKeyManagement.get(KeyMgmt.IEEE8021X)
                    || (config.wepKeys[0] != null));
            populateCurrentNetwork(pref, addPrefToScreen, true, config.getPrintableSsid(), secure,
                    wifiInfo);

            // Save the current network so it can be used as the "Last Used" network
            mSharedPreferences.edit()
                    .putString(KEY_LAST_USED_WIFI_SSID, config.getPrintableSsid())
                    .putBoolean(KEY_LAST_USED_WIFI_SECURE, secure)
                    .commit();
        }
    }

    private void removeCurrentNetworkPref(PreferenceScreen pref) {
        if (pref != null) {
            final Preference ipPref = pref.findPreference(KEY_PREF_WIFI_IP_ADDRESS);
            if (ipPref != null) {
                pref.removePreference(ipPref);
            }

            final Preference macPref = pref.findPreference(KEY_PREF_WIFI_MAC_ADDRESS);
            if (macPref != null) {
                pref.removePreference(macPref);
            }

            mPrefScreen.removePreference(pref);
        }
    }

    void populateCurrentNetwork(
            PreferenceScreen pref,
            boolean addToScreen,
            boolean current,
            String ssid,
            boolean secure,
            @Nullable final WifiInfo wifiInfo) {
        if (ssid == null) {
            if (!addToScreen) {
                // There's no network to show, so don't display the preference
                removeCurrentNetworkPref(pref);
            }
            return;
        }

        pref.setTitle(ssid);
        pref.setSummary(current
                ? R.string.wifi_current_connection_connected
                : R.string.wifi_connection_last_used);
        pref.setIcon(secure
                ? R.drawable.ic_cc_settings_wifi_secure_4_nobg
                : R.drawable.ic_cc_settings_wifi_4_nobg);
        // Goes below the "Connection" preference
        pref.setOrder(1);
        pref.setSelectable(current);

        populateIpAndMacAddress(pref, wifiInfo);

        if (addToScreen) {
            mPrefScreen.addPreference(pref);
        }
    }

    private void populateIpAndMacAddress(PreferenceScreen currentNetworkScreen,
            @Nullable final WifiInfo wifiInfo) {
        if (WifiSettingsUtil.getWearWifiEnabled(mContext)) {

            Preference ipPref = mPrefScreen.findPreference(KEY_PREF_WIFI_IP_ADDRESS);
            if (ipPref == null) {
                ipPref = new Preference(mContext);
                ipPref.setKey(KEY_PREF_WIFI_IP_ADDRESS);
                ipPref.setTitle(R.string.wifi_advanced_ip_address_title);
                currentNetworkScreen.addPreference(ipPref);
            }

            ipPref.setSummary(wifiInfo == null
                    ?  null
                    : NetworkUtils.intToInetAddress(wifiInfo.getIpAddress()).getHostAddress());

            Preference macPref = mPrefScreen.findPreference(KEY_PREF_WIFI_MAC_ADDRESS);
            if (macPref == null) {
                macPref = new Preference(mContext);
                macPref.setKey(KEY_PREF_WIFI_MAC_ADDRESS);
                macPref.setTitle(R.string.wifi_advanced_mac_address_title);
                currentNetworkScreen.addPreference(macPref);
            }
            macPref.setSummary(wifiInfo == null
                    ?  mContext.getString(R.string.wifi_status_unavailable)
                    : wifiInfo.getMacAddress());
        }
    }

    // ADD NETWORK

    void initAddNetwork() {
        PreferenceScreen prefScreen =
                (PreferenceScreen) mPrefScreen.findPreference(KEY_PREF_WIFI_ADD_NETWORK);
        if (!WifiSettingsUtil.getWearWifiEnabled(mContext)
                || mUserManager.hasUserRestriction(DISALLOW_CONFIG_WIFI)) {
            removeCurrentNetworkPref(prefScreen);
            return;
        }

        if (prefScreen == null) {
            // Goes below "Current network"
            mAddNetworkScreen.setOrder(2);
            mPrefScreen.addPreference(mAddNetworkScreen);
        }
    }

    // SAVED NETWORKS

    /**
     * Retrieves configured networks from WifiManager using a user executor, and populates the
     * "Current Network" and "Saved Networks" preferences.
     */
    void initSavedAndCurrentNetworks(List<WifiConfiguration> configs) {
        PreferenceScreen savedNetworksScreen =
                (PreferenceScreen) mPrefScreen.findPreference(KEY_PREF_WIFI_SAVED_NETWORKS);
        if (!WifiSettingsUtil.getWearWifiEnabled(mContext)) {
            removeCurrentNetworkPref(savedNetworksScreen);
            initCurrentNetwork(null);
            return;
        }

        if (savedNetworksScreen == null) {
            savedNetworksScreen = mSavedNetworksScreen;
            // Goes below "Add network"
            savedNetworksScreen.setOrder(3);
            mPrefScreen.addPreference(savedNetworksScreen);
        }

        mSavedNetworksScreen.removeAll();

        final WifiInfo wifiInfo = getCurrentConnectedWifiInfo();

        WifiConfiguration currentConfig = null;
        if (configs != null && configs.size() > 0) {
            mSavedNetworksScreen.setSummary("");
            for (WifiConfiguration config : configs) {
                if (wifiInfo != null
                        && wifiInfo.getNetworkId() != WifiConfiguration.INVALID_NETWORK_ID
                        && wifiInfo.getNetworkId() == config.networkId) {
                    currentConfig = config;
                }

                final SavedNetworkPreference pref =
                        new SavedNetworkPreference(mContext, config, mForgetNetworkListener);
                mSavedNetworksScreen.addPreference(pref);
            }
        } else {
            mSavedNetworksScreen.setSummary(R.string.wifi_empty_saved_networks);
            // Add an empty preference to the screen so that when the user clicks on
            // "Saved Networks", the "Saved Networks" screen actually shows.
            Preference emptyPref = new Preference(mContext);
            emptyPref.setSelectable(false);
            emptyPref.setTitle(R.string.wifi_empty_saved_networks);
            mSavedNetworksScreen.addPreference(emptyPref);
        }
        initCurrentNetwork(currentConfig);
        ((BaseAdapter) mPrefScreen.getRootAdapter()).notifyDataSetChanged();
    }

    /**
     * Retrieves the configured APs on a background thread, and notifies mWifiConfigsListener once
     * the call to WifiManager#getConfiguredNetworks has returned.
     */
    void retrieveWifiConfigsForSavedAndCurrentNetworks() {
        // Retreiving the configured APs can take some time, so use a user executor to perform the
        // work on a background thread.
        Executors.INSTANCE.get(mContext).getUserExecutor().submit(
                new AbstractCwRunnable("InitSavedAndCurrentNetworks") {
                    @Override
                    public void run() {
                        final List<WifiConfiguration> configs = mWifiManager
                                .getConfiguredNetworks();
                        onWifiConfigsAvailable(configs);
                    }
                });
    }

    private void onWifiConfigsAvailable(List<WifiConfiguration> configs) {
        if (mWifiConfigs == null || (mWifiConfigs != null && !mWifiConfigs.equals(configs))) {
            mWifiConfigs = configs;
            mWifiConfigsListener.onWifiConfigsAvailable(mWifiConfigs);
        }
    }
}
