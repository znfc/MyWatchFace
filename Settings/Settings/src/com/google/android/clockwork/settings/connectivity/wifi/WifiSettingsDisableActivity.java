package com.google.android.clockwork.settings.connectivity.wifi;

import android.app.Activity;
import android.net.wifi.WifiManager;
import android.os.Bundle;

public class WifiSettingsDisableActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSystemService(WifiManager.class).setWifiEnabled(false);
        setResult(RESULT_OK);
        finish();
    }
}
