package com.google.android.clockwork.settings.keyguard;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.AcceptDenyDialog;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.internal.widget.LockPatternUtils;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.common.systemui.SystemUIContract;
import com.google.android.clockwork.keyguard.KeyguardValidator;

/**
 * Requests the user to enter their pin or password and compares it against the existing password
 * for the device.
 */
public class RetrieveLockPassword extends WearableActivity {

    private LockPatternUtils mLockPatternUtils;
    private Toast mToast;
    private KeyguardValidator mKeyguardValidator;
    private Dialog mDialog;
    private PasswordEntryHelper mPasswordEntryHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLockPatternUtils = new LockPatternUtils(this);
        setUpPasswordEntry();
        mDialog = createVerifyDialog();
        mDialog.show();
        mKeyguardValidator = new KeyguardValidator(mLockPatternUtils, false, false);
    }

    @Override
    protected void onPause() {
        if (mDialog != null) {
            mDialog.dismiss();
        }
        super.onPause();
    }

    private Dialog createVerifyDialog() {
        AcceptDenyDialog verifyDialog =
                new KeyboardHidingAcceptDenyDialog(this, mPasswordEntryHelper);

        verifyDialog.addContentView(mPasswordEntryHelper.getPasswordEntryView(),
                new LinearLayout.LayoutParams(1, 0));
        verifyDialog.setMessage(getResources().getString(isAlphaMode() ?
                R.string.verify_screen_lock_password : R.string.verify_screen_lock_pin));
        verifyDialog.setNegativeButton((dialog, which) -> dialog.dismiss());
        // This click listener does nothing. This is just required so the AcceptDenyDialog
        // shows the positive button. The real logic is in the button OnClickListener below.
        verifyDialog.setPositiveButton((dialog, which) -> {
        });
        verifyDialog.setOnDismissListener((dialog) -> {
            mPasswordEntryHelper.hideKeyboard();
            cancelToast();
            finish();
        });


        ImageButton button = verifyDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        button.setImageResource(R.drawable.ic_settings_next);
        button.setBackgroundResource(R.drawable.ic_settings_next_bg);
        // This is the real logic for the positive button.
        button.setOnClickListener(v -> {
            mPasswordEntryHelper.clearText();
            mPasswordEntryHelper.showKeyboard();
        });

        return verifyDialog;
    }

    private void setUpPasswordEntry() {
        mPasswordEntryHelper = new PasswordEntryHelper(this, isAlphaMode());
        mPasswordEntryHelper.setHint(isAlphaMode() ? R.string.keyboard_password_hint
                : R.string.screen_lock_pin_hint);
        mPasswordEntryHelper.setOnEditorActionListener((passwordEntry, actionId, event) -> {
            boolean isKeyboardEnterKey = event != null
                    && KeyEvent.isConfirmKey(event.getKeyCode())
                    && event.getAction() == KeyEvent.ACTION_DOWN;
            if (actionId != EditorInfo.IME_ACTION_DONE && !isKeyboardEnterKey) {
                return false;
            }

            String passwordText = passwordEntry.getText().toString();
            mKeyguardValidator.validatePassword(
                    passwordText,
                    ActivityManager.getCurrentUser(),
                    new ValidationCallback(passwordText));
            return true;
        });
    }

    /**
     * @return whether alphabetic/alphanumeric keyboard is required.
     */
    private boolean isAlphaMode() {
        int quality = mLockPatternUtils.getActivePasswordQuality(UserHandle.myUserId());
        return quality >= DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC;
    }

    private void showToast(String msg) {
        cancelToast();

        mToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        mToast.show();
    }

    private void cancelToast() {
        if (mToast != null) {
            mToast.cancel();
        }
    }

    private class ValidationCallback implements KeyguardValidator.Callback {
        private final String mPasswordText;

        public ValidationCallback(String passwordText) {
            mPasswordText = passwordText;
        }

        @Override
        public void onMatched() {
            Intent intent = new Intent();
            intent.putExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD,
                    mPasswordText);

            Intent unlockIntent = new Intent(SystemUIContract.ACTION_HIDE_KEYGUARD);
            sendBroadcast(unlockIntent);

            setResult(Activity.RESULT_OK, intent);
            mPasswordEntryHelper.hideKeyboard();
            finish();
        }

        @Override
        public void onRejected(int reason, long lockoutDeadlineMs) {
            boolean tooManyAttempts = lockoutDeadlineMs > 0;
            int noMatchMessageId =
                    isAlphaMode()
                            ? R.string.screen_lock_password_no_match
                            : R.string.screen_lock_pin_no_match;
            int messageId = tooManyAttempts
                    ? R.string.screen_lock_too_many_attempts
                    : noMatchMessageId;
            showToast(getString(messageId));
            mPasswordEntryHelper.clearText();

            if (tooManyAttempts) {
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        }
    }
}
