package com.google.android.clockwork.settings.connectivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.android.clockwork.common.concurrent.AbstractCwRunnable;
import com.google.android.clockwork.common.concurrent.Executors;
import com.google.android.clockwork.settings.SettingsIntents;
import com.google.android.clockwork.common.content.CwPrefs;

/**
 * A receiver to be passed the MAC address of the bluetooth companion device. It stores the address,
 * so the companion device can be later filtered out of the list of paired bluetooth devices in the
 * bluetooth settings. We want to filter this device out, because we don't want the user to be able
 * to unpair from it on the watch.
 */
public class BluetoothCompanionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent.hasExtra(SettingsIntents.EXTRA_COMPANION_MAC_ADDRESS)) {
            Executors.INSTANCE.get(context).getBackgroundExecutor()
                    .submit(new AbstractCwRunnable("BluetoothCompanionReceiver") {
                        @Override
                        public void run() {
                            String companionBluetoothAddress = intent.getExtras()
                                    .getString(SettingsIntents.EXTRA_COMPANION_MAC_ADDRESS);
                            SharedPreferences preferences = CwPrefs.wrap(context,
                                    ConnectivityConsts.BLUETOOTH_PREFERENCES);
                            BluetoothAdapter bluetoothAdapter = BluetoothAdapter
                                    .getDefaultAdapter();
                            String oldCompanionAddress = preferences.getString(
                                    ConnectivityConsts.COMPANION_MAC_ADDRESS_PREFERENCE, null);
                            if (bluetoothAdapter != null && oldCompanionAddress != null
                                    && BluetoothAdapter
                                            .checkBluetoothAddress(oldCompanionAddress)) {
                                BluetoothDevice bluetoothDevice = bluetoothAdapter
                                        .getRemoteDevice(oldCompanionAddress);
                                if (bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                                    // Don't store the new companion's address if the old companion
                                    // is still paired
                                    // to the clockwork device.
                                    // This is needed for some CtsVerifier test cases which involve
                                    // temporarily
                                    // pairing two clockwork devices to each other. We don't want
                                    // the remote
                                    // clockwork device's address to overwrite the companion's mac
                                    // address in those
                                    // scenarios.
                                    return;
                                }
                            }
                            if (!TextUtils.isEmpty(companionBluetoothAddress)) {
                                preferences.edit()
                                        .putString(
                                                ConnectivityConsts.COMPANION_MAC_ADDRESS_PREFERENCE,
                                                companionBluetoothAddress)
                                        .apply();
                            }
                        }
                    });
        }
    }

}
