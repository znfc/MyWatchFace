/*
 * Copyright (C) 2015 The Android Open Source Project
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
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Trace;
import android.os.UserHandle;
import android.support.wearable.activity.ConfirmationActivity;
import android.widget.Toast;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.common.systemui.SystemUIContract;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper to process creating a new lock pattern.
 */
public class ChooseLockPattern extends Activity {
    private static final int WRONG_PATTERN_CLEAR_TIMEOUT_MS = 2000;
    private static final int STEP_CONFIRMATION_TIME_MS = 1000;
    private static final int MAX_CONFIRMATION_ATTEMPTS = 3;

    private LockPatternUtils mLockPatternUtils;
    private LockPatternView mLockPatternView;
    private boolean mDone;

    // If available, provide the current password when changing the device lock so that existing
    // keys aren't invalidated
    private String mCurrentPassword;

    // The number of attempts confirming pattern.
    private int mAttempts;

    private Stage mUiStage = Stage.Introduction;
    private ArrayList<LockPatternView.Cell> mChosenPattern;

    private Handler mHandler = new Handler();

    private Toast mCurrentToast;

    private Runnable mClearPatternRunnable = new Runnable() {
        public void run() {
            mLockPatternView.clearPattern();
        }
    };

    /**
     * The pattern listener that responds according to a user choosing a new
     * lock pattern.
     */
    private LockPatternView.OnPatternListener mChooseNewLockPatternListener =
            new LockPatternView.OnPatternListener() {

                public void onPatternStart() {
                    mLockPatternView.removeCallbacks(mClearPatternRunnable);
                    patternInProgress();
                }

                public void onPatternCleared() {
                    mLockPatternView.removeCallbacks(mClearPatternRunnable);
                }

                @Override
                public void onPatternCellAdded(List<LockPatternView.Cell> pattern) {

                }

                public void onPatternDetected(List<LockPatternView.Cell> pattern) {
                    if (mUiStage == Stage.NeedToConfirm || mUiStage == Stage.ConfirmWrong) {
                        if (mChosenPattern == null) {
                            throw new IllegalStateException(
                                    "null chosen pattern in stage 'need to confirm");
                        }
                        if (mChosenPattern.equals(pattern)) {
                            updateStage(Stage.ChoiceConfirmed);
                        } else {
                            updateStage(Stage.ConfirmWrong);
                        }
                    } else if (mUiStage == Stage.Introduction || mUiStage == Stage.ChoiceTooShort) {
                        if (pattern.size() < LockPatternUtils.MIN_LOCK_PATTERN_SIZE) {
                            updateStage(Stage.ChoiceTooShort);
                        } else {
                            mChosenPattern = new ArrayList<>(pattern);
                            updateStage(Stage.FirstChoiceValid);
                        }
                    } else {
                        throw new IllegalStateException("Unexpected stage " + mUiStage + " when "
                                + "entering the pattern.");
                    }
                }

                private void patternInProgress() {
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() != null
                && getIntent().hasExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD)) {
            mCurrentPassword =
                    getIntent().getStringExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD);
        }
        setContentView(R.layout.screen_lock);
        mLockPatternView = findViewById(R.id.lockPatternView);
        mLockPatternView.setOnPatternListener(mChooseNewLockPatternListener);
        mLockPatternView.setFadePattern(false);
        mLockPatternUtils = new LockPatternUtils(this);
        updateStage(mUiStage);
    }

    /**
     * Keep track internally of where the user is in choosing a pattern.
     */
    protected enum Stage {
        Introduction(
                R.string.lockpattern_recording_intro_header, true),
        ChoiceTooShort(
                R.string.lockpattern_recording_incorrect_too_short, true),
        FirstChoiceValid(
                R.string.lockpattern_pattern_entered_header, false),
        NeedToConfirm(
                R.string.lockpattern_need_to_confirm, true),
        ConfirmWrong(
                R.string.lockpattern_need_to_unlock_wrong, true),
        ChoiceConfirmed(0, false);


        /**
         * @param message        Message to be displayed.
         * @param patternEnabled Whether the pattern widget is enabled.
         */
        Stage(int message, boolean patternEnabled) {
            this.message = message;
            this.patternEnabled = patternEnabled;
        }

        final int message;
        final boolean patternEnabled;
    }

    private void showToast(Toast toast) {
        if (mCurrentToast != null) {
            mCurrentToast.cancel();
        }

        mCurrentToast = toast;

        if (mCurrentToast != null) {
            mCurrentToast.show();
        }
    }

    private void cancelToast() {
        if (mCurrentToast != null) {
            mCurrentToast.cancel();
        }
        mCurrentToast = null;
    }

    protected void updateStage(Stage stage) {
        mUiStage = stage;

        // header text, footer text, visibility and
        // enabled state all known from the stage
        if (stage == Stage.ChoiceTooShort) {
            showToast(Toast.makeText(this, getString(
                    stage.message,
                    LockPatternUtils.MIN_LOCK_PATTERN_SIZE), Toast.LENGTH_SHORT));
        } else if (stage.message != 0) {
            showToast(Toast.makeText(this, getString(stage.message), Toast.LENGTH_SHORT));
        }

        // same for whether the patten is enabled
        if (stage.patternEnabled) {
            mLockPatternView.enableInput();
        } else {
            mLockPatternView.disableInput();
        }

        // the rest of the stuff varies enough that it is easier just to handle
        // on a case by case basis.
        mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Correct);

        switch (mUiStage) {
            case Introduction:
                mAttempts = 0;
                mLockPatternView.clearPattern();
                break;
            case ChoiceTooShort:
                mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
                postClearPatternRunnable();
                break;
            case FirstChoiceValid:
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        updateStage(Stage.NeedToConfirm);
                    }
                }, STEP_CONFIRMATION_TIME_MS);
                break;
            case NeedToConfirm:
                mLockPatternView.clearPattern();
                break;
            case ConfirmWrong:
                mAttempts++;

                if (mAttempts >= MAX_CONFIRMATION_ATTEMPTS) {
                    updateStage(Stage.Introduction);
                } else {
                    mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
                    postClearPatternRunnable();
                }
                break;
            case ChoiceConfirmed:
                saveChosenPatternAndFinish();
                break;
        }
    }

    // clear the wrong pattern unless they have started a new one
    // already
    private void postClearPatternRunnable() {
        mLockPatternView.removeCallbacks(mClearPatternRunnable);
        mLockPatternView.postDelayed(mClearPatternRunnable, WRONG_PATTERN_CLEAR_TIMEOUT_MS);
    }

    private void saveChosenPatternAndFinish() {
        if (mDone) {
            return;
        }
        showToast(Toast.makeText(this, R.string.set_screen_lock_message, Toast.LENGTH_SHORT));
        new PatternTask(
                mLockPatternUtils,
                mChosenPattern,
                mCurrentPassword,
                () -> {
                    showToast(null);

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

                    cancelToast();
                    finish();
                    mDone = true;
                },
                UserHandle.myUserId()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private static class PatternTask extends AsyncTask<Void, Void, Void> {

        private final LockPatternUtils mLockPatternUtils;
        private final String mSavedPattern;
        private final List<LockPatternView.Cell> mPattern;
        private final Runnable mOnCompleteCallback;
        private final int mUserId;

        PatternTask(LockPatternUtils utils,
                List<LockPatternView.Cell> pattern,
                String savedPattern,
                Runnable onCompleteCallback,
                int userId) {
            mLockPatternUtils = utils;
            mSavedPattern = savedPattern;
            mUserId = userId;
            mPattern = new ArrayList<>(pattern);
            mOnCompleteCallback = onCompleteCallback;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            boolean lockVirgin = !mLockPatternUtils.isPatternEverChosen(mUserId);
            Trace.endSection();

            mLockPatternUtils.saveLockPattern(mPattern, mSavedPattern, mUserId);

            if (lockVirgin) {
                mLockPatternUtils.setVisiblePatternEnabled(true, mUserId);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mOnCompleteCallback.run();
        }
    }
}
