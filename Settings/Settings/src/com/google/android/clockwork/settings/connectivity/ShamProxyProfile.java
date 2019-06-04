package com.google.android.clockwork.settings.connectivity;

import android.annotation.Nullable;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.UserHandle;
import com.google.android.clockwork.common.concurrent.CwAsyncTask;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.common.annotations.VisibleForTesting;

/**
 * Bluetooth profile wrapper for the proxy connection.
 *
 * There is no such thing as a proxy profile but for convenience this
 * class provides the same API to the Bluetooth settings module.
 *
 * This class listens for shared preference state changes for either
 * companion BT mac updates or companion ACL state changes.  When these
 * changes occur this class broadcasts the companion mac address and
 * the current proxy connectivity state.
 */
public final class ShamProxyProfile extends BtProfile {
    private static final String TAG = ShamProxyProfile.class.getSimpleName();
    private static final String NAME = TAG;

    private Context mContext;
    private BluetoothAdapter mAdapter;
    private ContentResolver mResolver;
    private ConnectivityManager mConnectivityManager;

    private int mBluetoothMode;
    private String mCompanionAddress;
    private boolean mProxyConnected;

    private final Handler mHandler = new Handler();
    private final NetworkRequest mNetworkRequest =
        new NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_BLUETOOTH)
            .build();

    public ShamProxyProfile(Context context, BluetoothAdapter adapter) {
        super(context, adapter, 0);
        initialize(
                context,
                adapter,
                context.getContentResolver(),
                context.getSystemService(ConnectivityManager.class));
    }

    @VisibleForTesting
    void initialize(
            final Context context,
            final BluetoothAdapter adapter,
            final ContentResolver resolver,
            final ConnectivityManager connectivityManager) {
        mContext = context;
        mAdapter = adapter;
        mResolver = resolver;
        mConnectivityManager = connectivityManager;

        mConnectivityManager.registerNetworkCallback(mNetworkRequest, mMonitorBtProxyCallback);
        new ParseBluetoothContentProvider().submit();
    }

    /**
     * Returns the state of the connection
     *
     * See {@BluetoothProfile} for return values
     */
    @Override
    public int getConnectionStatus(@Nullable final BluetoothDevice device) {
        if (isBluetoothDeviceTheCompanion(device)) {
            return getBluetoothProfileProxyConnectionState();
        }
        return BluetoothProfile.STATE_DISCONNECTED;
    }

    private int getBluetoothProfileProxyConnectionState() {
        return mProxyConnected
            ? BluetoothProfile.STATE_CONNECTED
            : BluetoothProfile.STATE_DISCONNECTED;
    }

    /**
     * Sham proxy has no capability to connect or disconnect as this is deferred
     * to the connectivity service.
     */
    @Override
    protected boolean connect(BluetoothProfile profile, BluetoothDevice device) {
        return true;
    }

    @Override
    protected boolean disconnect(BluetoothProfile profile, BluetoothDevice device) {
        return true;
    }

    /**
     * Sham proxy has no concept of connecting to adapter.
     */
    @Override
    public void connectToAdapter() {
    }

    /**
     * Sham proxy has no concept of priority.
     */
    @Override
    protected int getPriority(BluetoothProfile profile, BluetoothDevice device) {
        return 0;
    }

    @Override
    protected void setPriority(BluetoothProfile profile, BluetoothDevice device, int priority) {
    }

    @Override
    public void release() {
        mConnectivityManager.unregisterNetworkCallback(mMonitorBtProxyCallback);
    }

    @Override
    public boolean isSupported(BluetoothDevice device) {
        return isBluetoothDeviceTheCompanion(device);
    }

    @Override
    public String toString() {
        return NAME;
    }

    private void notifyProxyChanged(@Nullable final BluetoothDevice device, int state) {
        if (device != null) {
            final Intent intent = new Intent(
                ConnectivityConsts.ACTION_SHAM_PROXY_PROFILE_CHANGE);
            intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
            intent.putExtra(android.bluetooth.BluetoothHeadset.EXTRA_STATE,
                state);
            mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT_OR_SELF);
        }
    }

    private boolean isBluetoothDeviceTheCompanion(@Nullable final BluetoothDevice device) {
        if (device != null && mCompanionAddress != null) {
            return mCompanionAddress.equals(device.getAddress());
        }
        return false;
    }

    private BluetoothDevice getCompanionBluetoothDevice() {
        return BluetoothAdapter.checkBluetoothAddress(mCompanionAddress)
                ? mAdapter.getRemoteDevice(mCompanionAddress)
                : null;
    }

    /**
     * There is no content observer here because this object is part of the
     * {@link BluetoothProfileGroup} object that is scoped within the
     * lifecycle of {@link BluetoothSettingsFragment}.
     */
    private void parseContentProvider() {
        final Cursor cursor = mResolver.query(
                SettingsContract.BLUETOOTH_URI, null, null, null, null);
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    if (SettingsContract.KEY_BLUETOOTH_MODE.equals(cursor.getString(0))) {
                        setBluetoothMode(cursor.getInt(1));
                    } else if (SettingsContract.KEY_COMPANION_ADDRESS.equals(cursor.getString(0))) {
                        setCompanionAddress(cursor.getString(1));
                    }
                }
            } finally {
                cursor.close();
            }
        }
    }

    /**
     * Use AsyncTask to avoid disk access in the main thread.
     */
    private class ParseBluetoothContentProvider extends CwAsyncTask<Void, Void, Void> {
        public ParseBluetoothContentProvider() {
            super("ParseBluetoothContentProviderTask");
        }

        @Override
        public Void doInBackground(Void... params) {
            parseContentProvider();
            return null;
        }
    }

    private void setProxyConnected(boolean proxyConnected) {
        mProxyConnected = proxyConnected;
        notifyProxyChanged(
                getCompanionBluetoothDevice(),
                getBluetoothProfileProxyConnectionState());
    }

    @VisibleForTesting
    boolean isProxyConnected() {
        return mProxyConnected;
    }

    private void setBluetoothMode(int bluetoothMode) {
        mBluetoothMode = bluetoothMode;
    }

    @VisibleForTesting
    int getBluetoothMode() {
        return mBluetoothMode;
    }

    private void setCompanionAddress(final String address) {
        mCompanionAddress = address;
        notifyProxyChanged(
                getCompanionBluetoothDevice(),
                getBluetoothProfileProxyConnectionState());
    }

    @VisibleForTesting
    String getCompanionAddress() {
        return mCompanionAddress;
    }

    @VisibleForTesting
    final ConnectivityManager.NetworkCallback mMonitorBtProxyCallback =
        new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                mHandler.post(
                    new Runnable() {
                        @Override
                        public void run() {
                            setProxyConnected(true);
                        }
                });
            }

            @Override
            public void onLost(Network network) {
                mHandler.post(
                    new Runnable() {
                        @Override
                        public void run() {
                            setProxyConnected(false);
                        }
                });
            }
          };
}
