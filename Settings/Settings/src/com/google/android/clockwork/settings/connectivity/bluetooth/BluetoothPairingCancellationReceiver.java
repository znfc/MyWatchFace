package com.google.android.clockwork.settings.connectivity.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * A broadcast receiver for receiving pairing state changes and pairing cancellation broadcasts
 * within a pairing user input activity. It dismisses the activity if the pairing state changes or
 * if the pairing is cancelled.
 */
public class BluetoothPairingCancellationReceiver extends BroadcastReceiver {

    public interface PairingCancelledListener {
        void onPairingCancelled();
    }

    public static final String EXTRA_DEVICE_ADDRESS = "device_mac_address";

    private final Activity mPairingUserInputActivity;
    private final PairingCancelledListener mPairingCancelledListener;

    public BluetoothPairingCancellationReceiver(
            Activity pairingUserInputActivity, PairingCancelledListener listener) {
        mPairingUserInputActivity = pairingUserInputActivity;
        mPairingCancelledListener = listener;
    }

    public BluetoothPairingCancellationReceiver(Activity pairingUserInputActivity) {
        this(pairingUserInputActivity, null);
    }

    public void register() {
        mPairingUserInputActivity.registerReceiver(
                this, new IntentFilter(BluetoothDevice.ACTION_PAIRING_CANCEL));
        mPairingUserInputActivity.registerReceiver(
                this, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
    }

    public void unregister() {
        mPairingUserInputActivity.unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        BluetoothDevice bondingDevice = mPairingUserInputActivity.getIntent().getParcelableExtra(
                BluetoothDevice.EXTRA_DEVICE);
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        if (BluetoothDevice.ACTION_PAIRING_CANCEL.equals(intent.getAction())
                && (device == null || device.equals(bondingDevice))) {
            if (mPairingCancelledListener != null) {
                mPairingCancelledListener.onPairingCancelled();
            }
            mPairingUserInputActivity.finish();
        } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {
            int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                    BluetoothDevice.ERROR);
            if ((device == null || device.equals(bondingDevice))
                    && (bondState == BluetoothDevice.BOND_NONE
                    || bondState == BluetoothDevice.BOND_BONDED)) {
                mPairingUserInputActivity.finish();
            }
        }
    }

}
