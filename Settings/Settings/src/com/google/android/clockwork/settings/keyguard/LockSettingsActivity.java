package com.google.android.clockwork.settings.keyguard;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.wearable.preference.WearablePreferenceActivity;

import com.android.internal.widget.LockPatternUtils;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.common.RadioButtonPreference;
import com.google.android.clockwork.settings.common.RadioGroupPreferenceScreenHelper;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;

/**
 * Device lock/keyguard settings. Allows the user to set a new pattern/pin/password or remove their
 * existing screen lock. Requires keyguard verification to launch if the user has keyguard enabled.
 */
public class LockSettingsActivity extends WearablePreferenceActivity {

    private static final int CONFIRM_KEY_GUARD_REQUEST = 1;
    private static final int CHANGE_KEY_GUARD_REQUEST = 2;

    private static final String KEY_PREF_NONE = "pref_none";
    private static final String KEY_PREF_PATTERN = "pref_pattern";
    private static final String KEY_PREF_PIN = "pref_pin";
    private static final String KEY_PREF_PASSWORD = "pref_password";

    private LockSettingsFragment fragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            fragment = new LockSettingsFragment();
            startPreferenceFragment(fragment, false);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if ((requestCode == CONFIRM_KEY_GUARD_REQUEST) && (resultCode == Activity.RESULT_OK)) {
            // save password and show visible activity.
            fragment.mCurrentPassword = intent.getStringExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD);
        } else {
            finish();
        }
    }

    public static class LockSettingsFragment extends SettingsPreferenceFragment {
        private String mCurrentPassword;
        private RadioGroupPreferenceScreenHelper mHelper;
        private LockPatternUtils mLockPatternUtils;
        private RadioButtonPreference mNone;
        private RadioButtonPreference mPattern;
        private RadioButtonPreference mPin;
        private RadioButtonPreference mPassword;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.prefs_keyguard);

            // helper runs first
            mHelper = new RadioGroupPreferenceScreenHelper(getPreferenceScreen());
            mLockPatternUtils = new LockPatternUtils(getContext());

            // init functions overwrites OnClickListener set by helper
            initNone(mNone = (RadioButtonPreference) findPreference(KEY_PREF_NONE));
            initPattern(mPattern = (RadioButtonPreference) findPreference(KEY_PREF_PATTERN));
            initPin(mPin = (RadioButtonPreference) findPreference(KEY_PREF_PIN));
            initPassword(mPassword = (RadioButtonPreference) findPreference(KEY_PREF_PASSWORD));

            disableUnusablePreferencesImpl();

            ChooseLockSettingsHelper.launchConfirmationActivity(
                    getActivity(), CONFIRM_KEY_GUARD_REQUEST);
        }

        @Override
        public void onResume() {
            super.onResume();
            int quality = mLockPatternUtils.getActivePasswordQuality(UserHandle.myUserId());
            if (quality > DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX) {
                mHelper.check(mPassword);
            } else if (quality > DevicePolicyManager.PASSWORD_QUALITY_SOMETHING) {
                mHelper.check(mPin);
            } else if (quality > DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED) {
                mHelper.check(mPattern);
            } else {
                mHelper.check(mNone);
            }
        }

        private void initNone(Preference pref) {
            pref.setOnPreferenceClickListener((p) -> {
                if (mLockPatternUtils.getActivePasswordQuality(UserHandle.myUserId())
                        > DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED) {
                    Intent removeLockIntent = new Intent(getContext(), RemoveLockActivity.class);
                    removeLockIntent.putExtra(
                            ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD, mCurrentPassword);
                    getActivity().startActivityForResult(removeLockIntent, 0);
                }
                return true;
            });
        }

        private void initPattern(Preference pref) {
            pref.setOnPreferenceClickListener((p) -> {
                Intent patternIntent = new Intent(getContext(), ChooseLockPattern.class);
                patternIntent.putExtra(
                        ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD, mCurrentPassword);
                getActivity().startActivityForResult(patternIntent, CHANGE_KEY_GUARD_REQUEST);
                return true;
            });
        }

        private void initPin(Preference pref) {
            pref.setOnPreferenceClickListener((p) -> {
                Intent pinIntent = new Intent(getContext(), ChooseLockPassword.class);
                pinIntent.putExtra(LockPatternUtils.PASSWORD_TYPE_KEY,
                        DevicePolicyManager.PASSWORD_QUALITY_NUMERIC);
                pinIntent.putExtra(
                        ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD, mCurrentPassword);
                getActivity().startActivityForResult(pinIntent, CHANGE_KEY_GUARD_REQUEST);
                return true;
            });
        }

        private void initPassword(Preference pref) {
            pref.setOnPreferenceClickListener((p) -> {
                Intent passwordIntent = new Intent(getContext(), ChooseLockPassword.class);
                passwordIntent.putExtra(LockPatternUtils.PASSWORD_TYPE_KEY,
                        DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC);
                passwordIntent
                        .putExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD, mCurrentPassword);
                getActivity().startActivityForResult(passwordIntent, CHANGE_KEY_GUARD_REQUEST);
                return true;
            });
        }

        /** Disables preferences that are less secure than device admin quality. */
        private void disableUnusablePreferencesImpl() {
            DevicePolicyManager dpm =
                    (DevicePolicyManager) getContext().getSystemService(
                            Context.DEVICE_POLICY_SERVICE);
            int adminEnforcedQuality = dpm.getPasswordQuality(null);

            boolean isLockScreenDisabled =
                    mLockPatternUtils.isLockScreenDisabled(UserHandle.myUserId());

            if (adminEnforcedQuality > DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED) {
                disableSettingByAdmin(mNone);
            }

            if (adminEnforcedQuality > DevicePolicyManager.PASSWORD_QUALITY_SOMETHING
                    || isLockScreenDisabled) {
                disableSettingByAdmin(mPattern);
            }

            if (adminEnforcedQuality > DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX
                    || isLockScreenDisabled) {
                disableSettingByAdmin(mPin);
            }

            if (isLockScreenDisabled) {
                disableSettingByAdmin(mPassword);
            }
        }

        private void disableSettingByAdmin(Preference pref) {
            pref.setEnabled(false);
            pref.setSummary(R.string.disabled_by_admin);
        }
    }
}
