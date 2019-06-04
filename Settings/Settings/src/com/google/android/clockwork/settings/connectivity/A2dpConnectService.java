package com.google.android.clockwork.settings.connectivity;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.util.Log;

import java.util.concurrent.CountDownLatch;

/**
 * Connects to the remote Bluetooth devices via A2DP profile.
 */
public class A2dpConnectService extends IntentService implements
        A2dpProfile.ProfileStateChangeListener {

    private static final String TAG = A2dpConnectService.class.getSimpleName();

    private final CountDownLatch mProfileInitializedCountDownLatch = new CountDownLatch(1);

    private A2dpProfile mA2dpProfile;
    private BluetoothAdapter mBluetoothAdapter;

    public A2dpConnectService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mA2dpProfile = new A2dpProfile(this, mBluetoothAdapter);
        mA2dpProfile.setProfileStateChangeListener(this);
        mA2dpProfile.connectToAdapter();
    }

    @Override
    public void onDestroy() {
        mA2dpProfile.release();
        super.onDestroy();
    }

    @Override
    public void onProfileStateChanged() {
        // Signal that the profile is now available - we need to wait for the profile to be
        // available before handling the intent.
        mProfileInitializedCountDownLatch.countDown();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        try {
            // Wait until the profile is available.
            mProfileInitializedCountDownLatch.await();
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }
            // Try to connect to the device.
            if (!mA2dpProfile.connectToDevice(device)) {
                Log.e(TAG, "Failed to connect to the Bluetooth device " +
                        device.getName());
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted before connecting to the Bluetooth device " +
                    device.getName());
        }
    }

}
