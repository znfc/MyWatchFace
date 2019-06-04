package com.google.android.clockwork.settings.connectivity.cellular;

import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.AcceptDenyDialog;
import android.support.wearable.view.WearableDialogHelper;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.PhoneFactory;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.companionrelay.Intents;
import com.google.android.clockwork.settings.Constants;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.cellular.PhoneNumberActivity;
import com.google.android.clockwork.settings.cellular.SetNumberService;

/**
 * Activity that handles showing a dialog to handle call forwarding.
 *
 * Different dialogs will be used depending on if a phone number is available.
 * <p>
 * Needs to be its own Activity so we can return to it after phone number is set.
 */
public class CallForwardingActivity extends WearableActivity {
    private boolean mPhoneNumberActivityLaunched;
    private TelephonyManager mTel;

    /**
     * Called when requested cellular data stuctures have changed.
     */
    private ResultReceiver mReceiver = new ResultReceiver(new Handler()) {
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            mPhoneNumberActivityLaunched = false;
            final String number = mTel.getLine1Number();
            updateScreen(number);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTel = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        final String number = mTel.getLine1Number();
        // We are resuming either from initial creation or returning from
        // changing the display phone number via the activity.
        mPhoneNumberActivityLaunched = false;
        updateScreen(number);
    }

   /**
     * On watch call fowarding screen.
     */
    private void updateScreen(final String number) {
        if (number == null || TextUtils.isEmpty(number)) {
            presentEmptyPhoneNumberDialog();
        } else {
            presentFilledPhoneNumberDialog(number);
        }
    }

    private void presentFilledPhoneNumberDialog(final String number) {
        new WearableDialogHelper.DialogBuilder(CallForwardingActivity.this)
                .setPositiveIcon(R.drawable.ic_cc_settings_open_on_phone)
                .setNeutralIcon(R.drawable.ic_settings_device_only)
                .setTitle(R.string.pref_callForwarding)
                .setMessage(getString(R.string.call_forwarding_activity_message,
                        SetNumberService.formatNumber(CallForwardingActivity.this, number)))
                .setPositiveButton(R.string.action_request_activation, (dialog, which) -> {
                    sendTurnOnCallForwarding(number);
                    startActivity(new Intent(this, ConfirmationActivity.class)
                            .putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                                    ConfirmationActivity.SUCCESS_ANIMATION));
                })
                .setNeutralButton(R.string.action_change_number, (dialog, which) -> {
                    launchPhoneNumberActivity();
                })
                .setOnDismissListener((dialog) -> {
                    if (!mPhoneNumberActivityLaunched) {
                        finish();
                    }
                })
                .show();
    }

    private void presentEmptyPhoneNumberDialog() {
        AcceptDenyDialog diag = new AcceptDenyDialog(CallForwardingActivity.this);
        diag.setTitle(getString(R.string.call_forward_empty_watch_number_title));
        diag.setMessage(getString(R.string.call_forward_empty_watch_number_message));
        diag.setPositiveButton((dialog, which) -> {
            launchPhoneNumberActivity();
        });
        diag.setNegativeButton((dialog, which) -> {
            finish();
        });
        diag.setOnDismissListener((dialog) -> {
            if (!mPhoneNumberActivityLaunched) {
                finish();
            }
        });
        diag.show();
    }

    private void sendTurnOnCallForwarding(final String number) {
        Bundle data = new Bundle();
        data.putInt(Constants.FIELD_COMMAND, Constants.RPC_TURN_ON_CALL_FORWARDING);
        data.putString(Constants.FIELD_WATCH_NUMBER, number);
        CallForwardingUtils.setLastRequestedForwardingAction(getApplicationContext(),
                SettingsContract.CALL_FORWARD_ACTION_ON);
        startService(Intents.getRelayRpcIntent(Constants.PATH_RPC_WITH_FEATURE, data));
    }

    private void launchPhoneNumberActivity() {
        mPhoneNumberActivityLaunched = true;
        final String oldNumber = mTel.getLine1Number();
        Intent intent = new Intent(getApplicationContext(), PhoneNumberActivity.class)
            .putExtra(SetNumberService.EXTRA_OLD_NUMBER, oldNumber)
            .putExtra(SetNumberService.EXTRA_RESULT_RECEIVER, mReceiver);
        startActivity(intent);
    }
}
