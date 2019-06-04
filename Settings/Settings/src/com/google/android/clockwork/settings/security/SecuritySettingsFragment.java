package com.google.android.clockwork.settings.security;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.support.wearable.preference.AcceptDenyDialogPreference;
import com.android.internal.widget.LockPatternUtils;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.common.systemui.SystemUIContract;
import com.google.android.clockwork.host.GKeys;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;
import com.google.android.clockwork.settings.DefaultSettingsContentResolver;
import com.google.android.clockwork.settings.SettingsContentResolver;
import com.google.android.clockwork.settings.keyguard.LockSettingsActivity;
import com.google.android.clockwork.settings.personal.device_administration.OverallDeviceAdministrationSettingsFragment;
import com.google.android.clockwork.settings.utils.FeatureManager;
import com.google.android.clockwork.settings.utils.RetailModeUtil;
import com.google.android.gms.common.GoogleApiAvailabilityLight;

/**
 * Security settings.
 */
public class SecuritySettingsFragment extends SettingsPreferenceFragment {

    private static final String TAG = "SecuritySettings";

    private static final String KEY_PREF_LOCK_SCREEN_NOW = "pref_lockScreenNow";
    private static final String KEY_PREF_SCREEN_LOCK = "pref_screenLock";
    private static final String KEY_PREF_DEVICE_ADMIN = "pref_deviceAdministration";
    private static final String KEY_PREF_YOLO = "pref_yolo";
    private static final String KEY_PREF_ACCOUNTS = "pref_accounts";

    private static final String[] HIDDEN_PREFERENCES_LE = {
        KEY_PREF_ACCOUNTS, KEY_PREF_YOLO, KEY_PREF_DEVICE_ADMIN
    };

