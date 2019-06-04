package com.google.android.clockwork.settings.cellular;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.os.RemoteException;
import android.os.Vibrator;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.support.wearable.activity.ConfirmationActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.cellular.views.PinPadView;
import com.google.android.clockwork.settings.cellular.views.PinPadView.PinPadListener;

public class SimPinChangeActivity extends Activity {
    private static final String TAG = SimPinChangeActivity.class.getSimpleName();

    // State when entering the old pin
    private static final int ICC_OLD_MODE = 0;
    // State when entering the new pin - first time
    private static final int ICC_NEW_MODE = 1;
    // State when entering the new pin - second time
    private static final int ICC_REENTER_MODE = 2;

    // For async handler to identify request type
    private static final int MSG_CHANGE_ICC_PIN_COMPLETE = 100;

    private Vibrator mVibrator;
    private Phone mPhone;
    private TextView mPinEdit;
    private TextView mErrorLabel;
    private String mPin = "";
    private String mOldPin;
    private String mNewPin;
    private int mState;

    private PinPadListener mPinPadListener = new PinPadListener() {
        @Override
        public void onKeyHover(int keyCode) {
        }

        @Override
        public void onKeyPressed(int keyCode) {
            mPinEdit.setVisibility(View.VISIBLE);
            mErrorLabel.setVisibility(View.GONE);

            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "mPin=" + mPin);
                }

                if (!TextUtils.isEmpty(mPin)) {
                    checkSimPin();
                }
            } else if (keyCode == KeyEvent.KEYCODE_DEL) {
                final int length = mPin.length();
                if (length > 0) {
                    mPin = mPin.substring(0, length - 1);
                    mPinEdit.setText(mPin);
                }
            } else {
                KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
                char c = (char) event.getUnicodeChar();
                mPin = mPin + c;
                mPinEdit.setText(mPin);
            }
        }
    };

    // For replies from IccCard interface
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;
            switch (msg.what) {
                case MSG_CHANGE_ICC_PIN_COMPLETE:
                    iccLockChanged(ar.exception == null, msg.arg1);
                    break;
            }
            return;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sim_pin_pad_activity);
        mVibrator = getSystemService(Vibrator.class);
        mPhone = PhoneFactory.getDefaultPhone();
        mPinEdit = (TextView) findViewById(R.id.pin_edit);
        mErrorLabel = (TextView) findViewById(R.id.error_label);
        final PinPadView pinPadView = (PinPadView) findViewById(R.id.pin_pad_view);
        pinPadView.registerListener(mPinPadListener);
        mPinEdit.setHint(R.string.sim_pin_edit_old_hint);

        mState = ICC_OLD_MODE;
    }

    @Override
    protected void onDestroy() {
        ResultReceiver receiver = getIntent().getParcelableExtra(
                Constants.EXTRA_RESULT_RECEIVER);
        if (receiver != null) {
            receiver.send(Activity.RESULT_OK, null);
        }
        super.onDestroy();
    }

    private void checkSimPin() {
        if (!isReasonablePin(mPin)) {
            showWarning(getString(R.string.sim_unlock_wrong_length));
        } else {
            switch (mState) {
                case ICC_OLD_MODE:
                    mOldPin = mPin;
                    mState = ICC_NEW_MODE;
                    mPin = "";
                    mPinEdit.setText(mPin);
                    mPinEdit.setHint(R.string.sim_pin_edit_new_hint);
                    break;
                case ICC_NEW_MODE:
                    mNewPin = mPin;
                    mState = ICC_REENTER_MODE;
                    mPin = "";
                    mPinEdit.setText(mPin);
                    mPinEdit.setHint(R.string.sim_pin_edit_repeat_hint);
                    break;
                case ICC_REENTER_MODE:
                    if (!mPin.equals(mNewPin)) {
                        mState = ICC_NEW_MODE;
                        mPin = "";
                        mPinEdit.setText(mPin);
                        mPinEdit.setHint(R.string.sim_pin_edit_new_hint);
                        showWarning(getString(R.string.sim_pin_edit_repeat_not_match));
                    } else {
                        Message callback = Message.obtain(mHandler, MSG_CHANGE_ICC_PIN_COMPLETE);
                        mPhone.getIccCard().changeIccLockPassword(mOldPin, mNewPin, callback);
                    }
                    break;
            }
        }
    }

    private void iccLockChanged(boolean success, int attemptsRemaining) {
        Log.d(TAG, "iccLockChanged " + success +" "+attemptsRemaining);
        if (success) {
            final Intent intent = new Intent(this, ConfirmationActivity.class);
            intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                ConfirmationActivity.SUCCESS_ANIMATION);
            intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE,
                getString(R.string.sim_lock_pin_change_success));
            startActivity(intent);
            finish();
        } else if (attemptsRemaining > 0) {
            // Clear the PIN string and widget.
            mState = ICC_OLD_MODE;
            mPin = "";
            mPinEdit.setText(mPin);
            mPinEdit.setHint(R.string.sim_pin_edit_old_hint);

            Spannable span = new SpannableString(getResources().getQuantityString(
                        R.plurals.sim_unlock_attempts_remaining, attemptsRemaining,
                        attemptsRemaining));
            mErrorLabel.setText(span);
            mErrorLabel.setVisibility(View.VISIBLE);
            mPinEdit.setVisibility(View.GONE);

            showWarning(getString(R.string.sim_unlock_wrong_code));
        } else if (attemptsRemaining == 0) {
            final Intent intent = new Intent(this, SimUnlockFailure.class);
            intent.putExtra(Constants.EXTRA_IS_PUK_PIN, false);
            startActivity(intent);
            finish();
        } else {
            // Phone.getIccCard().setIccLockEnabled() callback error?
            Log.e(TAG, "PIN_PASSWORD_INCORRECT but attemptsRemaining is negative.");
        }
    }

    private void showWarning(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
        mVibrator.vibrate(300);
    }

    private boolean isReasonablePin(String pin) {
        if (pin == null || pin.length() < Constants.MIN_SIM_PIN_LENGTH ||
                pin.length() > Constants.MAX_SIM_PIN_LENGTH) {
            return false;
        } else {
            return true;
        }
    }
}
