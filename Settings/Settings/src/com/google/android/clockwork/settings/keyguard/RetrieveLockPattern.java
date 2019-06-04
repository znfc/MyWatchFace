package com.google.android.clockwork.settings.keyguard;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.widget.Toast;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.common.systemui.SystemUIContract;
import com.google.android.clockwork.keyguard.KeyguardValidator;

import java.util.List;

/**
 * Base class for Showing a Lock Pattern and confirming the pattern.
 */
public class RetrieveLockPattern extends Activity implements LockPatternView.OnPatternListener {
    private static final int WRONG_PATTERN_CLEAR_TIMEOUT_MS = 2000;

    private LockPatternView mLockPatternView;
    private KeyguardValidator mKeyguardValidator;

    private Toast mCurrentToast;

    private Runnable mClearPatternRunnable = new Runnable() {
        public void run() {
            mLockPatternView.clearPattern();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_lock);
        mLockPatternView = findViewById(R.id.lockPatternView);
        LockPatternUtils lockPatternUtils = new LockPatternUtils(this);
        mLockPatternView.setOnPatternListener(this);
        mKeyguardValidator = new KeyguardValidator(lockPatternUtils, false, false);

        showToast(Toast.makeText(this, getString(R.string.screen_lock_enter_pattern),
                Toast.LENGTH_SHORT));
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

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

    @Override
    public void onPatternStart() {
        mLockPatternView.removeCallbacks(mClearPatternRunnable);
    }

    @Override
    public void onPatternCleared() {}

    @Override
    public void onPatternCellAdded(List<LockPatternView.Cell> pattern) {}

    @Override
    public void onPatternDetected(final List<LockPatternView.Cell> pattern) {
        mLockPatternView.disableInput();
        mKeyguardValidator.validatePattern(
                pattern,
                UserHandle.myUserId(),
                new CallbackImpl(pattern));
    }

    private class CallbackImpl implements KeyguardValidator.Callback {
        private final List<LockPatternView.Cell> mPattern;

        public CallbackImpl(List<LockPatternView.Cell> pattern) {
            mPattern = pattern;
        }

        @Override
        public void onMatched() {
            Intent intent = new Intent();
            intent.putExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD,
                    LockPatternUtils.patternToString(mPattern));

            Intent unlockIntent = new Intent(SystemUIContract.ACTION_HIDE_KEYGUARD);
            sendBroadcast(unlockIntent);

            mLockPatternView.enableInput();
            mLockPatternView.clearPattern();

            setResult(Activity.RESULT_OK, intent);
            showToast(null);
            finish();
        }

        @Override
        public void onRejected(int reason, long lockoutDeadlineMs) {
            boolean tooManyAttempts = lockoutDeadlineMs > 0;

            mLockPatternView.enableInput();
            mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
            mLockPatternView.removeCallbacks(mClearPatternRunnable);
            mLockPatternView.postDelayed(mClearPatternRunnable,
                    WRONG_PATTERN_CLEAR_TIMEOUT_MS);
            showToast(Toast.makeText(RetrieveLockPattern.this,
                    getString(tooManyAttempts
                            ? R.string.screen_lock_too_many_attempts
                            : R.string.screen_lock_pattern_no_match),
                    Toast.LENGTH_SHORT));

            if (tooManyAttempts) {
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        }
    }
}
