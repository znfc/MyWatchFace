package com.google.android.clockwork.settings;

import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.storage.StorageManager;
import android.preference.Preference;
import android.provider.Settings;
import android.support.wearable.preference.WearablePreferenceActivity;
import android.text.SpannableString;
import android.util.Log;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.accessibility.AccessibilityFragment;
import com.google.android.clockwork.settings.accessibility.tts.TtsFragment;
import com.google.android.clockwork.settings.apps.AdvancedAppSettingsFragment;
import com.google.android.clockwork.settings.apps.AppDetailsFragment;
import com.google.android.clockwork.settings.apps.AppStorageSettingsFragment;
import com.google.android.clockwork.settings.apps.AppsSettingsFragment;
import com.google.android.clockwork.settings.apps.UsageAccessAppsListFragment;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;
import com.google.android.clockwork.settings.connectivity.BluetoothSettingsFragment;
import com.google.android.clockwork.settings.connectivity.ConnectivitySettingsFragment;
import com.google.android.clockwork.settings.connectivity.nfc.NfcSettingsFragment;
import com.google.android.clockwork.settings.connectivity.wifi.WifiSettingsFragment;
import com.google.android.clockwork.settings.developer.DeveloperOptionsFragment;
import com.google.android.clockwork.settings.display.DisplaySettingsFragment;
import com.google.android.clockwork.settings.enterprise.EnterprisePrivacySettingsFragment;
import com.google.android.clockwork.settings.gestures.GesturesSettingsFragment;
import com.google.android.clockwork.settings.personal.AccountSettingsFragment;
import com.google.android.clockwork.settings.personal.buttons.ButtonSettingsFragment;
import com.google.android.clockwork.settings.personal.fitness.ExerciseDetectionSettingsFragment;
import com.google.android.clockwork.settings.security.SecuritySettingsFragment;
import com.google.android.clockwork.settings.sound.SoundSettingsFragment;
import com.google.android.clockwork.settings.sound.DoNotDisturbSettingsFragment;
import com.google.android.clockwork.settings.system.BatterySaverFragment;
import com.google.android.clockwork.settings.system.DateTimeSettingsFragment;
import com.google.android.clockwork.settings.system.DeviceInfoFragment;
import com.google.android.clockwork.settings.system.SystemSettingsFragment;
import com.google.android.clockwork.settings.utils.DefaultBluetoothModeManager;
import com.google.android.clockwork.settings.utils.FeatureManager;
import com.google.android.clockwork.settings.utils.RetailModeUtil;
import com.google.android.clockwork.telephony.Utils;
import android.content.Context;
import android.support.wearable.view.AcceptDenyDialog;
import android.support.wearable.preference.AcceptDenySwitchPreference;
import android.os.PowerManager;
import android.os.BatteryManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnCancelListener;
import android.text.Spanned;
import android.text.style.AlignmentSpan;
import android.text.Layout;

/**
 * Main settings activity menu, mostly static.
 */
public class MainSettingsActivity extends WearablePreferenceActivity {
    private static final String TAG = "MainSettingsActivity";

    private static final String ACTION_WRIST_GESTURES_SETTINGS =
            "com.google.android.clockwork.settings.WRIST_GESTURE_SETTINGS_DIALOG";

    // This is not defined in android.provider.Settings unlike other similar ones.
    private static final String ACTION_TTS_SETTINGS = "com.android.settings.TTS_SETTINGS";

    // Sent by the System Battery Service
    private static final String ACTION_ENTER_TWM = "com.google.android.clockwork.settings.ENTER_TWM";

    private static final String KEY_PREF_EMERGENCY_DIALER =
            "pref_emergency_dialer";
    private static final String KEY_PREF_MEDIA_VOLUME = "pref_mediaVolume";
    private static final String KEY_PREF_SOUND_NOTIFICATION = "pref_soundNotification";
    private static final String KEY_PREF_WIFI = "pref_wifi";
    private static final String KEY_BATTERY_SAVER = "pref_batterySaver_suggested_settings";
    private static final String KEY_DIVIDER = "pref_divider";
    private static final String KEY_PREF_GENERAL = "pref_mainGeneral";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initFeatureManager();
        if (savedInstanceState == null) {
            startPreferenceFragment();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        startPreferenceFragment();
    }

