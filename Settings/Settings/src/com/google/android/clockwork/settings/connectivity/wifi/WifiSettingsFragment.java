package com.google.android.clockwork.settings.connectivity.wifi;

import android.app.Activity;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.provider.Settings;
import com.android.settingslib.wifi.WifiTracker;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;
import java.util.List;

/**
 * Wifi Settings page.
 */
public class WifiSettingsFragment extends SettingsPreferenceFragment
        implements WifiTracker.WifiListener {
    /*package*/ static final String TAG = "WifiSettings";

    private WifiSettingsImpl mWifiSettings;

    /** Used for Available Networks */
    private WifiTracker mWifiTracker;

    private NetworkCallback mNetworkCallback;
    private WifiSettingsObserver mWifiSettingsObserver =
            new WifiSettingsObserver(new Handler(Looper.getMainLooper()));

    private final ConnectivityManager.NetworkCallback availableOrLostNetworkCallback
            = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            initAddSavedAndCurrentNetworks();
        }

        @Override
        public void onCapabilitiesChanged(Network network,
                NetworkCapabilities networkCapabilities) {}

        @Override
        public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {}

        @Override
        public void onLosing(Network network, int maxMsToLive) {}

        @Override
        public void onLost(Network network) {
            initAddSavedAndCurrentNetworks();
        }

        @Override
        public void onUnavailable() {}
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_wifi);

        mWifiTracker = new WifiTracker(
                getActivity(), this, true, true);

        mWifiSettings = new WifiSettingsImpl(
            getContext(),
            PreferenceManager.getDefaultSharedPreferences(getActivity()),
            getContext().getSystemService(ConnectivityManager.class),
            getContext().getSystemService(WifiManager.class),
            UserManager.get(getContext()),
            getPreferenceScreen(),
            mWifiConfigsListener,
            mForgetNetworkListener);
        mWifiSettings.initWifiToggle(mEnableSwitchPrefListener);
        mWifiSettings.initAboutWifiAutomatic();
        initAddSavedAndCurrentNetworks();
    }

    @Override
    public void onStart() {
        super.onStart();
        getContext().getContentResolver().registerContentObserver(
                Settings.System.getUriFor(WifiSettingsImpl.WIFI_SETTING_KEY),
                false,
                mWifiSettingsObserver);
        getContext().getSystemService(ConnectivityManager.class)
                .registerDefaultNetworkCallback(availableOrLostNetworkCallback);
    }

    @Override
    public void onResume() {
        super.onResume();
        WifiSettingsUtil.setInWifiSettings(getContext(), true);
        if (WifiSettingsUtil.getWearWifiEnabled(getContext())) {
            mNetworkCallback =
                    WifiSettingsUtil.setWifiHoldUp(getContext(), mNetworkCallback, true);
        }
        mWifiTracker.onStart();
    }

    @Override
    public void onPause() {
        super.onPause();
        mWifiTracker.onStop();
        mNetworkCallback =
                WifiSettingsUtil.setWifiHoldUp(getContext(), mNetworkCallback, false);
        WifiSettingsUtil.setInWifiSettings(getContext(), false);
    }

    @Override
    public void onStop() {
        getContext().getSystemService(ConnectivityManager.class)
                .unregisterNetworkCallback(availableOrLostNetworkCallback);
        getContext().getContentResolver().unregisterContentObserver(mWifiSettingsObserver);
        super.onStop();
    }

    @Override
    public void onDestroy() {
        mWifiTracker.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onAccessPointsChanged() {
    }

    @Override
    public void onWifiStateChanged(int state) {
    }

    @Override
    public void onConnectedChanged() {
        initAddSavedAndCurrentNetworks();
    }

    private final class WifiSettingsObserver extends ContentObserver {
        public WifiSettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (getContext() != null) {
                mWifiSettings.maybeUpdateWifiToggle();
            }
            initAddSavedAndCurrentNetworks();
        }
    }

    /**
     * Updates the "Add network" screen, and triggers an update to the "Saved networks" screen and
     * the "Current network" preference.
     */
    private void initAddSavedAndCurrentNetworks() {
        if (getContext() == null) {
            return;
        }
        mWifiSettings.initAddNetwork();
        // WifiSettingsImpl performs this work on a background thread and will notify
        // WifiConfigsListener once this call has returned so saved and current networks
        // can be updated on the UI thread.
        mWifiSettings.retrieveWifiConfigsForSavedAndCurrentNetworks();
    }

    /**
     * Updates the "Saved networks" screen and the "Current network" preference on the UI thread.
     * Must be called after WifiConfigurations have been retrieved on a background thread.
     */
    private void initSavedAndCurrentNetworksOnUiThread(List<WifiConfiguration> configs) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        activity.runOnUiThread(() -> {
            mWifiSettings.initSavedAndCurrentNetworks(configs);
        });
    }

    private WifiSettingsImpl.WifiConfigsListener mWifiConfigsListener =
        new WifiSettingsImpl.WifiConfigsListener() {
            @Override
            public void onWifiConfigsAvailable(List<WifiConfiguration> configs) {
                initSavedAndCurrentNetworksOnUiThread(configs);
            }
        };

    private SavedNetworkPreference.ForgetNetworkListener mForgetNetworkListener =
            new SavedNetworkPreference.ForgetNetworkListener() {
                @Override
                public void onNetworkForgotten() {
                    mWifiSettings.retrieveWifiConfigsForSavedAndCurrentNetworks();
                }
            };

    private OnPreferenceChangeListener mEnableSwitchPrefListener =
            new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            Boolean desiredSetting = (Boolean) newValue;
            mNetworkCallback =
                WifiSettingsUtil.setWifiHoldUp(getContext(), mNetworkCallback, desiredSetting);
            WifiSettingsUtil.setWearWifiEnabled(getContext(), desiredSetting);
            return true;
        }
    };
}
