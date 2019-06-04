package com.google.android.clockwork.settings.connectivity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothUuid;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.os.UserHandle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Listens to bond state changes and tries to connect to newly bonded devices using the A2DP
 * profile.
 */
public class BluetoothBondStateChangeReceiver extends BroadcastReceiver {
    // Bluetooth device to check for possible profile service start.
    private static BluetoothDevice sDevice = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            final int bondState = intent.getIntExtra(
                    BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
            if (device != null && bondState == BluetoothDevice.BOND_BONDED) {
                final ParcelUuid[] uuids = device.getUuids();
                // If uuids are null then the UUID fetch most likely has not
                // yet completed.  In this case we may connect to the profile
                // on the UUID broadcast intent after fetched from remote device.
                sDevice = (uuids == null ? device : null);
                maybeConnectToProfile(context, device, uuids);
            }
        } else if (BluetoothDevice.ACTION_UUID.equals(intent.getAction())) {
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device != null && sDevice != null && device.equals(sDevice)
                    && device.getBondState() == BluetoothDevice.BOND_BONDED) {
                final ParcelUuid[] uuids = device.getUuids();
                maybeConnectToProfile(context, device, uuids);
            }
            sDevice = null;
        }
    }

    private void maybeConnectToProfile(@NonNull final Context context,
            @NonNull final BluetoothDevice device, @Nullable final ParcelUuid[] uuids) {
        // If the device's UUIDs indicate that the device supports A2DP,
        // try to connect to it.
        if (uuids != null
                && BluetoothUuid.containsAnyUuid(uuids, A2dpProfile.A2DP_UUIDS)) {
            // A2DP profile proxy has to be requested asynchronously, which cannot be
            // done from a broadcast receiver, so start a service which will handle that
            // for us and then it will connect to the device.
            final Intent connectIntent = new Intent(context, A2dpConnectService.class);
            connectIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
            context.startServiceAsUser(connectIntent, UserHandle.CURRENT_OR_SELF);
        }
    }
}
