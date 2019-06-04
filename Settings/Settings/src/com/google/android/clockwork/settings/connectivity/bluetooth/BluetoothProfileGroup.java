package com.google.android.clockwork.settings.connectivity.bluetooth;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.google.android.clockwork.settings.connectivity.A2dpProfile;
import com.google.android.clockwork.settings.connectivity.BtProfile;
import com.google.android.clockwork.settings.connectivity.ConnectivityConsts;
import com.google.android.clockwork.settings.connectivity.HeadsetProfile;
import com.google.android.clockwork.settings.connectivity.ShamProxyProfile;
import com.google.common.base.Preconditions;

import java.util.Arrays;
import java.util.List;

/**
 * Thin wrapper of the collection of all bluetooth profiles available to the device.
 */
public class BluetoothProfileGroup {
    private static final String TAG = BluetoothProfileGroup.class.getSimpleName();
    private Context mContext;
    private BluetoothProfileGroup.ProfileCallback mCallback;
    private List<BtProfile> mProfiles;
    private boolean mBluetoothProfileReceiverRegistered;

    /**
     * @param  context          The context
     * @param  bluetoothAdapter The bluetooth adapter
     * @param  callback         A callback used whenever any available profile state changes
     */
    public BluetoothProfileGroup(Context context, BluetoothAdapter bluetoothAdapter,
            ProfileCallback callback) {
        mContext = context;
        mCallback = callback;

        mProfiles = Arrays.asList(
                new A2dpProfile(context, bluetoothAdapter),
                new HeadsetProfile(context, bluetoothAdapter),
                new ShamProxyProfile(context, bluetoothAdapter));

        for (final BtProfile profile : mProfiles) {
            profile.connectToAdapter();
        }
    }

