/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.clockwork.settings.keyguard;

import android.app.Activity;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Trace;
import android.os.UserHandle;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.AcceptDenyDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.common.systemui.SystemUIContract;

import java.util.ArrayList;
import java.util.List;

/**
 * Lets a user create a new lock screen pin or password and verify that it meets device policy
 * requirements.
 *
 * Derived from {@link com.android.settings.password.ChooseLockPassword}.
 */
public class ChooseLockPassword extends WearableActivity {

    private static final String TAG = "ChooseLockPassword";

    public static final String PASSWORD_MIN_KEY = "lockscreen.password_min";
    public static final String PASSWORD_MAX_KEY = "lockscreen.password_max";
    public static final String PASSWORD_MIN_LETTERS_KEY = "lockscreen.password_min_letters";
    public static final String PASSWORD_MIN_LOWERCASE_KEY = "lockscreen.password_min_lowercase";
    public static final String PASSWORD_MIN_UPPERCASE_KEY = "lockscreen.password_min_uppercase";
    public static final String PASSWORD_MIN_NUMERIC_KEY = "lockscreen.password_min_numeric";
    public static final String PASSWORD_MIN_SYMBOLS_KEY = "lockscreen.password_min_symbols";
    public static final String PASSWORD_MIN_NONLETTER_KEY = "lockscreen.password_min_nonletter";
    public static final int DEFAULT_REQUESTED_QUALITY
            = DevicePolicyManager.PASSWORD_QUALITY_NUMERIC;
    public static final int DEFAULT_PASSWORD_MIN_LENGTH = LockPatternUtils.MIN_LOCK_PASSWORD_SIZE;
    public static final int DEFAULT_PASSWORD_MAX_LENGTH = 16;

    private String mCurrentPassword;
    private String mChosenPassword;
    private String mFirstPin;
    private PasswordEntryHelper mPasswordEntryHelper;
    private int mRequestedQuality;
    private boolean mIsAlphaMode;
    private Stage mUiStage = Stage.Introduction;

    private LockPatternUtils mLockPatternUtils;
    private PasswordChecker mPasswordChecker;
    private int mUserId;
    private Toast mToast;
    private Dialog mDialog;

    /**
     * Keep track internally of where the user is in choosing a pattern.
     */
    protected enum Stage {
        // Introduction stage where the user choose a pin/password that meets policy requirements.
        Introduction(R.string.keyboard_password_hint,
                R.string.screen_lock_pin_hint),

        // Ask user to re-enter their pin or pass for confirmation.
        NeedToConfirm(R.string.screen_lock_confirm_hint,
                R.string.screen_lock_confirm_hint);

        Stage(int hintInAlpha, int hintInNumeric) {
            this.alphaHint = hintInAlpha;
            this.numericHint = hintInNumeric;
        }

        public final int alphaHint;
        public final int numericHint;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        mLockPatternUtils = new LockPatternUtils(this);

        mUserId = UserHandle.myUserId();
        mCurrentPassword = intent.getStringExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD);
        mRequestedQuality = Math.max(
                intent.getIntExtra(LockPatternUtils.PASSWORD_TYPE_KEY, DEFAULT_REQUESTED_QUALITY),
                mLockPatternUtils.getRequestedPasswordQuality(mUserId));
        int passwordMinLength = Math.max(
                Math.max(LockPatternUtils.MIN_LOCK_PASSWORD_SIZE,
                        intent.getIntExtra(PASSWORD_MIN_KEY, DEFAULT_PASSWORD_MIN_LENGTH)),
                mLockPatternUtils.getRequestedMinimumPasswordLength(mUserId));
        int passwordMaxLength = intent.getIntExtra(PASSWORD_MAX_KEY, DEFAULT_PASSWORD_MAX_LENGTH);
        int passwordMinLetters = Math.max(intent.getIntExtra(PASSWORD_MIN_LETTERS_KEY, 0),
                mLockPatternUtils.getRequestedPasswordMinimumLetters(mUserId));
        int passwordMinUpperCase = Math.max(
                intent.getIntExtra(PASSWORD_MIN_UPPERCASE_KEY, 0),
                mLockPatternUtils.getRequestedPasswordMinimumUpperCase(mUserId));
        int passwordMinLowerCase = Math.max(intent.getIntExtra(PASSWORD_MIN_LOWERCASE_KEY, 0),
                mLockPatternUtils.getRequestedPasswordMinimumLowerCase(mUserId));
        int passwordMinNumeric = Math.max(intent.getIntExtra(PASSWORD_MIN_NUMERIC_KEY, 0),
                mLockPatternUtils.getRequestedPasswordMinimumNumeric(mUserId));
        int passwordMinSymbols = Math.max(intent.getIntExtra(PASSWORD_MIN_SYMBOLS_KEY, 0),
                mLockPatternUtils.getRequestedPasswordMinimumSymbols(mUserId));
        int passwordMinNonLetter = Math.max(intent.getIntExtra(PASSWORD_MIN_NONLETTER_KEY, 0),
                mLockPatternUtils.getRequestedPasswordMinimumNonLetter(mUserId));

        mIsAlphaMode = DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC == mRequestedQuality
                || DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC == mRequestedQuality
                || DevicePolicyManager.PASSWORD_QUALITY_COMPLEX == mRequestedQuality;