    private void startPreferenceFragment() {
        Intent intent = getIntent();
        String action = intent.getAction();
        Fragment f;

        if (Settings.ACTION_LOCATION_SOURCE_SETTINGS.equals(action)) {
            f = ConnectivitySettingsFragment.newInstance(true);
        } else if (Settings.ACTION_WIFI_SETTINGS.equals(action)) {
            f = new WifiSettingsFragment();
        } else if (Settings.ACTION_NFC_SETTINGS.equals(action)) {
            f = new NfcSettingsFragment();
        } else if (Settings.ACTION_WIRELESS_SETTINGS.equals(action)) {
            f = ConnectivitySettingsFragment.newInstance();
        } else if (Settings.ACTION_DISPLAY_SETTINGS.equals(action)) {
            f = new DisplaySettingsFragment();
        } else if (Settings.ACTION_SOUND_SETTINGS.equals(action)) {
            f = new SoundSettingsFragment();
        } else if (ACTION_WRIST_GESTURES_SETTINGS.equals(action)) {
            f = new GesturesSettingsFragment();
        } else if (Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS.equals(action)) {
            f = new AppsSettingsFragment();
        } else if (Settings.ACTION_APPLICATION_SETTINGS.equals(action)) {
            f = new AppsSettingsFragment();
        } else if (Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS.equals(action)) {
            f = DeveloperOptionsFragment.newInstance();
        } else if (Settings.ACTION_BATTERY_SAVER_SETTINGS.equals(action)) {
            f = BatterySaverFragment.newInstance();
            int defaultLevel = getResources().getInteger(R.integer.config_lowPowerModeTriggerLevel);
            if (defaultLevel == 0 && !BatterySaverUtil.useTwm(this)) {
                finish();
            }
        } else if (ACTION_ENTER_TWM.equals(action)) {
            AcceptDenyDialog dialog = new AcceptDenyDialog(this);
            dialog.setTitle(R.string.pref_batterySaver_dialogTitle);
            dialog.setMessage(BatterySaverUtil.getSaverDialogMessage(this));
            dialog.setPositiveButton(new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    BatterySaverUtil.startBatterySaver(true, MainSettingsActivity.this,
                        MainSettingsActivity.this.getSystemService(PowerManager.class));
                    MainSettingsActivity.this.startActivity(
                        new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME));
                }
            });
            dialog.setNegativeButton(new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    MainSettingsActivity.this.finish();
                }
            });
            dialog.setOnCancelListener(new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    MainSettingsActivity.this.finish();
                }
            });
            getWindow().getDecorView().setBackgroundResource(android.R.color.transparent);
            dialog.show();
            return;
        } else if (ACTION_TTS_SETTINGS.equals(action)) {
            // Used by the Quick Settings menu when Accessibility is turned on.
            f = new TtsFragment();
        } else if (Settings.ACTION_ACCESSIBILITY_SETTINGS.equals(action)) {
            f = new AccessibilityFragment();
        } else if (Settings.ACTION_BLUETOOTH_SETTINGS.equals(action)) {
            f = new BluetoothSettingsFragment();
        } else if (Settings.ACTION_MANAGE_WRITE_SETTINGS.equals(action)
                || Settings.ACTION_MANAGE_OVERLAY_PERMISSION.equals(action)) {
            if (intent.getData() != null) {
                // If there is a package specified, send to detailed page.
                f = new AdvancedAppSettingsFragment();
            } else {
                // Otherwise send to general page
                f = new AppsSettingsFragment();
            }
        } else if (ButtonSettingsFragment.ACTION_BUTTON_SETTINGS.equals(action)) {
            f = new ButtonSettingsFragment();
        } else if (ExerciseDetectionSettingsFragment
                .ACTION_EXERCISE_DETECTION_SETTINGS.equals(action)) {
            f = new ExerciseDetectionSettingsFragment();
        } else if (Settings.ACTION_APPLICATION_DETAILS_SETTINGS.equals(action)) {
            f = new AppDetailsFragment();
        } else if (Settings.ACTION_PRIVACY_SETTINGS.equals(action)) {
            f = new SystemSettingsFragment();
        } else if (Settings.ACTION_DEVICE_INFO_SETTINGS.equals(action)) {
            f = new DeviceInfoFragment();
        } else if (Settings.ACTION_SYNC_SETTINGS.equals(action)
                || Settings.ACTION_ADD_ACCOUNT.equals(action)) {
            f = new AccountSettingsFragment();
        } else if (Settings.ACTION_DATE_SETTINGS.equals(action)) {
            f = new DateTimeSettingsFragment();
        } else if (StorageManager.ACTION_MANAGE_STORAGE.equals(action)) {
            f = new AppStorageSettingsFragment();
        } else if (Settings.ACTION_USAGE_ACCESS_SETTINGS.equals(action)) {
            f = new UsageAccessAppsListFragment();
        } else if (Settings.ACTION_ENTERPRISE_PRIVACY_SETTINGS.equals(action)) {
            f = new EnterprisePrivacySettingsFragment();
        } else if (Settings.ACTION_SECURITY_SETTINGS.equals(action)) {
            f = new SecuritySettingsFragment();
        } else if (DoNotDisturbSettingsFragment.ACTION_DO_NOT_DISTURB_SETTINGS.equals(action)) {
          f = new DoNotDisturbSettingsFragment();
        } else {
            f = new MainSettingsFragment();
        }

        startPreferenceFragment(f, false);
    }

    private void initFeatureManager() {
        int bluetoothMode = DefaultBluetoothModeManager.INSTANCE.get(this).getBluetoothMode();
        if (bluetoothMode != SettingsContract.BLUETOOTH_MODE_UNKNOWN) {
            boolean iosMode = bluetoothMode == SettingsContract.BLUETOOTH_MODE_ALT;
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "initFeatureManager: iosMode=" + iosMode);
            }
            FeatureManager.INSTANCE.get(this).setIosMode(iosMode);
        }
    }

    /** The main settings fragment for the Settings App */
    public static class MainSettingsFragment extends SettingsPreferenceFragment {
        private boolean mDeveloperOptionAdded;
        private ContentResolver mContentResolver;
        private ContentObserver mDeveloperOptionObserver;
        private boolean mSuggestedSettingsOn;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            mContentResolver = getContext().getContentResolver();

            final boolean setupWizardCompleted = Settings.System.getInt(
                    mContentResolver, Settings.System.SETUP_WIZARD_HAS_RUN, 0) == 1;

            SettingsContentResolver settingsContentResolver =
                    new DefaultSettingsContentResolver(mContentResolver);
            boolean inRetail = RetailModeUtil.isInRetailMode(settingsContentResolver);

            if (setupWizardCompleted && !inRetail) {
                addPreferencesFromResource(R.xml.prefs_main);
                initSoundNotification(findPreference(KEY_PREF_SOUND_NOTIFICATION));
                initSuggestedSettings();
                initGeneralSettings(findPreference(KEY_PREF_GENERAL));
            } else {
                addPreferencesFromResource(R.xml.prefs_main_prepair);
                initBeforePairing();
            }

            addDeveloperOptionsIfNecessary();

            mDeveloperOptionObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    addDeveloperOptionsIfNecessary();
                }
            };
            mContentResolver.registerContentObserver(
                    Settings.Global.getUriFor(Settings.Global.DEVELOPMENT_SETTINGS_ENABLED),
                    false,
                    mDeveloperOptionObserver);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            getContext().getContentResolver().unregisterContentObserver(mDeveloperOptionObserver);
        }

        protected void initSuggestedSettings() {
            mSuggestedSettingsOn = false;

            // Battery Saver Mode
            AcceptDenySwitchPreference batterySaverPref =
                    (AcceptDenySwitchPreference)findPreference(KEY_BATTERY_SAVER);

            if (batterySaverPref != null) {
                BatteryManager batteryManager = (BatteryManager)getActivity()
                        .getSystemService(BATTERY_SERVICE);
                PowerManager powerManager =
                        (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
                initSuggestedSettingBatterySaverMode(batterySaverPref,
                        mContentResolver, batteryManager, powerManager);

                // Divider between Suggested Settings and Main Settings
                Preference dividerPref = findPreference(KEY_DIVIDER);
                if (!mSuggestedSettingsOn && dividerPref != null) {
                    getPreferenceScreen().removePreference(dividerPref);
                }
            }
        }

        protected void initSuggestedSettingBatterySaverMode(AcceptDenySwitchPreference pref,
                ContentResolver contentResolver, BatteryManager batteryManager, PowerManager powerManager) {
            if (pref == null) {
                return;
            }

            pref.setChecked(powerManager.isPowerSaveMode());
            pref.setDialogTitle(R.string.pref_batterySaver_dialogTitle);

            String messageText = getString(BatterySaverUtil.useTwm(getContext()) ?
                    R.string.pref_batterySaverScr_dialogMessage
                    : R.string.pref_batterySaver_dialogMessage);
            SpannableString dialogMessage = new SpannableString(messageText);
            dialogMessage.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_NORMAL),
                    0, dialogMessage.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            pref.setDialogMessage(dialogMessage);

            pref.setOnPreferenceChangeListener((p, newVal) -> {
                BatterySaverUtil.startBatterySaver((Boolean) newVal, getContext(), powerManager);
                pref.setChecked((Boolean) newVal);
                return true;
            });

            final int lowPowerModeTriggerLevel = Settings.Global.getInt(
                    contentResolver,
                    Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL, 0);
            int batteryPercentage = batteryManager
                    .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

            if (batteryPercentage > lowPowerModeTriggerLevel || !powerManager.isPowerSaveMode()
                || BatterySaverUtil.useTwm(getContext())) {
                getPreferenceScreen().removePreference(pref);
            } else {
                mSuggestedSettingsOn = true;
            }
        }

        protected void initSoundNotification(Preference pref) {
            if (!getContext().getPackageManager().hasSystemFeature(
                    PackageManager.FEATURE_AUDIO_OUTPUT)) {
                getPreferenceScreen().removePreference(pref);
            }
        }

        protected void initBeforePairing() {
            initEmergencyDialer(findPreference(KEY_PREF_EMERGENCY_DIALER));
            initMediaVolume(findPreference(KEY_PREF_MEDIA_VOLUME));
            initWifi(findPreference(KEY_PREF_WIFI));
        }

        protected void initEmergencyDialer(Preference pref) {
            if (Utils.isEmergencyCallSupported(getActivity())) {
                pref.setIntent(new Intent()
                        .setAction(getString(R.string.emergency_call_action))
                        .setPackage(getString(R.string.emergency_dialer_package))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            } else {
                getPreferenceScreen().removePreference(pref);
            }
        }

        protected void initMediaVolume(Preference pref) {
            if (!getContext().getPackageManager().hasSystemFeature(
                    PackageManager.FEATURE_AUDIO_OUTPUT)) {
                getPreferenceScreen().removePreference(pref);
            }
        }

        protected void initWifi(Preference pref) {
            if (!getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI)) {
                getPreferenceScreen().removePreference(pref);
            }
        }

        private void addDeveloperOptionsIfNecessary() {
            if (!mDeveloperOptionAdded
                    && Settings.Global.getInt(getContext().getContentResolver(),
                            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0) {
                Preference p = new Preference(getContext());
                p.setKey("pref_developerOptions");
                p.setTitle(R.string.development_settings_title);
                p.setIcon(R.drawable.ic_cc_settings_developer);
                p.setFragment(DeveloperOptionsFragment.class.getCanonicalName());
                getPreferenceScreen().addPreference(p);
                mDeveloperOptionAdded = true;
            }
        }

        protected void initGeneralSettings(Preference preference) {
            // Check if there exists an activity to handle the intent.
            PackageManager pm = getContext().getPackageManager();
            if (pm.queryIntentActivities(preference.getIntent(), 0).isEmpty()) {
                getPreferenceScreen().removePreference(preference);
            }
        }
    }
}