    /**
     * Receiver used to listen for profile changes and update preferences
     * accordingly.
     */
    private final BroadcastReceiver mBluetoothProfileReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                final int profileState = intent.getIntExtra(
                        android.bluetooth.BluetoothHeadset.EXTRA_STATE,
                        android.bluetooth.BluetoothHeadset.STATE_DISCONNECTED);
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Broadcast.onReceive: got headset ACTION_CONNECTION_STATE_CHANGED"
                            + ", profileState=" + profileState + ", isInitialSticky="
                            + isInitialStickyBroadcast());
                }

                final BluetoothDevice device = intent.getParcelableExtra(
                        BluetoothDevice.EXTRA_DEVICE);
                if (mCallback != null) {
                    mCallback.onProfileChanged(device, profileState);
                }
            } else if (BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                // This is the profile state.
                final int profileState = intent.getIntExtra(
                        android.bluetooth.BluetoothA2dp.EXTRA_STATE,
                        android.bluetooth.BluetoothA2dp.STATE_DISCONNECTED);
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Broadcast.onReceive: got a2dp ACTION_CONNECTION_STATE_CHANGED"
                            + ", profileState=" + profileState + ", isInitialSticky="
                            + isInitialStickyBroadcast());
                }
                final BluetoothDevice device = intent.getParcelableExtra(
                        BluetoothDevice.EXTRA_DEVICE);
                if (mCallback != null) {
                    mCallback.onProfileChanged(device, profileState);
                }
            } else if (action.equals(ConnectivityConsts.ACTION_SHAM_PROXY_PROFILE_CHANGE)) {
                // This is the profile state.
                final int profileState = intent.getIntExtra(
                        android.bluetooth.BluetoothProfile.EXTRA_STATE,
                        android.bluetooth.BluetoothProfile.STATE_DISCONNECTED);
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Broadcast.onReceive: got ShamProxy ACTION_CONNECTION_STATE_CHANGED"
                            + ", profileState=" + profileState + ", isInitialSticky="
                            + isInitialStickyBroadcast());
                }
                final BluetoothDevice device = intent.getParcelableExtra(
                        BluetoothDevice.EXTRA_DEVICE);
                if (mCallback != null) {
                    mCallback.onProfileChanged(device, profileState);
                }
            }
        }
    };

    public void onResume(final BtProfile.ProfileStateChangeListener listener) {
        mProfiles.forEach(p -> p.setProfileStateChangeListener(listener));

        final IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(ConnectivityConsts.ACTION_SHAM_PROXY_PROFILE_CHANGE);
        mContext.registerReceiver(mBluetoothProfileReceiver, filter);
        mBluetoothProfileReceiverRegistered = true;
    }

    public void onPause() {
        if (mBluetoothProfileReceiverRegistered) {
            mContext.unregisterReceiver(mBluetoothProfileReceiver);
            mBluetoothProfileReceiverRegistered = false;
        }
        mProfiles.forEach(p -> p.setProfileStateChangeListener(null));
    }

    public void onDestroy() {
        mProfiles.forEach(p -> p.release());
    }

    /**
     * Connect all possible profiles to this device.
     */
    public void connectDevice(final BluetoothDevice device) {
        Preconditions.checkNotNull(device);
        mProfiles.forEach(p -> {
                if (p.isSupported(device)) {
                    p.connectToDevice(device);
                }
        });
    }

    /**
     * Disconnect all possible profiles from this device.
     */
    public void disconnectDevice(final BluetoothDevice device) {
        Preconditions.checkNotNull(device);
        mProfiles.forEach(p -> {
                if (p.isSupported(device)) {
                    p.disconnectFromDevice(device);
                }
        });
    }

    /**
     * Return the number of profiles we support with the given remote device.
     */
    public int getProfilesSupported(final BluetoothDevice device) {
        Preconditions.checkNotNull(device);
        int profilesSupported = 0;

        for (final BtProfile profile : mProfiles) {
            if (profile.isSupported(device)) {
                profilesSupported++;
            }
        }
        return profilesSupported;
    }

    public boolean isAnyProfileSupported(final BluetoothDevice device) {
        return getProfilesSupported(device) > 0;
    }

    /**
     * Examine all supported profiles for current connection status.
     *
     * Any overlapping supported profiles between our device and the
     * remote device is checked for connection status.  If any of
     * the profiles are connected then the remote device is considered
     * connected.  If no profiles are connected, the remote device is
     * considered disconnected.
     *
     * @param device the bluetooth remote device to examine.
     * @return       a bluetooth profile connection state
     * @see          {android.bluetooth.BluetoothProfile}
     */
    public int getConnectionStatus(final BluetoothDevice device) {
        Preconditions.checkNotNull(device);
        int connectionStatus = BluetoothProfile.STATE_DISCONNECTED;

        for (final BtProfile profile : mProfiles) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "device " + device.getAddress() + " profile " + profile
                        + " is supported " + profile.isSupported(device) + " state "
                        + profile.getConnectionStatus(device));
            }
            if (profile.isSupported(device)) {
                switch (profile.getConnectionStatus(device)) {
                    case BluetoothProfile.STATE_CONNECTED:
                        // if something is connected, it overrides all other status
                        return BluetoothProfile.STATE_CONNECTED;
                    case BluetoothProfile.STATE_CONNECTING:
                        // only thing overriding this is connected, and that returns immediately
                        connectionStatus = BluetoothProfile.STATE_CONNECTING;
                        break;
                    case BluetoothProfile.STATE_DISCONNECTING:
                        // check for connecting, b/c it's higher priority
                        // and connected returns immediately
                        if (connectionStatus != BluetoothProfile.STATE_CONNECTING) {
                            connectionStatus = BluetoothProfile.STATE_DISCONNECTING;
                        }
                        break;
                        // no cases needed for disconnected as that's the default
                }
            }
        }
        return connectionStatus;
    }

    /**
     * Used to call back when a profile connection state has changed.
     */
    public interface ProfileCallback {
        void onProfileChanged(BluetoothDevice device, int state);
    }
}
