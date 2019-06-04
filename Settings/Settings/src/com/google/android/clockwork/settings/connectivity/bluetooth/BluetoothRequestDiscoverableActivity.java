package com.google.android.clockwork.settings.connectivity.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.AcceptDenyDialog;
import android.util.Log;
import android.view.View;

import com.google.android.apps.wearable.settings.R;

/**
 * Asks the user for permission to make the device discoverable.
 */
public class BluetoothRequestDiscoverableActivity extends WearableActivity {
    public static final int ENABLE_BLUETOOTH_REQUEST_CODE = 1;
    private static final String TAG = BluetoothRequestDiscoverableActivity.class.getSimpleName();
    private BluetoothAdapter mBluetoothAdapter;
    private AcceptDenyDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Failed to get the Bluetooth adapter.");
            finish();
            return;
        }

        mDialog = new AcceptDenyDialog(this);
        mDialog.setTitle(getString(R.string.bluetooth_make_discoverable));
        mDialog.setPositiveButton((dialog, which) -> {
            boolean succeeded;
            int duration = getIntent().getIntExtra(
                        BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, BluetoothAdapter.ERROR);
            if (duration != BluetoothAdapter.ERROR) {
                succeeded = mBluetoothAdapter.setScanMode(
                        BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, duration);
            } else {
                succeeded = mBluetoothAdapter.setScanMode(
                        BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE);
            }
            if (!succeeded) {
                Log.e(TAG, "Failed to set scan mode to discoverable.");
            }
        });
        mDialog.setNegativeButton((dialog, which) -> { /* do nothing */ });
        mDialog.setOnDismissListener((dialog) -> finish());

        int state = mBluetoothAdapter.getState();
        switch (state) {
            case BluetoothAdapter.STATE_OFF:
            case BluetoothAdapter.STATE_TURNING_ON:
            case BluetoothAdapter.STATE_TURNING_OFF:
                // Try to enable Bluetooth first
                startActivityForResult(new Intent(this, BluetoothSettingsEnableActivity.class),
                        ENABLE_BLUETOOTH_REQUEST_CODE);
                break;
            default:
                mDialog.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ENABLE_BLUETOOTH_REQUEST_CODE && resultCode == RESULT_CANCELED) {
            // User does not want to enable the Bluetooth radio, leave this activity too
            finish();
        } else {
            mDialog.show();
        }
    }
}
