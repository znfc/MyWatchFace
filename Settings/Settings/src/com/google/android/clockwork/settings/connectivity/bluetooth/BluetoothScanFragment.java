package com.google.android.clockwork.settings.connectivity.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.util.Log;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;
import com.google.common.base.Preconditions;

/**
 * Bluetooth device discovery scan for available devices.
 *
 * When this fragment is created a scan will automatically be initiated.
 * There is also a preference to initiate a BT re-scan from this
 * fragment once the initial scan has terminated due to limited discovery time.
 *
 * Once a scan has initiated, each BT device discovered is bound to a preference
 * indexed  by the BT mac address.  These preferences are then shown in an available
 * preference catagory presented to the user.
 *
 * Users click on a preference in a the available preference catagory to intiate a
 * bonding sequence with the device.  Should the bond be successful, the device will disappear
 * from the available preference catagory and appear in the previous fragment bonded
 * preference list.  If the bond fails the device will remain in the available preference
 * catagory.
 */
public class BluetoothScanFragment extends SettingsPreferenceFragment {
    private static final String TAG = "BluetoothScan";

    private static final String KEY_PREF_BLUETOOTH_SCAN = "pref_bluetoothScan";
    private static final String KEY_PREF_BLUETOOTH_AVAILABLE = "pref_bluetoothAvailable";

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothProfileGroup mBluetoothProfileGroup;

    private BluetoothStateReceiver mStateReceiver;
    private BluetoothScanReceiver mScanReceiver;

    private Preference mInitiateScanDevices;
    private PreferenceGroup mAvailableDevices;

