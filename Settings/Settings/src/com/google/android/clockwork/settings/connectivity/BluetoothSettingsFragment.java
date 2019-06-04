package com.google.android.clockwork.settings.connectivity;

import static android.os.UserManager.DISALLOW_CONFIG_BLUETOOTH;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.util.Log;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.phone.common.Constants;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.SettingsIntents;
import com.google.android.clockwork.settings.cellular.Utils;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;
import com.google.android.clockwork.settings.connectivity.bluetooth.BluetoothDevicePreference;
import com.google.android.clockwork.settings.connectivity.bluetooth.BluetoothProfileGroup;
import com.google.android.clockwork.settings.utils.DefaultBluetoothModeManager;
import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;

/**
 * Bluetooth devices settings.
 */
public class BluetoothSettingsFragment extends SettingsPreferenceFragment {
    private static final String TAG = "BluetoothSettings";

    private static final String BLUETOOTH_SETTINGS_PREF_KEY = "cw_bt_settings_pref";
    private static final String KEY_PREF_BLUETOOTH_ENABLED = "pref_bluetoothEnabled";
    private static final String KEY_PREF_BLUETOOTH_HFP = "pref_bluetoothHfp";
    private static final String KEY_PREF_BLUETOOTH_SCAN = "pref_bluetoothScan";

    private static final int BLUETOOTH_MAJOR_GROUP_UNSPECIFIED = -1;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothProfileGroup mBluetoothProfileGroup;

    private final List<Preference> mBondedDevices = new ArrayList<Preference>();
    private SwitchPreference mBluetoothSwitch;

    private ContentObserver mHfpObserver;
    private ContentResolver mContentResolver;

    private int mBluetoothMode;

    private boolean mScanReceiverRegistered;

    private SettingsProfileListener mSettingsProfileListener = new SettingsProfileListener();

    /* package */ static int PREFERENCE_ORDER_NORMAL = 100;
    /* package */ static int PREFERENCE_ORDER_IMPORTANT = 10;