        mPasswordChecker = new PasswordChecker(
                mLockPatternUtils,
                passwordMinLength,
                passwordMaxLength,
                passwordMinLetters,
                passwordMinUpperCase,
                passwordMinLowerCase,
                passwordMinSymbols,
                passwordMinNumeric,
                passwordMinNonLetter,
                mRequestedQuality,
                mIsAlphaMode,
                mUserId
        );

        setUpPasswordEntry();
        mDialog = createChooseDialog();
        mDialog.show();
    }

    private Dialog createChooseDialog() {
        AcceptDenyDialog chooseDialog =
                new KeyboardHidingAcceptDenyDialog(this, mPasswordEntryHelper);
        chooseDialog.setMessage(getResources().getString(mIsAlphaMode ?
                R.string.choose_screen_lock_password : R.string.choose_screen_lock_pin));
        chooseDialog.setNegativeButton((dialog, which) -> dialog.dismiss());
        // The listener below does nothing. This is just required so the AcceptDenyDialog
        // shows the positive button. The real logic is in the button.setOnClickListener below.
        chooseDialog.setPositiveButton((dialog, which) -> {
        });
        chooseDialog.setOnDismissListener((dialog) -> {
            cancelToast();
            finish();
        });

        chooseDialog.addContentView(mPasswordEntryHelper.getPasswordEntryView(),
                new LinearLayout.LayoutParams(1, 0));

        ImageButton button = chooseDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        button.setImageResource(R.drawable.ic_settings_next);
        button.setBackgroundResource(R.drawable.ic_settings_next_bg);
        // This is the real logic for the positive button.
        button.setOnClickListener(v -> {
            updateStage(Stage.Introduction);
            mFirstPin = null;
            mPasswordEntryHelper.showKeyboard();
        });

        return chooseDialog;
    }

    private void setUpPasswordEntry() {
        mPasswordEntryHelper = new PasswordEntryHelper(this, mIsAlphaMode);
        mPasswordEntryHelper.setHint(mIsAlphaMode ? R.string.keyboard_password_hint
                : R.string.screen_lock_pin_hint);

        mPasswordEntryHelper.setOnEditorActionListener((passwordEntry, actionId, event) -> {
            boolean isKeyboardEnterKey = event != null
                    && KeyEvent.isConfirmKey(event.getKeyCode())
                    && event.getAction() == KeyEvent.ACTION_DOWN;
            if (actionId != EditorInfo.IME_ACTION_DONE && !isKeyboardEnterKey) {
                return false;
            }

            mChosenPassword = passwordEntry.getText().toString();

            String errorMsg = null;
            if (mUiStage == Stage.Introduction) {
                errorMsg = mPasswordChecker.validatePassword(mChosenPassword,
                        ChooseLockPassword.this);
                if (errorMsg == null) {
                    mFirstPin = mChosenPassword;
                    updateStage(Stage.NeedToConfirm);
                }
            } else if (mUiStage == Stage.NeedToConfirm) {
                if (mFirstPin.equals(mChosenPassword)) {
                    saveChosenPasswordAndFinish();
                } else {
                    errorMsg = mIsAlphaMode ? getString(R.string.lockpassword_invalid_password)
                            : getString(R.string.lockpassword_invalid_pin);
                }
            }

            if (errorMsg != null) {
                Log.e(TAG, errorMsg);
                showToast(errorMsg);
            }

            return true;
        });
    }

    @Override
    protected void onPause() {
        mDialog.dismiss();
        super.onPause();
    }

    private void saveChosenPasswordAndFinish() {
        showToast(getString(R.string.set_screen_lock_message));
        new PasswordTask(
                mLockPatternUtils,
                mChosenPassword,
                mCurrentPassword,
                mRequestedQuality,
                () -> {
                    // Broadcast the keyguard password state has changed
                    Intent keyguardIntent = new Intent(
                            SystemUIContract.ACTION_KEYGUARD_PASSWORD_SET);
                    keyguardIntent.putExtra(SystemUIContract.EXTRA_PASSWORD_SET, true);
                    sendBroadcast(keyguardIntent);

                    // Start confirmation activity animation
                    Intent intent = new Intent(this, ConfirmationActivity.class);
                    intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                            ConfirmationActivity.SUCCESS_ANIMATION);
                    startActivity(intent);
                    setResult(Activity.RESULT_OK);

                    mPasswordEntryHelper.hideKeyboard();
                    cancelToast();
                    finish();
                },
                mUserId).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void updateStage(Stage stage) {
        mUiStage = stage;
        cancelToast();
        mPasswordEntryHelper.setHint(mIsAlphaMode ? stage.alphaHint : stage.numericHint);
        mPasswordEntryHelper.clearText();
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

    private static class PasswordTask extends AsyncTask<Void, Void, Void> {

        private final LockPatternUtils mLockPatternUtils;
        private final String mChosenPassword;
        private final String mSavedPassword;
        private final Runnable mOnCompleteCallback;
        private final int mPasswordQuality;
        private final int mUserId;

        PasswordTask(LockPatternUtils utils,
                String chosenPassword,
                String savedPassword,
                int passwordQuality,
                Runnable onCompleteCallback,
                int userId) {
            mLockPatternUtils = utils;
            mChosenPassword = chosenPassword;
            mSavedPassword = savedPassword;
            mPasswordQuality = passwordQuality;
            mUserId = userId;
            mOnCompleteCallback = onCompleteCallback;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            mLockPatternUtils
                    .saveLockPassword(mChosenPassword, mSavedPassword, mPasswordQuality, mUserId);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mOnCompleteCallback.run();
        }
    }
}
