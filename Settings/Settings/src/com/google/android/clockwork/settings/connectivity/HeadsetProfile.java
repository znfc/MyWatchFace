package com.google.android.clockwork.settings.connectivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.content.Context;

/**
 * Headset profile wrapper around the bluetooth profile.
 */
public class HeadsetProfile extends BtProfile {
    private static final String NAME = "HeadsetProfile";
    public HeadsetProfile(Context context, BluetoothAdapter adapter) {
        super(context, adapter, BluetoothProfile.HEADSET);
    }

    @Override
    public String toString() {
        return NAME;
    }

    @Override
    protected boolean connect(BluetoothProfile profile, BluetoothDevice device) {
        return ((BluetoothHeadset) profile).connect(device);
    }

    @Override
    protected boolean disconnect(BluetoothProfile profile, BluetoothDevice device) {
        return ((BluetoothHeadset) profile).disconnect(device);
    }

    @Override
    protected int getPriority(BluetoothProfile profile, BluetoothDevice device) {
        return ((BluetoothHeadset) profile).getPriority(device);
    }

    @Override
    protected void setPriority(BluetoothProfile profile, BluetoothDevice device, int priority) {
        ((BluetoothHeadset) profile).setPriority(device, priority);
    }

    @Override
    public boolean isSupported(BluetoothDevice device) {
        return BluetoothUuid.isUuidPresent(device.getUuids(), BluetoothUuid.Handsfree);
    }
}
