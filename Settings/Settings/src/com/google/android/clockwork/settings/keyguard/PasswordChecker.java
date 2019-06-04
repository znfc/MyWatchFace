package com.google.android.clockwork.settings.keyguard;

import android.app.admin.DevicePolicyManager;
import android.app.admin.PasswordMetrics;
import android.content.Context;

import com.android.internal.widget.LockPatternUtils;
import com.google.android.apps.wearable.settings.R;
import com.google.common.annotations.VisibleForTesting;

/**
 * Verifier for password.
 */
public class PasswordChecker {

    private final int mPasswordMinLength;
    private final int mPasswordMaxLength;
    private final int mPasswordMinLetters;
    private final int mPasswordMinUpperCase;
    private final int mPasswordMinLowerCase;
    private final int mPasswordMinSymbols;
    private final int mPasswordMinNumeric;
    private final int mPasswordMinNonLetter;
    private final int mRequestedQuality;
    private final boolean mIsAlphaMode;
    private final LockPatternUtils mLockPatternUtils;
    private final int mUserId;

    public PasswordChecker(
            LockPatternUtils lockPatternUtils,
            int passwordMinLength,
            int passwordMaxLength,
            int passwordMinLetters,
            int passwordMinUpperCase,
            int passwordMinLowerCase,
            int passwordMinSymbols,
            int passwordMinNumeric,
            int passwordMinNonLetter,
            int requestedQuality,
            boolean isAlphaMode,
            int userId) {
        mLockPatternUtils = lockPatternUtils;
        mPasswordMinLength = passwordMinLength;
        mPasswordMaxLength = passwordMaxLength;
        mPasswordMinLetters = passwordMinLetters;
        mPasswordMinUpperCase = passwordMinUpperCase;
        mPasswordMinLowerCase = passwordMinLowerCase;
        mPasswordMinSymbols = passwordMinSymbols;
        mPasswordMinNumeric = passwordMinNumeric;
        mPasswordMinNonLetter = passwordMinNonLetter;
        mRequestedQuality = requestedQuality;
        mIsAlphaMode = isAlphaMode;
        mUserId = userId;
    }

    /**
     * Validates PIN or password and returns a message to display if PIN fails test.
     * @param password the raw password the user typed in
     * @return error message to show to user or null if password is OK
     */
    public String validatePassword(String password, Context context) {
        PasswordMetrics passwordMetrics = PasswordMetrics.computeForPassword(password);
        if (passwordMetrics.length < mPasswordMinLength) {
            return context.getString(mIsAlphaMode ?
                    R.string.lockpassword_password_too_short
                    : R.string.lockpassword_pin_too_short, mPasswordMinLength);
        }
        if (passwordMetrics.length > mPasswordMaxLength) {
            return context.getString(mIsAlphaMode ?
                    R.string.lockpassword_password_too_long
                    : R.string.lockpassword_pin_too_long, mPasswordMaxLength + 1);
        }
        if (DevicePolicyManager.PASSWORD_QUALITY_NUMERIC == mRequestedQuality
                || DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX == mRequestedQuality) {
            if (passwordMetrics.letters > 0 || passwordMetrics.symbols > 0) {
                // This shouldn't be possible unless user finds some way to bring up
                // soft keyboard
                return context.getString(R.string.lockpassword_pin_contains_non_digits);
            }

            // Check for repeated characters or sequences (e.g. '1234', '0000', '2468')
            int sequence = PasswordMetrics.maxLengthSequence(password);
            if (DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX == mRequestedQuality
                    && sequence > PasswordMetrics.MAX_ALLOWED_SEQUENCE) {
                return context.getString(R.string.lockpassword_pin_no_sequential_digits);
            }
        } else if (DevicePolicyManager.PASSWORD_QUALITY_COMPLEX == mRequestedQuality) {
            if (passwordMetrics.letters < mPasswordMinLetters) {
                return String.format(context.getResources().getQuantityString(
                        R.plurals.lockpassword_password_requires_letters, mPasswordMinLetters),
                        mPasswordMinLetters);
            } else if (passwordMetrics.numeric < mPasswordMinNumeric) {
                return String.format(context.getResources().getQuantityString(
                        R.plurals.lockpassword_password_requires_numeric, mPasswordMinNumeric),
                        mPasswordMinNumeric);
            } else if (passwordMetrics.lowerCase < mPasswordMinLowerCase) {
                return String.format(context.getResources().getQuantityString(
                        R.plurals.lockpassword_password_requires_lowercase, mPasswordMinLowerCase),
                        mPasswordMinLowerCase);
            } else if (passwordMetrics.upperCase < mPasswordMinUpperCase) {
                return String.format(context.getResources().getQuantityString(
                        R.plurals.lockpassword_password_requires_uppercase, mPasswordMinUpperCase),
                        mPasswordMinUpperCase);
            } else if (passwordMetrics.symbols < mPasswordMinSymbols) {
                return String.format(context.getResources().getQuantityString(
                        R.plurals.lockpassword_password_requires_symbols, mPasswordMinSymbols),
                        mPasswordMinSymbols);
            } else if (passwordMetrics.nonLetter < mPasswordMinNonLetter) {
                return String.format(context.getResources().getQuantityString(
                        R.plurals.lockpassword_password_requires_nonletter, mPasswordMinNonLetter),
                        mPasswordMinNonLetter);
            }
        } else {
            final boolean alphabetic = DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC
                    == mRequestedQuality;
            final boolean alphanumeric = DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC
                    == mRequestedQuality;
            if ((alphabetic || alphanumeric) && passwordMetrics.letters == 0) {
                return context.getString(R.string.lockpassword_password_requires_alpha);
            }
            if (alphanumeric && passwordMetrics.numeric == 0) {
                return context.getString(R.string.lockpassword_password_requires_digit);
            }
        }
        byte[] hashFactor = mLockPatternUtils.getPasswordHistoryHashFactor(password, mUserId);
        if (mLockPatternUtils.checkPasswordHistory(password, hashFactor, mUserId)) {
            return context.getString(mIsAlphaMode ? R.string.lockpassword_password_recently_used
                    : R.string.lockpassword_pin_recently_used);
        }

        return null;
    }
}
