package com.google.android.clockwork.settings.connectivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Base class for BluetoothProfile wrapper classes. This code is based on the A2dpProfile class in
 * in the standard non-wear Settings app in packages/apps/Settings. Some parts of the original code
 * were removed, as they were not needed here.
 */
public abstract class BtProfile {
    private static final String TAG = "BtProfile";
    private static final boolean DEBUG = false;

    private final Context mContext;
    private final BluetoothAdapter mBluetoothAdapter;
    private final int mProfileId;

    private ProfileStateChangeListener mProfileStateChangeListener;
    private BluetoothProfile mService;

    // These callbacks run on the main thread.
    private final class ServiceListener implements BluetoothProfile.ServiceListener {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (DEBUG) Log.d(TAG,"Bluetooth service connected");
            mService = proxy;
            if (mProfileStateChangeListener != null) {
                mProfileStateChangeListener.onProfileStateChanged();
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            if (DEBUG) Log.d(TAG,"Bluetooth service disconnected");
            mService = null;
            if (mProfileStateChangeListener != null) {
                mProfileStateChangeListener.onProfileStateChanged();
            }
        }

    }

    public BtProfile(Context context, BluetoothAdapter adapter, int profileId) {
        // Use the application context so that any actions taken through the BT adapter
        // are not done using Settings activities.
        // See http://b/37300026
        mContext = context.getApplicationContext();
        mBluetoothAdapter = adapter;
        mProfileId = profileId;
    }

    /**
     * Call this initially to connect to the profile service.
     */
    public void connectToAdapter() {
        mBluetoothAdapter.getProfileProxy(mContext, new ServiceListener(), mProfileId);
    }

    /**
     * The listener will be called on the main thread.
     */
    public void setProfileStateChangeListener(@Nullable ProfileStateChangeListener listener) {
        mProfileStateChangeListener = listener;
    }

    public List<BluetoothDevice> getConnectedDevices() {
        if (mService == null) return new ArrayList<BluetoothDevice>(0);
        return mService.getDevicesMatchingConnectionStates(
                new int[]{BluetoothProfile.STATE_CONNECTED,
                        BluetoothProfile.STATE_CONNECTING,
                        BluetoothProfile.STATE_DISCONNECTING});
    }

    public boolean connectToDevice(BluetoothDevice device) {
        if (mService == null) return false;
        List<BluetoothDevice> connectedDevices = getConnectedDevices();
        if (connectedDevices != null) {
            for (BluetoothDevice sink : connectedDevices) {
                if (!device.equals(sink)) {
                    disconnect(mService, sink);
                }
            }
        }
        return connect(mService, device);
    }

    public boolean disconnectFromDevice(BluetoothDevice device) {
        if (mService == null) return false;
        // Downgrade priority as user is disconnecting the headset.
        if (getPriority(mService, device) > BluetoothProfile.PRIORITY_ON){
            setPriority(mService, device, BluetoothProfile.PRIORITY_ON);
        }
        return disconnect(mService, device);
    }

    public int getConnectionStatus(BluetoothDevice device) {
        if (mService == null) {
            return BluetoothProfile.STATE_DISCONNECTED;
        }
        return mService.getConnectionState(device);
    }

    public void release() {
        if (mService != null) {
            try {
                BluetoothAdapter.getDefaultAdapter().closeProfileProxy(mProfileId, mService);
                mService = null;
            } catch (Throwable t) {
                Log.w(TAG, "Error cleaning up proxy", t);
            }
        }
    }

    public interface ProfileStateChangeListener {
        void onProfileStateChanged();
    }

    protected Context getContext() {
        return mContext;
    }

    protected BluetoothAdapter getAdapter() {
        return mBluetoothAdapter;
    }

    /** Connect a device to the given profile. */
    protected abstract boolean connect(BluetoothProfile profile, BluetoothDevice device);

    /** Disconnect a device from the given profile. */
    protected abstract boolean disconnect(BluetoothProfile profile, BluetoothDevice device);

    /** Get the priority of the given device for the given profile. */
    protected abstract int getPriority(BluetoothProfile profile, BluetoothDevice device);

    /** Set the priority of the given device for the given profile. */
    protected abstract void setPriority(
            BluetoothProfile profile, BluetoothDevice device, int priority);

    public abstract boolean isSupported(BluetoothDevice device);
}
