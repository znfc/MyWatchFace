package com.google.android.clockwork.settings.connectivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadsetClient;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.os.ParcelUuid;

/**
 * BtProfile wrapper for BluetoothHeadsetClient (which is actually a BluetoothProfile).
 */
final class HfpProfile extends BtProfile {

    private static final ParcelUuid[] HANDSFREE_UUIDS = {
            BluetoothUuid.Handsfree_AG
    };

    public static boolean isHandsfreeAudioGateway(BluetoothDevice device) {
        return BluetoothUuid.containsAnyUuid(device.getUuids(), HANDSFREE_UUIDS);
    }

    public HfpProfile(Context context, BluetoothAdapter adapter) {
        super(context, adapter, BluetoothProfile.HEADSET_CLIENT);
    }

    @Override
    protected boolean connect(BluetoothProfile profile, BluetoothDevice device) {
        return ((BluetoothHeadsetClient) profile).connect(device);
    }

    @Override
    protected boolean disconnect(BluetoothProfile profile, BluetoothDevice device) {
        return ((BluetoothHeadsetClient) profile).disconnect(device);
    }

    @Override
    protected int getPriority(BluetoothProfile profile, BluetoothDevice device) {
        return ((BluetoothHeadsetClient) profile).getPriority(device);
    }

    @Override
    protected void setPriority(BluetoothProfile profile, BluetoothDevice device, int priority) {
        ((BluetoothHeadsetClient) profile).setPriority(device, priority);
    }

    @Override
    public boolean isSupported(BluetoothDevice device) {
        return isHandsfreeAudioGateway(device);
    }
}
