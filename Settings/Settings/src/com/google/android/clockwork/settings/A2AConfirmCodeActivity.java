package com.google.android.clockwork.settings;

import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.connectivity.bluetooth.BluetoothPairingDialog;
import com.google.android.clockwork.settings.utils.A2AHelper;

public class A2AConfirmCodeActivity extends WearableActivity {
    private static final String TAG = "A2AConfirm";

    private boolean mCancelOnExit = true;
    private BroadcastReceiver mResultReceiver;

    private Runnable mShowDialogRunnable;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BluetoothManager bluetooth = getSystemService(BluetoothManager.class);
        BluetoothAdapter adapter = bluetooth.getAdapter();
        BluetoothDevice device = null;
        for (BluetoothDevice bonded : adapter.getBondedDevices()) {
            // TODO: This is a terrible way to determine the iOS device.
            final int deviceType = bonded.getType();
            if (deviceType == BluetoothDevice.DEVICE_TYPE_LE
                    || deviceType == BluetoothDevice.DEVICE_TYPE_DUAL) {
                device = bonded;
                break;
            }
        }

        if (device == null) {
            Log.e(TAG, "There must be a device BT bonded already");
            finish();
            return;
        }

        final BluetoothPairingDialog diag = new BluetoothPairingDialog(this);

        mResultReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String code = intent.getStringExtra(A2AHelper.EXTRA_CODE);
                if (code == null
                        || !code.equals(getIntent().getStringExtra(A2AHelper.EXTRA_CODE))) {
                    Log.e(TAG, "Got finish for wrong code, ignoring");
                    return;
                }
                Log.e(TAG, "Got finish for a2a pairing");

                if (!intent.getBooleanExtra(A2AHelper.EXTRA_SUCCESS, false)) {
                    mCancelOnExit = false;
                }

                diag.dismiss();
                finish();
            }
        };
        registerReceiver(
            mResultReceiver,
            new IntentFilter(A2ABroadcastReceiver.ACTION_A2A_RESULT_INTERNAL)
        );

        final String code = A2AHelper.formatCode(getIntent().getStringExtra(A2AHelper.EXTRA_CODE));

        diag.setDevice(device);
        diag.setPairingMessage(getString(R.string.bluetooth_confirm_code), code);
        diag.setOnDismissListener((dialog) -> {
            if (mCancelOnExit) {
                A2AHelper.rejectPairing(getIntent());
                mCancelOnExit = false;
            }
            finish();
        });

        diag.setPositiveButton((dialog, which) -> {
            A2AHelper.acceptPairing(getIntent());
            mCancelOnExit = false;
        });
        diag.setNegativeButton((dialog, which) -> { /* do nothing, just show button */ });
        diag.show();
    }

    @Override
    protected void onDestroy() {
        if (mResultReceiver != null) {
            unregisterReceiver(mResultReceiver);
        }

        super.onDestroy();
    }
}