    private boolean mIsAltMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final StrictMode.ThreadPolicy oldPolicy = StrictMode.allowThreadDiskReads();
        try {
            addPreferencesFromResource(R.xml.prefs_bluetooth);
            final int bluetoothMode
                    = DefaultBluetoothModeManager.INSTANCE.get(getContext()).getBluetoothMode();
            mIsAltMode = (bluetoothMode == SettingsContract.BLUETOOTH_MODE_ALT);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Bluetooth alt mode:" + mIsAltMode);
            }
        } finally {
            StrictMode.setThreadPolicy(oldPolicy);
        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothProfileGroup
                = new BluetoothProfileGroup(getContext(), mBluetoothAdapter, new ProfileCallback());

        mContentResolver = getContext().getContentResolver();

        mBluetoothSwitch = (SwitchPreference) findPreference(KEY_PREF_BLUETOOTH_ENABLED);

        mBluetoothMode = SettingsContract.getIntValueForKey(
                getContext().getContentResolver(),
                SettingsContract.BLUETOOTH_MODE_URI,
                SettingsContract.KEY_BLUETOOTH_MODE,
                SettingsContract.BLUETOOTH_MODE_UNKNOWN);

        initBluetoothSwitchAndDevices(mBluetoothSwitch);
        initHfp((SwitchPreference) findPreference(KEY_PREF_BLUETOOTH_HFP));

        registerStateReceiver();
    }

    /**
     * Intercepts clicks on disallowed preferences, and brings up status-message dialog.
     * OnPreferenceClickListener() doesn't bypass preference click actions, so this is
     * necessary.
     */
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        String key = preference.getKey();
        if (KEY_PREF_BLUETOOTH_SCAN.equals(key)) {
            final EnforcedAdmin disallowConfigBluetoothAdmin =
                RestrictedLockUtils.checkIfRestrictionEnforced(getActivity(),
                        DISALLOW_CONFIG_BLUETOOTH, UserHandle.myUserId());
            if (disallowConfigBluetoothAdmin != null) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getActivity(),
                    disallowConfigBluetoothAdmin);
                return true;
            }
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateBluetoothSwitchAndDevices(mBluetoothSwitch);
        mBluetoothProfileGroup.onResume(mSettingsProfileListener);
    }

    @Override
    public void onPause() {
        mBluetoothProfileGroup.onPause();
        unregisterScanReceiver();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mHfpObserver != null) {
            mContentResolver.unregisterContentObserver(mHfpObserver);
            mHfpObserver = null;
        }
        mBluetoothProfileGroup.onDestroy();
        unregisterStateReceiver();
        super.onDestroy();
    }

    protected void initBluetoothSwitchAndDevices(final SwitchPreference pref) {
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            if ((Boolean) newVal) {
                mBluetoothAdapter.enable();
                Settings.System.putInt(getContext().getContentResolver(),
                        BLUETOOTH_SETTINGS_PREF_KEY, 1);
            } else {
                mBluetoothAdapter.disable();
                Settings.System.putInt(getContext().getContentResolver(),
                        BLUETOOTH_SETTINGS_PREF_KEY, 0);
            }

            pref.setEnabled(false);
            return true;
        });
    }

    protected void updateBluetoothSwitchAndDevices(SwitchPreference pref) {
        switch (mBluetoothAdapter.getState()) {
            case BluetoothAdapter.STATE_OFF:
                pref.setChecked(false);
                pref.setEnabled(true);
                unregisterScanReceiver();
                clearBondedDevices();
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                pref.setChecked(false);
                pref.setEnabled(false);
                clearBondedDevices();
                break;
            case BluetoothAdapter.STATE_ON:
                pref.setChecked(true);
                pref.setEnabled(true);
                registerScanReceiver();
                updateBondedDevices();
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
                pref.setChecked(true);
                pref.setEnabled(false);
                clearBondedDevices();
                break;
        }
    }

    protected void initHfp(final SwitchPreference pref) {
        if (!getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUDIO_OUTPUT)
                || mBluetoothMode == SettingsContract.BLUETOOTH_MODE_ALT
                || Utils.isCallTwinningEnabled(getContext())) {
            getPreferenceScreen().removePreference(pref);
            return;
        }

        mHfpObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                updateHfp(pref);
            }
        };
        mContentResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.BLUETOOTH_DISABLED_PROFILES), false,
                mHfpObserver);
        mContentResolver.registerContentObserver(
                Settings.Global.getUriFor(Constants.GLOBAL_CALL_TWINNING_STATE_KEY),
                false,
                mHfpObserver);

        updateHfp(pref);

        pref.setOnPreferenceChangeListener((p, newVal) -> {
            boolean checked = (Boolean) newVal;
            Intent intent = SettingsIntents.getEnableHFPIntent(checked, true);
            // Without the flag the broadcast can take 12 seconds to be received by
            // HFPBroadcastReceiver. See: b/31976064
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            getContext().sendBroadcastAsUser(intent, UserHandle.CURRENT_OR_SELF);
            pref.setEnabled(false);
            return true;
        });
    }

    protected void updateHfp(final SwitchPreference pref) {
        final long disabledProfileSetting = Settings.Global.getLong(
                mContentResolver, Settings.Global.BLUETOOTH_DISABLED_PROFILES, 0);
        final long headsetClientBit = 1 << BluetoothProfile.HEADSET_CLIENT;
        pref.setChecked((disabledProfileSetting & headsetClientBit) == 0);
        pref.setEnabled(true);
    }

    protected void updateBondedDevices() {
        for (final BluetoothDevice device : mBluetoothAdapter.getBondedDevices()) {
            updatePreferenceBondState(device);
        }
    }

    /**
     * Examine the bond state of the device and update preference if necessary.
     */
    protected void updatePreferenceBondState(final BluetoothDevice device) {
        final BluetoothDevicePreference pref = findOrAllocateDevicePreference(device);
        int connectionState = mBluetoothProfileGroup.getConnectionStatus(device);
        int bluetoothMajorDeviceClass = BLUETOOTH_MAJOR_GROUP_UNSPECIFIED;
        final BluetoothClass bluetoothClass = device.getBluetoothClass();
        if (bluetoothClass != null) {
            bluetoothMajorDeviceClass = bluetoothClass.getMajorDeviceClass();
        }
        reallyUpdatePreferenceBondState(device, device.getBondState(), bluetoothMajorDeviceClass,
                pref, connectionState);
    }

    @VisibleForTesting
    void reallyUpdatePreferenceBondState(final BluetoothDevice device,
                int bondState, int bluetoothMajorDeviceClass, final BluetoothDevicePreference pref,
                int connectionState) {
        pref.updateBondState();
        pref.updateProfileConnectionState(connectionState);
        switch (bondState) {
            case BluetoothDevice.BOND_BONDED:
                int sortOrder = PREFERENCE_ORDER_NORMAL;
                if (bluetoothMajorDeviceClass == BluetoothClass.Device.Major.PHONE) {
                    // List the bonded phone first
                    sortOrder = PREFERENCE_ORDER_IMPORTANT;
                    // disable the capability to alter the bluetooth state to a phone from this end.
                    pref.setSelectable(false);
                    pref.setIcon(R.drawable.ic_phone_nobg);
                } else if (bluetoothMajorDeviceClass == BluetoothClass.Device.Major.UNCATEGORIZED
                         && mIsAltMode) {
                    sortOrder = PREFERENCE_ORDER_IMPORTANT;
                    // disable the capability to alter the bluetooth state to a phone from this end.
                    pref.setSelectable(false);
                    pref.setIcon(R.drawable.ic_phone_nobg);
                }
                pref.setEnabled(true);
                pref.setOrder(sortOrder);
                mBondedDevices.add(pref);
                addPreference(pref);
                break;
            case BluetoothDevice.BOND_NONE:
                pref.setEnabled(false);
                mBondedDevices.remove(pref);
                removePreference(pref);
                break;
            case BluetoothDevice.BOND_BONDING:
                pref.setEnabled(false);
                break;
        }
    }

    private void addPreference(final BluetoothDevicePreference pref) {
        if (getPreferenceManager() != null) {
            getPreferenceScreen().addPreference(pref);
        }
    }

    private void removePreference(final BluetoothDevicePreference pref) {
        if (getPreferenceManager() != null) {
            getPreferenceScreen().removePreference(pref);
        }
    }

    protected void clearBondedDevices() {
        mBondedDevices.forEach((p) -> {
            getPreferenceScreen().removePreference(p);
        });
        mBondedDevices.clear();
    }

    private void registerScanReceiver() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        getContext().registerReceiver(mBluetoothScanReceiver, intentFilter);
        mScanReceiverRegistered = true;
    }

    private void unregisterScanReceiver() {
        if (mScanReceiverRegistered) {
            getContext().unregisterReceiver(mBluetoothScanReceiver);
            mScanReceiverRegistered = false;
        }
    }

    private void registerStateReceiver() {
        getContext().registerReceiver(
                mBluetoothStateReceiver,
                new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    private void unregisterStateReceiver() {
        getContext().unregisterReceiver(mBluetoothStateReceiver);
    }

    /**
     * Called when a profile connects or disconnects.
     */
    private final class SettingsProfileListener implements BtProfile.ProfileStateChangeListener {
        @Override
        public void onProfileStateChanged() {
            for (final BluetoothDevice device : mBluetoothAdapter.getBondedDevices()) {
                final BluetoothDevicePreference pref = findDevicePreference(device);
                if (pref != null) {
                    pref.updateProfileConnectionState(mBluetoothProfileGroup.getConnectionStatus(device));
                }
            }
        }
    }

    /**
     * Handles bluetooth scan responses and other indicators.
     */
    private final BroadcastReceiver mBluetoothScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (getContext() == null) {
                Log.w(TAG, "BluetoothScanReceiver got intent with no context");
                return;
            }
            final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            final String action = intent.getAction();
            switch (action == null ? "" : action) {
                case ConnectivityManager.CONNECTIVITY_ACTION:
                    updateBondedDevices();
                    break;
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                    updatePreferenceBondState(device);
                    break;
                case BluetoothDevice.ACTION_NAME_CHANGED:
                    BluetoothDevicePreference pref = findDevicePreference(device);
                    if (pref != null) {
                        pref.updateName();
                    }
                    break;
            }
        }
    };

    /**
     * Receiver to listen for changes in the bluetooth adapter state.
     */
    private final BroadcastReceiver mBluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (getContext() == null) {
                Log.w(TAG, "BluetoothStateReceiver got intent with no context");
                return;
            }
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                updateBluetoothSwitchAndDevices(mBluetoothSwitch);
            }
        }
    };

    /**
     * Callback when a profile state has changed.
     */
    private class ProfileCallback implements BluetoothProfileGroup.ProfileCallback {
        @Override
        public void onProfileChanged(final BluetoothDevice device, int connectionState) {
            updatePreferenceBondState(device);
        }
    }

    /**
     * Looks for a preference in the preference group.
     *
     * Returns null if no preference found.
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
            pref = allocateBluetoothDevicePreference(device);
        }
        return pref;
    }

    private BluetoothDevicePreference allocateBluetoothDevicePreference(BluetoothDevice device) {
        return new BluetoothDevicePreference(getContext(), device, mBluetoothProfileGroup);
    }

    /**
     * Returns whether the device has current network connection via BT proxy.
     */
    private boolean isProxyConnected() {
        final ConnectivityManager connectivityManager
                = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null) {
            return false;
        } else {
            return (networkInfo.getType() == ConnectivityManager.TYPE_PROXY)
                    && networkInfo.isConnected();
        }
    }
}
