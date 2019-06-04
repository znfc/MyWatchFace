package com.google.android.clockwork.settings.connectivity;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

/**
 * BtProfile wrapper for BluetoothA2dp profile.
 */
public final class A2dpProfile extends BtProfile {
    private static final String TAG = "A2dpProfile";
    private static final String NAME = "A2dpProfile";

    public static final ParcelUuid[] A2DP_UUIDS = {
            BluetoothUuid.AudioSink,
            BluetoothUuid.AdvAudioDist,
            BluetoothUuid.AvrcpController,
    };

    public A2dpProfile(Context context, BluetoothAdapter adapter) {
        super(context, adapter, BluetoothProfile.A2DP);
    }

    @Override
    public String toString() {
        return NAME;
    }

    @Override
    protected boolean connect(BluetoothProfile profile, BluetoothDevice device) {
        logDebug("connected() device " + device.getAddress());
        return ((BluetoothA2dp) profile).connect(device);
    }

    @Override
    protected boolean disconnect(BluetoothProfile profile, BluetoothDevice device) {
        logDebug("disconnected() device " + device.getAddress());
        return ((BluetoothA2dp) profile).disconnect(device);
    }

    @Override
    protected int getPriority(BluetoothProfile profile, BluetoothDevice device) {
        logDebug("getPriority() device " + device.getAddress());
        return ((BluetoothA2dp) profile).getPriority(device);
    }

    @Override
    protected void setPriority(BluetoothProfile profile, BluetoothDevice device, int priority) {
        logDebug("setPriority() device " + device.getAddress() + " priority " + priority);
        ((BluetoothA2dp) profile).setPriority(device, priority);
    }

    @Override
    public boolean isSupported(BluetoothDevice device) {
        return BluetoothUuid.containsAnyUuid(device.getUuids(), A2DP_UUIDS);
    }

    private void logDebug(final String s) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, s);
        }
    }
}
