package com.google.android.clockwork.settings.personal;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.preference.TwoStatePreference;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.support.wearable.input.WearableButtons;
import android.support.wearable.preference.AcceptDenyDialogPreference;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.util.Log;
import com.android.internal.widget.LockPatternUtils;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.host.GKeys;
import com.google.android.clockwork.settings.DefaultHotwordConfig;
import com.google.android.clockwork.settings.DefaultSettingsContentResolver;
import com.google.android.clockwork.settings.DefaultSmartReplyConfig;
import com.google.android.clockwork.settings.HotwordConfig;
import com.google.android.clockwork.settings.SettingsContentResolver;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.SmartReplyConfig;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;
import com.google.android.clockwork.settings.keyguard.LockSettingsActivity;
import com.google.android.clockwork.settings.personal.device_administration.OverallDeviceAdministrationSettingsFragment;
import com.google.android.clockwork.settings.personal.fitness.models.ExercisesSupportedModel;
import com.google.android.clockwork.settings.utils.FeatureManager;
import com.google.android.clockwork.settings.utils.RetailModeUtil;
import com.google.android.clockwork.common.systemui.SystemUIContract;
import com.google.android.gms.common.GoogleApiAvailabilityLight;
import java.util.Locale;

/**
 * Personal settings.
 */
public class PersonalSettingsFragment extends SettingsPreferenceFragment {

    private static final String TAG = "PersonalSettings";

    private static final String KEY_PREF_LOCK_SCREEN_NOW = "pref_lockScreenNow";
    private static final String KEY_PREF_SCREEN_LOCK = "pref_screenLock";
    private static final String KEY_PREF_BUTTONS = "pref_buttons";
    private static final String KEY_PREF_EXERCISE_DETECTION = "pref_exerciseDetection";
    private static final String KEY_PREF_SMART_REPLIES = "pref_smartReply";
    private static final String KEY_PREF_DEVICE_ADMIN = "pref_deviceAdministration";
    private static final String KEY_PREF_YOLO = "pref_yolo";
    private static final String KEY_PREF_HOTWORD_DETECTION = "pref_hotwordDetection";
    private static final String KEY_PREF_VOICE_ASSISTANT = "pref_voiceAssistant";
    private static final String KEY_PREF_ACCOUNTS = "pref_accounts";
    private static final String KEY_PREF_ALTERNATE_LAUNCHER = "pref_alternateLauncher";

    private static final String SYS_PROP_ALTERNATE_LAUNCHER = "ro.launcher.package";

    private static final String FEATURE_NO_LOCKSCREEN = "android.software.lockscreen_disabled";

    private static final String[] HIDDEN_PREFERENCES_LE = {
        KEY_PREF_ACCOUNTS, KEY_PREF_YOLO, KEY_PREF_DEVICE_ADMIN
    };

    private LockPatternUtils mLockPatternUtils;
    private SettingsContentResolver mSettingsContentResolver;
    private PreferenceScreen mPreferenceScreen;
    private KeyguardChangedReceiver mKeyguardChangedReceiver;

