package com.google.android.clockwork.settings.connectivity.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.views.ViewUtils;

/**
 * Enables the user to enter a PIN for pairing with a remote bluetooth device.
 *
 * <p>Its UI consists of a PIN field and a simple numeric keyboard. It returns the entered PIN as
 * a result.
 */
public class BluetoothEnterPinActivity extends WearableActivity
        implements BluetoothPairingCancellationReceiver.PairingCancelledListener {

    public static final int MIN_PIN_LENGTH = 1;
    public static final int MAX_PIN_LENGTH = 16;

    private static final String TAG = BluetoothEnterPinActivity.class.getSimpleName();

    private final View.OnClickListener mBackButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            CharSequence text = mPinView.getText();
            if (text.length() > 0) {
                mPinView.setText(text.subSequence(0, text.length() - 1));
            }
        }
    };

    private final View.OnClickListener mOKButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            byte[] pinBytes = BluetoothDevice.convertPinToBytes(mPinView.getText().toString());
            mDevice.setPin(pinBytes);
            mCancelOnExit = false;
            finish();
        }
    };

    /**
     * Specifies whether the activity should cancel pairing once it finishes.
     *
     * <p>Pairing must be cancelled if it didn't finish successfully and if it wasn't cancelled by
     * the remote device.
     */
    private boolean mCancelOnExit = true;

    private BluetoothDevice mDevice;
    private BluetoothPairingCancellationReceiver mCancellationReceiver;
    private LinearLayout mKeyboardLayout;
    private TextView mPinView;
    private Typeface mTypeface;
    private ImageButton mOKButton;

    @Override
    public void onPairingCancelled() {
        mCancelOnExit = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDevice = getIntent().getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (mDevice == null) {
            Log.e(TAG, "Device cannot be null.");
            finish();
        }

        mCancellationReceiver = new BluetoothPairingCancellationReceiver(this, this);
        mCancellationReceiver.register();

        setContentView(R.layout.bluetooth_enter_pin_dialog);
        mKeyboardLayout = (LinearLayout) findViewById(R.id.keyboard);
        mPinView = (TextView) findViewById(R.id.pin);
        mTypeface = mPinView.getTypeface();
        mPinView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updatePinFont();
                int pinLength = mPinView.length();
                mOKButton.setEnabled(MIN_PIN_LENGTH <= pinLength && pinLength <= MAX_PIN_LENGTH);
            }
        });

        if(ViewUtils.isCircular(this)) {
            int padding = (int) getResources().getDimension(
                    R.dimen.bluetooth_pairing_pin_padding_circular);
            mKeyboardLayout.setPadding(padding, 0, padding, padding);
            mPinView.setPadding(padding, padding, padding, 0);
        }

        populateKeyboard();
    }

    @Override
    protected void onDestroy() {
        mCancellationReceiver.unregister();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        if (isFinishing()) {
            maybeCancelPairing();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        maybeCancelPairing();
        finish();
        super.onStop();
    }

    private void maybeCancelPairing() {
        if (mCancelOnExit && mDevice != null) {
            mDevice.cancelPairingUserInput();
            mCancelOnExit = false;
        }
    }

    private void populateKeyboard() {
        LinearLayout row = null;
        for (int i = 1; i <= 9; i++) {
            if (i % 3 == 1) {
                row = (LinearLayout) getLayoutInflater().inflate(
                        R.layout.bluetooth_pin_keyboard_row, mKeyboardLayout, false);
                mKeyboardLayout.addView(row);
            }
            addDigitKey(i, row);
        }
        row = (LinearLayout) getLayoutInflater().inflate(
                R.layout.bluetooth_pin_keyboard_row, mKeyboardLayout, false);
        mKeyboardLayout.addView(row);
        addSpecialKey(R.drawable.ic_settings_pairing_leftarrow, row, mBackButtonListener);
        addDigitKey(0, row);
        mOKButton = addSpecialKey(R.drawable.check_icon, row, mOKButtonListener);
        mOKButton.setEnabled(false);
    }

    private ImageButton addSpecialKey(int iconResourceId, ViewGroup parent,
            View.OnClickListener listener) {
        ImageButton keyView = (ImageButton) getLayoutInflater().inflate(
                R.layout.bluetooth_pin_image_key, parent, false);
        parent.addView(keyView);
        keyView.setImageResource(iconResourceId);
        keyView.setOnClickListener(listener);
        return keyView;
    }

    private void addDigitKey(int digit, ViewGroup parent) {
        TextView keyView = (TextView) getLayoutInflater().inflate(
                R.layout.bluetooth_pin_key, parent, false);
        parent.addView(keyView);
        String digitString = String.format("%d", digit);
        keyView.setText(digitString);
        keyView.setOnClickListener(new OnDigitClickListener(digitString));
    }

    private void updatePinFont() {
        if (mPinView.length() > 0) {
            mPinView.setTypeface(mTypeface, Typeface.BOLD);
        } else {
            mPinView.setTypeface(mTypeface, Typeface.NORMAL);
        }
    }

    private class OnDigitClickListener implements View.OnClickListener {
        private final String mDigit;

        public OnDigitClickListener(String digit) {
            mDigit = digit;
        }

        @Override
        public void onClick(View v) {
            mPinView.append(mDigit);
        }
    }

}
