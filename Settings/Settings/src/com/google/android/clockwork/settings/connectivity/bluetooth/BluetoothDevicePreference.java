package com.google.android.clockwork.settings.connectivity.bluetooth;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.wearable.preference.WearableDialogPreference;
import android.text.TextUtils;
import android.util.Log;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.common.SettingsPreferenceLogConstants;

/**
 * Preference class representing a single bluetooth device.
 */
public class BluetoothDevicePreference extends WearableDialogPreference {
    private static final String TAG = "BluetoothDevicePreference";

    private final BluetoothDevice mDevice;
    private final BluetoothProfileGroup mBluetoothProfileGroup;

    private int mWhichButtonClicked;
    private int mConnectionState;
    private String mNameFormat;
    private boolean mHideSummary;

    public BluetoothDevicePreference(Context context, final BluetoothDevice device,
            final BluetoothProfileGroup bluetoothProfileGroup) {
        super(context);
        mDevice = device;
        mBluetoothProfileGroup = bluetoothProfileGroup;

        setKey(BluetoothDevicePreference.deviceToPreferenceKey(device));
        setIcon(R.drawable.ic_cc_settings_bluetooth);
        setNegativeButtonText(R.string.cancel);

        updateName();
        updateBondState();
        updateClass();
    }

    public static String deviceToPreferenceKey(@NonNull BluetoothDevice device) {
        return SettingsPreferenceLogConstants.IGNORE_SUBSTRING + device.getAddress();
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        // No need to try to connect to devices when we don't support any profiles
        // on the target device.
        if (!mBluetoothProfileGroup.isAnyProfileSupported(mDevice)) {
            builder.setPositiveButton(R.string.pref_bluetooth_unavailable, this);
        } else {
            mConnectionState = mBluetoothProfileGroup.getConnectionStatus(mDevice);
            builder.setPositiveButton(
                    mConnectionState == BluetoothProfile.STATE_CONNECTED
                    || mConnectionState == BluetoothProfile.STATE_CONNECTING
                    ? R.string.pref_bluetooth_disconnect : R.string.pref_bluetooth_connect,
                    this);
        }
        builder.setNeutralButton(R.string.pref_bluetooth_forget, this);
    }

    /**
     * Present when the device is available
     */
    @Override
    protected void onClick() {
        if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
            super.onClick();
        } else {
            // Discovery may be in progress so cancel discovery before
            // attempting to bond.
            stopDiscovery();
            mDevice.createBond();
        }
    }

    /**
     * Present when the device requires a dialog
     */
    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);
        mWhichButtonClicked = which;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        switch (mWhichButtonClicked) {
            case DialogInterface.BUTTON_POSITIVE:
                if (mConnectionState == BluetoothProfile.STATE_CONNECTED
                        || mConnectionState == BluetoothProfile.STATE_CONNECTING) {
                    mBluetoothProfileGroup.disconnectDevice(mDevice);
                } else {
                    mBluetoothProfileGroup.connectDevice(mDevice);
                }
                break;
            case DialogInterface.BUTTON_NEUTRAL:
                requestUnpair(mDevice);
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                break; // do nothing, dismiss
        }
    }

    /**
     * Request to unpair and remove the bond
     */
    protected void requestUnpair(BluetoothDevice device) {
        final int state = device.getBondState();

        if (state == BluetoothDevice.BOND_BONDING) {
            device.cancelBondProcess();
        }

        if (state != BluetoothDevice.BOND_NONE) {
            if (!device.removeBond()) {
                Log.w(TAG, "Unpair request rejected straight away.");
            }
        }
    }

    private void stopDiscovery() {
        final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            if (adapter.isDiscovering()) {
                adapter.cancelDiscovery();
            }
        }
    }

    public void updateName() {
        String name = mDevice.getName();
        if (TextUtils.isEmpty(name)) {
            name = mDevice.getAddress();
        }

        String format = mNameFormat;
        if (format != null) {
            setTitle(String.format(format, name));
        } else {
            setTitle(name);
        }

        setDialogTitle(name);
        notifyChanged();
    }

    /**
     * Re-examine the device and update the bond state.
     */
    public void updateBondState() {
        switch (mDevice.getBondState()) {
            case BluetoothDevice.BOND_BONDED:
                setSummary(null);
                break;
            case BluetoothDevice.BOND_BONDING:
                setSummary(R.string.bluetooth_pairing);
                break;
            case BluetoothDevice.BOND_NONE:
                setSummary(R.string.pref_bluetooth_available);
                break;
        }
        notifyChanged();
    }

    public void updateClass() {
        if (mDevice.getBluetoothClass() == null) {
            return;
        }

        switch (mDevice.getBluetoothClass().getDeviceClass()) {
            case BluetoothClass.Device.PHONE_CELLULAR:
            case BluetoothClass.Device.PHONE_SMART:
            case BluetoothClass.Device.PHONE_UNCATEGORIZED:
                setIcon(R.drawable.ic_phone);
                break;

            case BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES:
            case BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET:
            case BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER:
                setIcon(R.drawable.ic_settings_headset);
                break;

            case BluetoothClass.Device.WEARABLE_WRIST_WATCH:
                setIcon(R.drawable.ic_settings_device_only);
                break;

            case BluetoothClass.Device.WEARABLE_GLASSES:
                setIcon(R.drawable.ic_glass_device_white);
                break;
        }
        notifyChanged();
    }

    /**
     * Update the preference summary with the profile connection state
     *
     * However, if no profiles are supported from the target device
     * we indicate that this target device is unavailable.
     */
    public void updateProfileConnectionState(int connectionState) {
        if (mHideSummary) {
            setSummary(null);
        } else if (mBluetoothProfileGroup != null
                && !mBluetoothProfileGroup.isAnyProfileSupported(mDevice)) {
            // If the device is a phone it is likely the primary paired
            // phone so skip indicating it is unavailable.
            if (mDevice.getBluetoothClass() != null
                    && mDevice.getBluetoothClass().getMajorDeviceClass()
                            != BluetoothClass.Device.Major.PHONE) {
                setSummary(R.string.pref_bluetooth_unavailable);
            } else {
                setSummary(null);
            }
        } else {
            switch (connectionState) {
                case BluetoothProfile.STATE_CONNECTED:
                    setSummary(R.string.bluetooth_connected);
                    break;
                case BluetoothProfile.STATE_CONNECTING:
                    setSummary(R.string.bluetooth_connecting);
                    break;
                case BluetoothProfile.STATE_DISCONNECTING:
                    setSummary(R.string.bluetooth_disconnecting);
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    setSummary(R.string.bluetooth_disconnected);
                    break;
            }
        }
        notifyChanged();
    }

    /**
     *  Set a format for the primary phone with which the watch is paired.
     *
     * @param  string The string to format the displayed paired phone name.
     */
    public void setNameFormat(final String format) {
        mNameFormat = format;
        updateName();
    }

    public void setHideSummary(boolean value) {
        mHideSummary = value;
    }
}
