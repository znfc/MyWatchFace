package com.google.android.clockwork.settings.connectivity.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.SettingsIntents;
import java.util.List;

/**
 * Show a spinner animation while the user enters the WiFi password on their phone.
 *
 * The activity finishes once the user connects to the network.
 */
public class WifiWaitForPhoneActivity extends WearableActivity {

    private static final String TAG = "WifiWaitingForPhone";

    private static final boolean DEBUG = false;

    static final String EXTRA_SSID_KEY = "ssid";

    // The SSID of the network the user wants to connect to.
    private String mSsid;

    private NetworkCallback mNetworkCallback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setAmbientEnabled();
        setContentView(R.layout.wifi_waiting_for_phone);

        mSsid = getIntent().getStringExtra(EXTRA_SSID_KEY);
        debugLog("Waiting to connect to ssid: " + mSsid);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(SettingsIntents.ACTION_REQUEST_WIFI_PASSWORD_DONE);
        registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public void onResume() {
        super.onResume();
        WifiSettingsUtil.setInWifiSettings(getApplicationContext(), true);
        mNetworkCallback =
                WifiSettingsUtil.setWifiHoldUp(getApplicationContext(), mNetworkCallback, true);
    }

    @Override
    public void onPause() {
        super.onPause();
        mNetworkCallback =
                WifiSettingsUtil.setWifiHoldUp(getApplicationContext(), mNetworkCallback, false);
        WifiSettingsUtil.setInWifiSettings(getApplicationContext(), false);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    private final BroadcastReceiver mBroadcastReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    debugLog("onReceive: " + action);
                    if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                        NetworkInfo networkInfo = intent
                                .getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                        handleNetworkStateChange(networkInfo);
                    } else if (action.equals(SettingsIntents.ACTION_REQUEST_WIFI_PASSWORD_DONE)) {
                        finish();
                    }
                }
            };

    private void handleNetworkStateChange(NetworkInfo networkInfo) {
        if (networkInfo == null) {
            debugLog("networkInfo is null");
            finish();
            return;
        }

        debugLog("Got network state change: " + networkInfo.getDetailedState());
        if (networkInfo.getDetailedState() == DetailedState.AUTHENTICATING
            && inSavedNetwork(mSsid)) {
            debugLog("SSID is now saved: " + mSsid);
            finish();
        }
    }

    private boolean inSavedNetwork(String ssid) {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        List<WifiConfiguration> wifiConfigurations = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration wifiConfiguration : wifiConfigurations) {
            String savedSsid = getSsidFromString(wifiConfiguration.SSID);
            if (savedSsid.equals(ssid)) {
                return true;
            }
        }
        return false;
    }

    private static String getSsidFromString(String ssid) {
        if (ssid == null) {
            return "";
        } else if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
            return ssid.substring(1, ssid.length() - 1);
        }
        return ssid;
    }

    private void debugLog(String message) {
        if (DEBUG || Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, message);
        }
    }
}