    private boolean mAlternateLauncherEnabledOriginal;
    private boolean mAlternateLauncherEnabledFinal;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_personal);
        addPreferencesFromResource(R.xml.prefs_personal_customization);

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

        initButtonPref(findPreference(KEY_PREF_BUTTONS));
        initExerciseDetectionPref(findPreference(KEY_PREF_EXERCISE_DETECTION));
        initSmartReplyPref((TwoStatePreference) findPreference(KEY_PREF_SMART_REPLIES),
                isLeDevice,
                DefaultSmartReplyConfig.INSTANCE.get(context),
                getResources().getConfiguration().locale,
                mPreferenceScreen);
        initDeviceAdminPref(findPreference(KEY_PREF_DEVICE_ADMIN));
        initYoloPref(findPreference(KEY_PREF_YOLO));

        initHotwordDetection(
                (TwoStatePreference) findPreference(KEY_PREF_HOTWORD_DETECTION),
                DefaultHotwordConfig.getInstance(context),
                isLeDevice,
                context.getResources());
        initVoiceAssistantPref(
                findPreference(KEY_PREF_VOICE_ASSISTANT), isLeDevice, context.getResources());

        mAlternateLauncherEnabledOriginal = 1 == mSettingsContentResolver.getIntValueForKey(
                SettingsContract.ALTERNATE_LAUNCHER_URI,
                SettingsContract.KEY_ALTERNATE_LAUNCHER_ENABLED, 1);
        mAlternateLauncherEnabledFinal = mAlternateLauncherEnabledOriginal;
        initAlternateLauncherPref((TwoStatePreference) findPreference(KEY_PREF_ALTERNATE_LAUNCHER));
    }

    @Override
    public void onDestroy() {
        getContext().unregisterReceiver(mKeyguardChangedReceiver);
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mAlternateLauncherEnabledOriginal != mAlternateLauncherEnabledFinal) {
            mSettingsContentResolver.putIntValueForKey(SettingsContract.ALTERNATE_LAUNCHER_URI,
                    SettingsContract.KEY_ALTERNATE_LAUNCHER_ENABLED,
                    mAlternateLauncherEnabledFinal ? 1 : 0);
        }
    }

    private boolean hasAlternateLauncher() {
        return !TextUtils.isEmpty(SystemProperties.get(SYS_PROP_ALTERNATE_LAUNCHER));
    }

    private void updateLockScreenPref() {
        if (getContext().getPackageManager().hasSystemFeature(FEATURE_NO_LOCKSCREEN)) {
            return;
        }

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
        try {
            return mLockPatternUtils.isSecure(UserHandle.myUserId());
        } catch (Exception e) {
            Log.i(TAG, "Caught exception while checking lock screen state", e);
            return false;
        }
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
    void initSmartReplyPref(TwoStatePreference pref,
            boolean isLeDevice,
            final SmartReplyConfig smartReplyConfig,
            Locale currentLocale,
            PreferenceScreen preferenceScreen) {
        if (pref == null) {
            return;
        }

        pref.setChecked(smartReplyConfig.isSmartReplyEnabled());

        pref.setOnPreferenceChangeListener((p, newVal) -> {
            boolean checked = (Boolean) newVal;
            smartReplyConfig.setSmartReplyEnabled(checked);
            return true;
        });

        // TODO: check against a list of supported languages (just english for now); possibly sync
        // the list through SmartReplyConfig?
        if ((isLeDevice && !Locale.SIMPLIFIED_CHINESE.equals(currentLocale))
            || (!isLeDevice && !Locale.ENGLISH.getLanguage().equals(currentLocale.getLanguage()))) {
            // On LE device, only locale zh_CN is supported. On RoW, only English is supported,
            // including all its country variants.
            preferenceScreen.removePreference(pref);
        }
    }

    @VisibleForTesting
    void initButtonPref(Preference pref) {
        if (pref == null) {
            return;
        }

        Context context = getContext();
        int buttonCount = WearableButtons.getButtonCount(context);
        if (buttonCount > 1) {
            pref.setTitle(getResources().getQuantityString(R.plurals.pref_buttons, buttonCount));

            // If buttons are available, KEYCODE_STEM_1 will always be available. So use its button
            // location as the icon for the settings page.
            if (WearableButtons.getButtonInfo(context, KeyEvent.KEYCODE_STEM_1) != null) {
                pref.setIcon(
                        wrapIcon(WearableButtons.getButtonIcon(context, KeyEvent.KEYCODE_STEM_1)));
            }
        } else {
            // Remove buttons pref if there are no secondary buttons available
            mPreferenceScreen.removePreference(pref);
        }
    }

    public Drawable wrapIcon(Drawable icon) {
        if (icon instanceof LayerDrawable && ((LayerDrawable) icon).findDrawableByLayerId(
                R.id.nested_icon) != null) {
            return icon;
        } else {
            LayerDrawable wrappedDrawable = (LayerDrawable) getContext().getDrawable(
                    R.drawable.ic_cc_settings_buttons);
            wrappedDrawable.setDrawableByLayerId(R.id.icon, icon);
            return wrappedDrawable;
        }
    }

    @VisibleForTesting
    void initExerciseDetectionPref(Preference pref) {
        if (pref == null) {
            return;
        }

        // Remove exercise detection on unsupported devices.
        ExercisesSupportedModel supportedModel = new ExercisesSupportedModel(getResources());
        if (!supportedModel.hasHardwareSupport() || !supportedModel.hasSupportedExercises()) {
            mPreferenceScreen.removePreference(pref);
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

    @VisibleForTesting
    void initHotwordDetection(
            TwoStatePreference pref,
            final HotwordConfig hotwordConfig,
            boolean isLeDevice,
            Resources resources) {
        // For LE devices (China) the Hotword is not the standard "OK Google", so we need to update
        // the string for the setting to display the correct Hotword.
        if (isLeDevice) {
            pref.setTitle(resources.getText(R.string.pref_hotwordDetectionLe));
        }
        pref.setChecked(hotwordConfig.isHotwordDetectionEnabled());

        pref.setOnPreferenceChangeListener((p, newVal) -> {
            hotwordConfig.setHotwordDetectionEnabled((Boolean) newVal);
            return true;
        });
    }

    @VisibleForTesting
    void initVoiceAssistantPref(Preference pref, boolean isLeDevice, Resources resources) {
        boolean multipleVoiceAssistants =
                resources.getStringArray(R.array.config_le_system_voice_assistant_packages).length
                        > 1;
        if (!isLeDevice || !multipleVoiceAssistants) {
            if (pref != null) {
                getPreferenceScreen().removePreference(pref);
            }
        }
    }

    @VisibleForTesting
    void initAlternateLauncherPref(TwoStatePreference pref) {
        if(hasAlternateLauncher()) {
            pref.setChecked(mAlternateLauncherEnabledOriginal);
            pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    mAlternateLauncherEnabledFinal = (boolean) newValue;
                    return true;
                }
            });
        } else {
            getPreferenceScreen().removePreference(pref);
        }
    }
}
