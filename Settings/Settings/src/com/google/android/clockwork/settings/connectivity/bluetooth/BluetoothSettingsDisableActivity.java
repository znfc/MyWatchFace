package com.google.android.clockwork.settings.connectivity.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;

public class BluetoothSettingsDisableActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BluetoothAdapter.getDefaultAdapter().disable();
        setResult(RESULT_OK);
        finish();
    }
}
