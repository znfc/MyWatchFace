package com.google.android.clockwork.settings.connectivity.bluetooth;

import android.annotation.StyleRes;
import android.content.Context;
import android.content.Intent;
import android.bluetooth.BluetoothDevice;
import android.support.wearable.view.AcceptDenyDialog;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.TextAppearanceSpan;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.apps.wearable.settings.R;

public class BluetoothPairingDialog extends AcceptDenyDialog {
    private static final String TAG = "BluetoothPairing";
    private static final int FIRST_LINE_CHAR_LIMIT = 18;

    private BluetoothDevice mDevice;

    public BluetoothPairingDialog(Context context) {
        super(context);
    }

    public BluetoothPairingDialog(Context context, @StyleRes int themeResId) {
        super(context, themeResId);
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }

    public boolean setDevice(Intent intent) {
        return setDevice((BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE));
    }

    public boolean setDevice(BluetoothDevice device) {
        mDevice = device;
        if (device == null) {
            Log.e(TAG, "Device cannot be null.");
            return false;
        }
        // Set the first line of the dialog to: "Pair with <deviceName>".
        String deviceName = TextUtils.isEmpty(device.getName())
                ? device.getAddress() : device.getName();
        String pairWith = getContext().getString(R.string.bluetooth_pair_with, deviceName);
        setTitle(pairWith.length() > FIRST_LINE_CHAR_LIMIT
                ? getContext().getString(R.string.bluetooth_pair_with_short, deviceName)
                : pairWith);
        return true;
    }

    public void setPairingMessage(CharSequence pairingInstruction, CharSequence pairingCode) {
        setMessage(new SpannableStringBuilder(pairingInstruction)
                .append('\n')
                .append(pairingCode,
                        new TextAppearanceSpan(
                                getContext(),
                                android.R.style.TextAppearance_Material_Headline),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE));
    }
}
