package com.google.android.clockwork.settings.connectivity.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.util.Log;

/**
 * This reciever listens to bluetooth ACL connection state changes
 * and adapter state changes.
 *
 * These changes are then passed onto a service to be persisted.
 */
public class BluetoothAclReceiver extends BroadcastReceiver {
    private static final String TAG = BluetoothAclReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        final Intent newIntent = new Intent(context, BluetoothAclService.class);
        final String action = intent.getAction();
        int stateChange = 0;
        BluetoothDevice device = null;
        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            stateChange = BluetoothAclService.ACL_CONNECTED;
            device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            stateChange = BluetoothAclService.ACL_DISCONNECTED;
            device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        } else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
            stateChange = BluetoothAclService.ACL_DISCONNECTING;
            device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            stateChange = BluetoothAclService.STATE_CHANGED;
        } else {
            Log.w(TAG, "Unexpected intent sent " + intent.toString());
            return;
        }
        newIntent.putExtra(BluetoothAclService.EXTRA_STATE_CHANGE, stateChange);
        newIntent.putExtra(BluetoothAclService.EXTRA_DEVICE, device);
        context.startServiceAsUser(newIntent, UserHandle.CURRENT_OR_SELF);
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Received broadcast " + intent.toString());
        }
    }
}