    private int mBluetoothMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_bluetooth_scan);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothProfileGroup = new BluetoothProfileGroup(getContext(), mBluetoothAdapter,
                new ProfileCallback());

        mInitiateScanDevices = findPreference(KEY_PREF_BLUETOOTH_SCAN);
        mAvailableDevices = (PreferenceGroup) findPreference(KEY_PREF_BLUETOOTH_AVAILABLE);
        mAvailableDevices.setLayoutResource(R.layout.preference_group_no_title);

        initScanDevices(mInitiateScanDevices);
        initAvailDevices(mAvailableDevices);

        mBluetoothMode = SettingsContract.getIntValueForKey(
                getContext().getContentResolver(),
                SettingsContract.BLUETOOTH_MODE_URI,
                SettingsContract.KEY_BLUETOOTH_MODE,
                SettingsContract.BLUETOOTH_MODE_UNKNOWN);

        registerStateReceiver();
    }

    @Override
    public void onResume() {
        super.onResume();
        checkBluetoothState();
    }

    @Override
    public void onPause() {
        stopDiscovery();
        mBluetoothProfileGroup.onPause();
        unregisterScanReceiver();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mBluetoothProfileGroup.onDestroy();
        unregisterStateReceiver();
        super.onDestroy();
    }

    protected void initScanDevices(Preference pref) {
        if (mBluetoothAdapter.isDiscovering()) {
            pref.setEnabled(false);
        }

        pref.setOnPreferenceClickListener((p) -> {
            clearAvailDevices();
            startDiscovery();
            return true;
        });
    }

    protected void initAvailDevices(PreferenceGroup pref) {
        clearAvailDevices();
    }

    protected boolean isDeviceSupported(BluetoothDevice device) {
        BluetoothClass bluetoothClass = device.getBluetoothClass();
        if (bluetoothClass != null && bluetoothClass.getMajorDeviceClass()
                == BluetoothClass.Device.Major.PHONE) {
            return false;
        }

        return true;
    }

    protected BluetoothDevicePreference addAvailableDevice(BluetoothDevice device) {
        final BluetoothDevicePreference pref = findOrAllocateDevicePreference(device);
        if (device.getBondState() == BluetoothDevice.BOND_NONE) {
            mAvailableDevices.addPreference(pref);
            pref.setEnabled(true);
        }
        return pref;
    }

    /**
     * Re-examine the device and update if necessary.
     */
    protected void updateAvailableDevice(BluetoothDevice device) {
        final BluetoothDevicePreference pref = findDevicePreference(device);
        if (pref != null) {
            pref.updateBondState();
            switch (device.getBondState()) {
                case BluetoothDevice.BOND_BONDED:
                    pref.setEnabled(false);
                    mAvailableDevices.removePreference(pref);
                    break;
                case BluetoothDevice.BOND_BONDING:
                    pref.setEnabled(false);
                    break;
                case BluetoothDevice.BOND_NONE:
                    pref.setEnabled(true);
                    if (isDeviceSupported(device)) {
                        addAvailableDevice(device);
                    }
                    break;
            }
        }
    }

    protected void clearAvailDevices() {
        mAvailableDevices.removeAll();
    }

    /**
     * Handles changes in the bluetooth adapter state.
     */
    protected void checkBluetoothState() {
        switch (mBluetoothAdapter.getState()) {
            case BluetoothAdapter.STATE_OFF:
                mInitiateScanDevices.setTitle(R.string.generic_disabled);
                mInitiateScanDevices.setEnabled(false);
                clearAvailDevices();
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                mInitiateScanDevices.setEnabled(false);
                clearAvailDevices();
                break;
            case BluetoothAdapter.STATE_ON:
                mInitiateScanDevices.setTitle(R.string.pref_bluetoothScan);
                mInitiateScanDevices.setEnabled(true);
                registerScanReceiver();
                startDiscovery();
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
                mInitiateScanDevices.setEnabled(false);
                clearAvailDevices();
                break;
        }
    }

    private void startDiscovery() {
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        mBluetoothAdapter.startDiscovery();
        mInitiateScanDevices.setEnabled(false);
    }

    private void stopDiscovery() {
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
    }

    private void registerScanReceiver() {
        if (mScanReceiver != null) {
            return;
        }

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        getContext().registerReceiver(mScanReceiver = new BluetoothScanReceiver(), intentFilter);
    }

    private void unregisterScanReceiver() {
        if (mScanReceiver != null) {
            getContext().unregisterReceiver(mScanReceiver);
            mScanReceiver = null;
        }
    }

    private void registerStateReceiver() {
        Preconditions.checkArgument(mStateReceiver == null);
        final IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        getContext().registerReceiver(
                mStateReceiver = new BluetoothStateReceiver(), intentFilter);
    }

    private void unregisterStateReceiver() {
        if (mStateReceiver != null) {
            getContext().unregisterReceiver(mStateReceiver);
            mStateReceiver = null;
        }
    }

    /**
     * Handles bluetooth scan responses and other indicators.
     **/
    protected class BluetoothScanReceiver extends BroadcastReceiver {
       @Override
        public void onReceive(Context context, Intent intent) {
            if (getContext() == null) {
                Log.w(TAG, "BluetoothScanReceiver context disappeared");
                return;
            }

            final String action = intent.getAction();
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            switch (action == null ? "" : action) {
                case BluetoothDevice.ACTION_FOUND:
                    // A bluetooth device was discovered.
                    if (isDeviceSupported(device)) {
                        addAvailableDevice(device);
                    }
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    mInitiateScanDevices.setEnabled(false);
                    mInitiateScanDevices.setTitle(R.string.pref_bluetoothScan_scanning);
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    mInitiateScanDevices.setEnabled(true);
                    mInitiateScanDevices.setTitle(R.string.pref_bluetoothScan);
                    break;
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                    updateAvailableDevice(device);
                    break;
                case BluetoothDevice.ACTION_NAME_CHANGED:
                    BluetoothDevicePreference pref = findDevicePreference(device);
                    if (pref != null) {
                        pref.updateName();
                    }
                    break;
            }
        }
    }

    /**
     * Receiver to listen for changes in the bluetooth adapter state.
     */
    protected class BluetoothStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                checkBluetoothState();
            }
        }
    }

    /**
     * Callback when a profile state has changed.
     */
    private class ProfileCallback implements BluetoothProfileGroup.ProfileCallback {
        @Override
        public void onProfileChanged(final BluetoothDevice device, int connectionState) {
            final BluetoothDevicePreference pref = findOrAllocateDevicePreference(device);
            pref.updateProfileConnectionState(connectionState);
        }
    }

    /**
     * Looks for a preference in the preference group.
     *
     * Returns null if no preference available.
     */
    private BluetoothDevicePreference findDevicePreference(final BluetoothDevice device) {
        return (BluetoothDevicePreference) findPreference(
                BluetoothDevicePreference.deviceToPreferenceKey(device));
    }

    /**
     * Looks for a preference in the preference group.
     *
     * Allocates a new preference if none found.
     */
    private BluetoothDevicePreference findOrAllocateDevicePreference(final BluetoothDevice device) {
        BluetoothDevicePreference pref = findDevicePreference(device);
        if (pref == null) {
            pref = new BluetoothDevicePreference(getContext(), device, mBluetoothProfileGroup);
        }
        return pref;
    }
}
