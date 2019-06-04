package com.google.android.clockwork.settings.cellular;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.Vibrator;
import android.telephony.SubscriptionManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.support.wearable.activity.ConfirmationActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.PhoneConstants;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.cellular.views.PinPadView;
import com.google.android.clockwork.settings.cellular.views.PinPadView.PinPadListener;

public class SimUnlockActivity extends Activity {
    private static final String TAG = SimUnlockActivity.class.getSimpleName();

    private SubscriptionManager mSubscriptionManager;
    private Vibrator mVibrator;
    private String mPuk = "";
    private String mPin = "";
    private String mLastPin = "";
    private TextView mPinEdit;
    private TextView mErrorLabel;
    private boolean mIsPukPin;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sim_pin_pad_activity);
        mSubscriptionManager = SubscriptionManager.from(this);
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mPinEdit = (TextView) findViewById(R.id.pin_edit);
        mErrorLabel = (TextView) findViewById(R.id.error_label);
        final PinPadView pinPadView = (PinPadView) findViewById(R.id.pin_pad_view);
        pinPadView.registerListener(mPinPadListener);

        mIsPukPin = getIntent().getBooleanExtra(Constants.EXTRA_IS_PUK_PIN, false);

        mPinEdit.setHint(
            mIsPukPin ? R.string.sim_pin_edit_puk_hint : R.string.sim_pin_edit_hint);
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
        boolean needPukPin = mIsPukPin && mPuk.isEmpty();
        boolean needRepeatPin = mIsPukPin && mLastPin.isEmpty();

        if (!isReasonablePin(mPin, needPukPin)) {
            showWarning(getString(needPukPin
                ? R.string.sim_unlock_wrong_length_puk : R.string.sim_unlock_wrong_length));
        } else {
            // When unlock with PUK, first type PUK pin.
            if (mIsPukPin && needPukPin) {
                mPuk = mPin;
                mPin = "";
                mPinEdit.setText(mPin);
                mPinEdit.setHint(R.string.sim_pin_edit_new_hint);
            // When unlock with PUK, need user type new SIM pin.
            } else if (mIsPukPin && needRepeatPin) {
                mLastPin = mPin;
                mPin = "";
                mPinEdit.setText(mPin);
                mPinEdit.setHint(R.string.sim_pin_edit_repeat_hint);
            // When unlock with PUK, need user type new SIM pin, require repeat to double check.
            } else if (mIsPukPin && !mLastPin.equals(mPin)) {
                mLastPin = "";
                mPin = "";
                mPinEdit.setText(mPin);
                mPinEdit.setHint(R.string.sim_pin_edit_new_hint);
                showWarning(getString(R.string.sim_pin_edit_repeat_not_match));
            // When unlock with PUK, try to unlock after PUK and SIM pin both ready.
            } else {
                int pinResult = PhoneConstants.PIN_GENERAL_FAILURE;
                int attemptsRemaining = -1;

                try {
                    final int subId = mSubscriptionManager.getDefaultSubscriptionId();

                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "call supplyPinReportResultForSubscriber(subid=" + subId + ")");
                    }
                    final int[] result =
                        mIsPukPin
                        ? ITelephony.Stub.asInterface(ServiceManager.checkService("phone"))
                            .supplyPukReportResultForSubscriber(subId, mPuk, mPin)
                        : ITelephony.Stub.asInterface(ServiceManager.checkService("phone"))
                            .supplyPinReportResultForSubscriber(subId, mPin);
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "supplyPinReportResult returned: " + result[0] + " " + result[1]);
                    }
                    pinResult = result[0];
                    attemptsRemaining = result[1];
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException for supplyPinReportResult:", e);
                    pinResult = PhoneConstants.PIN_GENERAL_FAILURE;
                    attemptsRemaining = -1;
                }

                // To test error case, uncomment following:
                // pinResult = PhoneConstants.PIN_PASSWORD_INCORRECT;
                // attemptsRemaining = 2;
                simUnlockChanged(pinResult, attemptsRemaining);
            }
        }
    }

    private void simUnlockChanged(int pinResult, int attemptsRemaining) {
        if (pinResult == PhoneConstants.PIN_RESULT_SUCCESS) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "PIN result success!!!");
            }

            SimStateNotification.cancelNotification(this);

            final Intent intent = new Intent(this, ConfirmationActivity.class);
            intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                ConfirmationActivity.SUCCESS_ANIMATION);
            intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE,
                getString(mIsPukPin ? R.string.puk_unlock_success : R.string.sim_unlock_success));
            startActivity(intent);
            finish();
        } else if (pinResult == PhoneConstants.PIN_PASSWORD_INCORRECT) {
            if (attemptsRemaining > 0) {
                // Clear the PIN string and widget.
                mLastPin = "";
                mPuk = "";
                mPin = "";
                mPinEdit.setText(mPin);
                mPinEdit.setHint(
                    mIsPukPin ? R.string.sim_pin_edit_puk_hint :R.string.sim_pin_edit_hint);
                Spannable span = new SpannableString(getResources().getQuantityString(
                        R.plurals.sim_unlock_attempts_remaining, attemptsRemaining,
                        attemptsRemaining));
                mErrorLabel.setText(span);
                mErrorLabel.setVisibility(View.VISIBLE);
                mPinEdit.setVisibility(View.GONE);
            } else if (attemptsRemaining == 0) {
                final Intent intent = new Intent(this, SimUnlockFailure.class);
                intent.putExtra(Constants.EXTRA_IS_PUK_PIN, mIsPukPin);
                startActivity(intent);
                finish();
            } else {
                // supplyPinReportResultForSubscriber error?
                Log.e(TAG, "PIN_PASSWORD_INCORRECT but attemptsRemaining is negative.");
            }
            showWarning(getString(
                mIsPukPin ? R.string.sim_unlock_wrong_code_puk : R.string.sim_unlock_wrong_code));
        } else {
            // RemoteException
            Log.e(TAG, "RemoteException calling supplyPinReportResultForSubscriber.");
            showWarning(getString(R.string.sim_unlock_wrong_code));
        }
    }

    private void showWarning(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
        mVibrator.vibrate(300);
    }

    private boolean isReasonablePin(String pin, boolean pukPin) {
        if (pin == null) {
            return false;
        } else if (pukPin) {
            return pin.length() == Constants.PUK_PIN_LENGTH;
        } else {
            return pin.length() >= Constants.MIN_SIM_PIN_LENGTH
                    && pin.length() <= Constants.MAX_SIM_PIN_LENGTH;
        }
    }
}
