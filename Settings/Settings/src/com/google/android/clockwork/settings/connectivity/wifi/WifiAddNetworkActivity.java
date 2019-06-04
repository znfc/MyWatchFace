package com.google.android.clockwork.settings.connectivity.wifi;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.wearable.preference.WearablePreferenceActivity;
import android.util.Log;

/**
 * A Wi-Fi settings activity that sends the user directly into the "Add network" Wi-Fi settings
 * screen, and finishes as soon as the device has successfully connected to any Wi-Fi AP. Note that
 * this activity will enable Wi-Fi if it is disabled.
 *
 * Intended to be used by 1st and 3rd party apps that want to use Wi-Fi for something (e.g.
 * app install, etc.) and need the user to select a Wi-Fi AP to connect to. Apps that start this
 * activity will need to hold the Wi-Fi permission android.permission.CHANGE_WIFI_STATE.
 */
public class WifiAddNetworkActivity extends WearablePreferenceActivity {
    private static final String TAG = "WifiAddNetwork";

    public static String ACTION_WIFI_ADD_NETWORK_SETTINGS =
            "com.google.android.clockwork.settings.connectivity.wifi.ADD_NETWORK_SETTINGS";

    private NetworkCallback mNetworkCallback;

    private BroadcastReceiver mNetworkStateChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (networkInfo.getState() == NetworkInfo.State.CONNECTED) {
                WifiAddNetworkActivity.this.setResult(Activity.RESULT_OK);
                finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

         // Force Wi-Fi enabled. Wi-Fi will remain enabled after this activity finishes.
        if (!WifiSettingsUtil.getWearWifiEnabled(getApplicationContext())) {
            WifiSettingsUtil.setWearWifiEnabled(getApplicationContext(), true);
        }

        if (savedInstanceState == null) {
            startPreferenceFragment();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        WifiSettingsUtil.setInWifiSettings(getApplicationContext(), true);
        mNetworkCallback =
                WifiSettingsUtil.setWifiHoldUp(getApplicationContext(), mNetworkCallback, true);
        registerReceiver(mNetworkStateChangeReceiver,
                new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mNetworkStateChangeReceiver);
        mNetworkCallback =
                WifiSettingsUtil.setWifiHoldUp(getApplicationContext(), mNetworkCallback, false);
        WifiSettingsUtil.setInWifiSettings(getApplicationContext(), false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        startPreferenceFragment();
    }

    private void startPreferenceFragment() {
        Intent intent = getIntent();
        String action = intent.getAction();
        Fragment f = null;

        if (ACTION_WIFI_ADD_NETWORK_SETTINGS.equals(action)) {
            f = new WifiAddNetworkFragment();
        }

        if (f != null) {
            startPreferenceFragment(f, false);
        }
    }
}