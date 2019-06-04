package com.google.android.clockwork.settings.connectivity.bluetooth;

import android.app.IntentService;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.clockwork.common.content.CwPrefs;
import com.google.android.clockwork.settings.connectivity.ConnectivityConsts;

/**
 * Service that receives companion mac address and connectivity state
 * and persists to system properties.
 *
 * Any ACL created connected to a phone is assumed to be the (one of)
 * companion phone(s).
 */
public class BluetoothAclService extends IntentService {
    private static final String TAG = "BluetoothAclService";

    private static final int UNKNOWN_BT_CLASS = -1;
    public static final int STATE_CHANGED = 1;
    public static final int ACL_CONNECTED = 2;
    public static final int ACL_DISCONNECTED = 3;
    public static final int ACL_DISCONNECTING = 4;

    public static final String EXTRA_STATE_CHANGE
            = "com.google.android.clockwork.settings.connectivity.bluetooth.EXTRA_STATE_CHANGE";
    public static final String EXTRA_DEVICE
            = "com.google.android.clockwork.settings.connectivity.bluetooth.EXTRA_DEVICE";

    public BluetoothAclService() {
        super(TAG);
    }

    @Override
    public void onHandleIntent(Intent intent) {
        final SharedPreferences prefs = CwPrefs.DEFAULT.get(this);
        final int stateChange = intent.getIntExtra(EXTRA_STATE_CHANGE, 0);
        final BluetoothDevice device = intent.getParcelableExtra(EXTRA_DEVICE);

        int state = BluetoothProfile.STATE_DISCONNECTED;
        String address = prefs.getString(
                ConnectivityConsts.PREFERENCE_COMPANION_ADDRESS, "");
        int bluetoothClass = UNKNOWN_BT_CLASS;

        switch (stateChange) {
            case BluetoothAclService.ACL_CONNECTED:
                state = BluetoothProfile.STATE_CONNECTED;
                // fall through
            case BluetoothAclService.ACL_DISCONNECTED:
            case BluetoothAclService.ACL_DISCONNECTING:
                address = device.getAddress();
                if (device.getBluetoothClass() != null) {
                    bluetoothClass = device.getBluetoothClass().getMajorDeviceClass();
                }
                break;
            case BluetoothAclService.STATE_CHANGED:
                break;
        }

        if (bluetoothClass == BluetoothClass.Device.Major.PHONE
                || stateChange == BluetoothAclService.STATE_CHANGED) {
            prefs.edit()
                .putString(ConnectivityConsts.PREFERENCE_COMPANION_ADDRESS, address)
                .putInt(ConnectivityConsts.PREFERENCE_COMPANION_CONNECT_STATE, state)
                .apply();
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Updated companion state " + address + " " + state);
            }
        }
    }
}