    private LockPatternUtils mLockPatternUtils;
    private SettingsContentResolver mSettingsContentResolver;
    private PreferenceScreen mPreferenceScreen;
    private KeyguardChangedReceiver mKeyguardChangedReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_security);

        Context context = getContext();
        mPreferenceScreen = getPreferenceScreen();
        mLockPatternUtils = new LockPatternUtils(context);

        mSettingsContentResolver = new DefaultSettingsContentResolver(
                context.getContentResolver());
        mKeyguardChangedReceiver = new KeyguardChangedReceiver();
        context.registerReceiver(mKeyguardChangedReceiver,
                new IntentFilter(SystemUIContract.ACTION_KEYGUARD_PASSWORD_SET));

        boolean isLeDevice = FeatureManager.INSTANCE.get(context).isLocalEditionDevice();
        if (isLeDevice) {
            for (String key : HIDDEN_PREFERENCES_LE) {
                Preference pref = findPreference(key);
                if (pref != null) {
                    getPreferenceScreen().removePreference(pref);
                }
            }
        }

        updateLockScreenPref();
        initDeviceAdminPref(findPreference(KEY_PREF_DEVICE_ADMIN));
        initYoloPref(findPreference(KEY_PREF_YOLO));
    }

    @Override
    public void onDestroy() {
        getContext().unregisterReceiver(mKeyguardChangedReceiver);
        super.onDestroy();
    }

    private void updateLockScreenPref() {
        boolean screenLockOn = isLockScreenSecured();
        final boolean setupWizardHasRun = Settings.System.getInt(
                getContext().getContentResolver(), Settings.System.SETUP_WIZARD_HAS_RUN, 0) == 1;
        boolean inRetail = RetailModeUtil.isInRetailMode(mSettingsContentResolver);

        // Remove existing lock screen prefs.
        Preference screenLockPref = findPreference(KEY_PREF_SCREEN_LOCK);
        if (screenLockPref != null) {
            mPreferenceScreen.removePreference(screenLockPref);
        }

        // add screen lock preference based on state.
        if (setupWizardHasRun && !inRetail) {
            screenLockPref = new Preference(getContext());
            screenLockPref.setKey(KEY_PREF_SCREEN_LOCK);
            screenLockPref.setTitle(R.string.pref_screenLock);
            screenLockPref.setSummary(screenLockOn
                    ? R.string.generic_automatic
                    : R.string.generic_disabled);
            screenLockPref.setIcon(isLockScreenSecured()
                    ? R.drawable.ic_cc_settings_screen_lock_automatic
                    : R.drawable.ic_cc_settings_screen_lock_off);
            screenLockPref.setIntent(new Intent(getContext(), LockSettingsActivity.class));

            mPreferenceScreen.addPreference(screenLockPref);
        }

        // Remove existing lock screen prefs.
        AcceptDenyDialogPreference lockNowPref =
                (AcceptDenyDialogPreference) findPreference(KEY_PREF_LOCK_SCREEN_NOW);
        if (lockNowPref != null) {
            mPreferenceScreen.removePreference(lockNowPref);
        }

        // Add lock screen now preference based on state.
        if (screenLockOn && !inRetail) {
            lockNowPref = new AcceptDenyDialogPreference(getContext());
            lockNowPref.setKey(KEY_PREF_LOCK_SCREEN_NOW);
            lockNowPref.setTitle(R.string.pref_lockScreenNow);
            lockNowPref.setIcon(R.drawable.ic_cc_settings_screen_lock_off);
            lockNowPref.setDialogTitle(R.string.pref_lockScreenNow);
            lockNowPref.setOnDialogClosedListener((positiveResult) -> {
                if (positiveResult) {
                    Intent lockIntent = new Intent(SystemUIContract.ACTION_SHOW_KEYGUARD);
                    lockIntent.putExtra(SystemUIContract.EXTRA_SOURCE, TAG);
                    lockIntent.putExtra(SystemUIContract.EXTRA_LOCK_NOW, true);
                    lockIntent.setPackage(SystemUIContract.KEYGUARD_PACKAGE);
                    getContext().sendBroadcast(lockIntent);
                    getActivity().finish();
                }
            });

            mPreferenceScreen.addPreference(lockNowPref);
        }
    }

    private boolean isLockScreenSecured() {
        return mLockPatternUtils.isSecure(UserHandle.myUserId());
    }

    /**
     * Receiver to detect when keyguard has been changed and thus, the lockscreen prefs need to
     * be updated.
     */
    private class KeyguardChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (SystemUIContract.ACTION_KEYGUARD_PASSWORD_SET.equals(intent.getAction())) {
                updateLockScreenPref();
            }
        }
    }

    @VisibleForTesting
    void initDeviceAdminPref(Preference pref) {
        if (pref == null) {
            return;
        }

        // Enable additional settings on newer gmscore versions for certain watches (as decided by
        // the ENABLE_ADM_SETTINGS flag).

        // If the watch doesn't have v9 or higher yet, these settings should not be displayed.
        // If the watch does have v9 or higher, check if the server says to display the settings.
        // The server will say not to display the settings if the device does not have an onboard
        // GPS and b/32830840 is not yet fixed in the current gmscore version.

        int version = GoogleApiAvailabilityLight.getInstance().getApkVersion(getContext());
        // Remove last 3 digits which represent verison minor (1 digit) and version build (2 digits)
        int versionMajor = version / 1000;
        int gmscoreV9VersionMajor = 10200;

        if ((versionMajor >= gmscoreV9VersionMajor) && GKeys.ENABLE_ADM_SETTINGS.get()) {
            pref.setFragment(OverallDeviceAdministrationSettingsFragment.class.getName());
        }
    }

    @VisibleForTesting
    void initYoloPref(Preference pref) {
        if (pref == null) {
            return;
        }

        // Enable YOLO/"Smart Lock for Passwords" only on devices w/GMSCore versions that support
        // it.

        int version = GoogleApiAvailabilityLight.getInstance().getApkVersion(getContext());

        if (version < GKeys.GMSCORE_VERSION_FOR_YOLO.get()) {
            mPreferenceScreen.removePreference(pref);
        } else {
            pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent yoloSettingsIntent = new Intent();
                    yoloSettingsIntent.setAction(
                            "com.google.android.gms.settings.CREDENTIALS_SETTINGS");
                    startActivityForResult(yoloSettingsIntent, 0);
                    return true;
                }
            });
        }
    }
}
