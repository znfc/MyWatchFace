package com.google.android.clockwork.settings.connectivity.bluetooth;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;

import com.google.android.apps.wearable.settings.R;

/**
 * Tells the user which PIN/passkey should be entered on a remote Bluetooth device to pair with it.
 *
 * <p>It is shown when processing pairing requests with the following pairing variants:
 * {@link BluetoothDevice.PAIRING_VARIANT_DISPLAY_PASSKEY} and
 * {@link BluetoothDevice.PAIRING_VARIANT_DISPLAY_PIN}.
 */
public class BluetoothDisplayPasskeyActivity extends WearableActivity {
    public static final String EXTRA_PAIRING_KEY = "pairing_key";

    private static final String TAG = "BluetoothDisplayPasskey";

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (mDialog == null || mDialog.getDevice() == null) {
                return;
            }
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)
                    && mDialog.getDevice().equals(device)) {
                int bondState = intent.getIntExtra(
                        BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                if (bondState == BluetoothDevice.BOND_BONDED) {
                    // Pairing was successfully finished.
                    mCancelOnExit = false;
                    mDialog.dismiss();
                } else if (bondState == BluetoothDevice.BOND_NONE) {
                    // Already cancelled.
                    mCancelOnExit = false;
                    mDialog.dismiss();
                }
            } else if (BluetoothDevice.ACTION_PAIRING_CANCEL.equals(intent.getAction())
                    && mDialog.getDevice().equals(device)) {
                // Already cancelled.
                mCancelOnExit = false;
                mDialog.dismiss();
            }
        }
    };

    /**
     * Specifies whether the activity should cancel pairing once it finishes.
     *
     * <p>Pairing must be cancelled if it didn't finish successfully and if it wasn't cancelled by
     * the remote device.
     */
    private boolean mCancelOnExit = true;
    private BluetoothPairingDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDialog = new BluetoothPairingDialog(this);
        if (!mDialog.setDevice(getIntent())) {
            finish();
            return;
        }

        String pairingKey = getIntent().getStringExtra(EXTRA_PAIRING_KEY);
        if (pairingKey == null) {
            Log.e(TAG, "Pairing key cannot be null.");
            finish();
            return;
        }

        int type = getIntent().getIntExtra(
                BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.ERROR);
        if (type == BluetoothDevice.PAIRING_VARIANT_DISPLAY_PASSKEY) {
            mDialog.getDevice().setPairingConfirmation(true);
        } else if (type == BluetoothDevice.PAIRING_VARIANT_DISPLAY_PIN) {
            mDialog.getDevice().setPin(BluetoothDevice.convertPinToBytes(pairingKey));
        } else {
            Log.e(TAG, String.format("Unknown pairing variant %d.", type));
            finish();
            return;
        }

        mDialog.setPairingMessage(
                getString(mDialog.getDevice().getBluetoothClass().getMajorDeviceClass()
                        == BluetoothClass.Device.Major.PHONE
                        ? R.string.bluetooth_enter_code_on_phone
                        : R.string.bluetooth_enter_code),
                pairingKey);

        // Show cancel button if not used in the Setup flow.
        if (Settings.System.getInt(getContentResolver(), Settings.System.SETUP_WIZARD_HAS_RUN, 0)
                != 0) {
            mDialog.setNegativeButton((dialog, which) -> { /* do nothing, just show button */ });
        }

        mDialog.setOnDismissListener((dialog) -> {
            if (mCancelOnExit) {
                mDialog.getDevice().cancelPairingUserInput();
                mCancelOnExit = false;
            }
            finish();
        });
        mDialog.show();

        registerReceiver(mBroadcastReceiver,
                new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        registerReceiver(mBroadcastReceiver,
                new IntentFilter(BluetoothDevice.ACTION_PAIRING_CANCEL));
    }
}
