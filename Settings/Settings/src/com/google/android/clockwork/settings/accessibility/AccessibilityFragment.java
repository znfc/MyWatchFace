package com.google.android.clockwork.settings.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.admin.DevicePolicyManager;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.support.wearable.preference.AcceptDenySwitchPreference;
import android.support.wearable.view.AcceptDenyDialog;
import android.support.wearable.view.WearableDialogHelper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.text.TextUtils;
import android.util.ArraySet;
import android.view.accessibility.AccessibilityManager;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settingslib.accessibility.AccessibilityUtils;
import com.google.android.clockwork.host.GKeys;
import com.google.android.clockwork.settings.BatterySaverUtil;
import com.google.common.collect.Sets;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;
import com.google.android.clockwork.settings.common.SettingsPreferenceUtil;
import com.google.android.clockwork.settings.accessibility.tts.TtsFragment;

import java.util.List;
import java.util.Set;

/**
 * Shows Settings related to Accessibility.
 */
public class AccessibilityFragment extends SettingsPreferenceFragment {
    private static String KEY_COLOR_INVERSION = "pref_accessibility_colorInversion";
    private static String KEY_LARGE_TEXT = "pref_accessibility_largeText";
    private static String KEY_MAGNIFICATION = "pref_accessibility_magnification";
    private static String KEY_TTS = "pref_accessibility_tts";
    private static String KEY_SERVICE_PREFIX = "pref_accessibility_service_";
    private static String KEY_SERVICE_LOGGING = "pref_accessibility_service_selected";
    private static String KEY_SIDE_BUTTON = "pref_accessibility_sideButton";

    private static final float LARGE_FONT_SCALE = 1.3f;
    private static final float DEFAULT_FONT_SCALE = 1f;

    private static final String TALKBACK_LABEL = "TalkBack";
    private static final Set<String> EXPERIMENTAL_SERVICES = new ArraySet<>();
    static {
        EXPERIMENTAL_SERVICES.add(TALKBACK_LABEL);
        EXPERIMENTAL_SERVICES.add("Switch Access");
    };

    // Extras passed to sub-fragments.
    static final String EXTRA_TITLE = "title";
    static final String EXTRA_COMPONENT_NAME = "component_name";
    static final String EXTRA_ENABLED = "enabled";
    static final String EXTRA_SERVICE_INFO = "service_info";
    static final String EXTRA_SETTINGS_COMPONENT_NAME = "settings_component_name";
    static final String EXTRA_SUMMARY = "summary";

    private final Configuration mCurConfig = new Configuration();
    private DevicePolicyManager mDpm;
    private ContentResolver mContentResolver;

    /**
     * In order to have the Services in between some Settings, we need to set Order on all of the
     * Prefs. We will determine this value below based on how many Prefs need to be present above
     * the Dynamic list. For example: Magnification, Color inversion, Large Text, TTS are above
     * Accessibility Services.
     */
    private int mDynamicListOrderStart;
    private int mDynamicListOrderEnd;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDpm = (DevicePolicyManager) getContext().getSystemService(Context.DEVICE_POLICY_SERVICE);
        mContentResolver = getContext().getContentResolver();

        addPreferencesFromResource(R.xml.prefs_accessibility);
        addPreferencesFromResource(R.xml.prefs_accessibility_customization);

        mDynamicListOrderStart = 0;
        setupColorInversionSetting((SwitchPreference) findPreference(KEY_COLOR_INVERSION));
        setupLargeTextSetting((AcceptDenySwitchPreference) findPreference(KEY_LARGE_TEXT));
        setupMagnificationSetting((AcceptDenySwitchPreference) findPreference(KEY_MAGNIFICATION));
        setupTtsSetting(findPreference(KEY_TTS));
        setupSideButtonSetting((SwitchPreference) findPreference(KEY_SIDE_BUTTON));
    }

    @Override
    public void onResume() {
        super.onResume();
        // When we come back to this Activity after changing Setting in Accessibility Service,
        // redraw this to reflect those changes.
        setupAccessibilityServices(getPreferenceScreen());
        // Reset ordering of items below Accessibility Services
        resetOrdering();
    }

    @Override
    public void onPause() {
        disableAllPrefsWithKey(getPreferenceScreen(), KEY_SERVICE_PREFIX);
        super.onPause();
    }

    private void setupColorInversionSetting(final SwitchPreference colorInversionPref) {
        if (!GKeys.SHOW_ACCESSIBILITY_SETTING_COLOR_INVERSION.get()) {
            getPreferenceScreen().removePreference(colorInversionPref);
        } else {
            colorInversionPref.setOrder(mDynamicListOrderStart++);
            colorInversionPref.setChecked(isColorInversionOn());
            colorInversionPref.setOnPreferenceChangeListener((p, newVal) -> {
                Settings.Secure.putInt(mContentResolver,
                        Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED,
                        ((Boolean) newVal) ? 1 : 0);
                return true;
            });
        }
    }

    private void setupLargeTextSetting(final AcceptDenySwitchPreference largeTextPref) {
        if (!GKeys.SHOW_ACCESSIBILITY_SETTING_LARGE_TEXT.get()) {
            getPreferenceScreen().removePreference(largeTextPref);
        } else {
            largeTextPref.setOrder(mDynamicListOrderStart++);
            boolean isLargeTextOn = isLargeTextOn();
            largeTextPref.setChecked(isLargeTextOn);
            resetLargeTextPrefUi(largeTextPref, isLargeTextOn);
            largeTextPref.setOnPreferenceChangeListener((p, newVal) -> {
                Boolean newPref = (Boolean) newVal;
                mCurConfig.fontScale = newPref ? LARGE_FONT_SCALE : DEFAULT_FONT_SCALE;
                try {
                    ActivityManager.getService().updatePersistentConfiguration(
                            mCurConfig);
                } catch (RemoteException re) {
                    // Ignored
                }
                resetLargeTextPrefUi((AcceptDenySwitchPreference) p, newPref);
                return true;
            });
        }
    }

    private void resetLargeTextPrefUi(AcceptDenySwitchPreference pref, boolean val) {
        pref.setDialogTitle(val ? R.string.disable_large_text : R.string.enable_large_text);
    }

    private void setupMagnificationSetting(final AcceptDenySwitchPreference magnificationPref) {
        if (!GKeys.SHOW_ACCESSIBILITY_SETTING_MAGNIFICATION.get()) {
            getPreferenceScreen().removePreference(magnificationPref);
        } else {
            magnificationPref.setOrder(mDynamicListOrderStart++);
            boolean isMagnificationOn = isMagnificationOn();
            magnificationPref.setChecked(isMagnificationOn);
            resetMagnificationPrefUi(magnificationPref, isMagnificationOn);
            magnificationPref.setOnPreferenceChangeListener((p, newVal) -> {
                Boolean newPref = (Boolean) newVal;
                Settings.Secure.putInt(mContentResolver,
                        Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED,
                        newPref ? 1 : 0);

                resetMagnificationPrefUi((AcceptDenySwitchPreference) p, newPref);
                return true;
            });
        }
    }

    private void resetMagnificationPrefUi(AcceptDenySwitchPreference pref, boolean val) {
        pref.setDialogTitle(val ? R.string.disable_magnification :
                R.string.enable_magnification);
        if (!val) {
            pref.setDialogMessage(R.string.magnification_subtitle);
        } else {
            pref.setDialogMessage(null);
        }
    }

    private void setupAccessibilityServices(PreferenceScreen allPrefs) {
        final Context context = getContext();
        final PackageManager packageManager = context.getPackageManager();

        // Remove all Services prefs, could get called multiple times from onResume().
        SettingsPreferenceUtil.removeAllPrefsWithKey(allPrefs, KEY_SERVICE_PREFIX);

        // Get accessibility services information
        AccessibilityManager accessibilityManager = AccessibilityManager.getInstance(context);
        List<AccessibilityServiceInfo> installedServices =
                accessibilityManager.getInstalledAccessibilityServiceList();
        Set<ComponentName> enabledServices = AccessibilityUtils.getEnabledServicesFromSettings(
                context);
        List<String> permittedServices = mDpm.getPermittedAccessibilityServices(
                UserHandle.myUserId());
        final boolean accessibilityEnabled = Settings.Secure.getInt(mContentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1;

        // Parse through each accessibility service to display
        for (int i = 0, count = installedServices.size(); i < count; ++i) {
            final AccessibilityServiceInfo info = installedServices.get(i);

            String label = info.getResolveInfo().loadLabel(packageManager).toString();
            final boolean isTalkback = label.equals(TALKBACK_LABEL);
            final String title = munchAccessibilityLabel(context, label);

            ServiceInfo serviceInfo = info.getResolveInfo().serviceInfo;
            final ComponentName componentName = new ComponentName(serviceInfo.packageName,
                    serviceInfo.name);

            // Check if the service is itself enabled
            try { // Need to pull a fresh set of service info
                if (!packageManager.getServiceInfo(componentName, 0).enabled) {
                    continue; // Don't display at all if the service was disabled
                }
            } catch (PackageManager.NameNotFoundException e) {
                continue; // Skip if the service cannot be found
            }

            // Determine if the service was turned on or off
            final boolean serviceEnabled = accessibilityEnabled &&
                    enabledServices.contains(componentName);

            // Hide all accessibility services that are not permitted.
            if (permittedServices != null && !permittedServices.contains(serviceInfo.packageName)) {
                continue; // don't display option, as opposed to just keeping it disabled.
            }

            // Add setting to adapter
            final SwitchPreference servicePref = new SwitchPreference(context);
            servicePref.setOrder(mDynamicListOrderStart + i);
            allPrefs.addPreference(servicePref);
            servicePref.setKey(KEY_SERVICE_PREFIX + title);
            servicePref.getExtras().putString(EXTRA_LOGGING_KEY, KEY_SERVICE_LOGGING);
            servicePref.setTitle(title);
            servicePref.setChecked(serviceEnabled); // needs to be set after preference is added

            servicePref.setOnPreferenceChangeListener((p, newVal) -> {
                // Build the dialog for this service to enable/disable/etc.
                WearableDialogHelper.DialogBuilder builder =
                        new WearableDialogHelper.DialogBuilder(context);
                builder.setTitle(title);

                // Use default description if none provided
                String description = info.loadDescription(packageManager);
                if (TextUtils.isEmpty(description)) {
                    description = getString(R.string.accessibility_service_default_description);
                }
                builder.setMessage(description);

                // Use the negative button to toggle between enabled or disabled.
                if ((Boolean) newVal) {
                    // new value is true, user wants to enable
                    builder.setNegativeIcon(R.drawable.action_accept);
                    builder.setNegativeButton(R.string.enable_text, (dialog, which) -> {
                        // create and show dialog for user to accept before enabling
                        AcceptDenyDialog d = new AcceptDenyDialog(context);
                        d.setTitle(title);
                        d.setMessage(generateContent(info, title));
                        d.setCancelable(false);
                        d.setNegativeButton((diag, j) -> { /* do nothing */ });
                        d.setPositiveButton((diag, j) -> {
                            AccessibilityUtils.setAccessibilityServiceState(
                                    context, componentName, true);
                            servicePref.setChecked(true);
                            if (isTalkback) {
                                BatterySaverUtil
                                        .onTalkbackChanged(context, /*talkbackEnabled= */ true);
                            }
                        });
                        d.show();
                    });
                } else {
                    // new value is true, user wants to disable
                    builder.setNegativeIcon(R.drawable.action_no_thanks);
                    builder.setNegativeButton(R.string.disable_text, (dialog, which) -> {
                        AccessibilityUtils.setAccessibilityServiceState(
                                context, componentName, false);
                        servicePref.setChecked(false);
                        if (isTalkback) {
                            BatterySaverUtil
                                    .onTalkbackChanged(context, /*talkbackEnabled= */ false);
                        }
                    });
                }

                // only include settings if one was specified
                String settingsClassName = info.getSettingsActivityName();
                if (!TextUtils.isEmpty(settingsClassName)) {
                    final ComponentName settingsComponent = new ComponentName(
                            info.getResolveInfo().serviceInfo.packageName, settingsClassName);
                    builder.setNeutralIcon(R.drawable.action_settings);
                    builder.setNeutralButton(R.string.accessibility_menu_item_settings,
                            (dialog, which) -> startActivity(
                                    new Intent(Intent.ACTION_MAIN).setComponent(
                                            settingsComponent)));
                }

                builder.show();
                return false; // false so that the switch won't actually change state
            });
        }
        mDynamicListOrderEnd = installedServices.size();
    }

    @VisibleForTesting
    String munchAccessibilityLabel(Context context, String label) {
        // Mark as experimental, if it is in the experimental list.
        if (EXPERIMENTAL_SERVICES.contains(label)) {
            label = context.getString(R.string.accessibility_service_experimental, label);
        }

        return label;
    }

    private void setupTtsSetting(Preference ttsPreference) {
        if (TtsFragment.getDisplayedEnginesCount(getContext()) <= 0) {
            getPreferenceScreen().removePreference(ttsPreference);
        } else {
            ttsPreference.setOrder(mDynamicListOrderStart++);
        }
    }

    private void setupSideButtonSetting(SwitchPreference sideButtonPref) {
        if (!getContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_AUDIO_OUTPUT)) {
            getPreferenceScreen().removePreference(sideButtonPref);
        } else {
            sideButtonPref.setChecked(isSideButtonEnabled());
            sideButtonPref.setOnPreferenceChangeListener((p, newVal) -> {
                Settings.Secure.putInt(mContentResolver,
                        Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR,
                        (Boolean) newVal ? Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP
                                : Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_DEFAULT);
                return true;
            });
        }
    }

    private boolean isColorInversionOn() {
        return Settings.Secure.getInt(mContentResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, 0) == 1;
    }

    private boolean isLargeTextOn() {
        try {
            mCurConfig.updateFrom(ActivityManager.getService().getConfiguration());
        } catch (RemoteException re) {
            // Ignored
        }
        return mCurConfig.fontScale > DEFAULT_FONT_SCALE;
    }

    private boolean isMagnificationOn() {
        return Settings.Secure.getInt(mContentResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, 0) == 1;
    }

    private boolean isSideButtonEnabled() {
        return Settings.Secure.getInt(mContentResolver,
                Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR,
                Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_DEFAULT) ==
                Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP;
    }

    /**
     * Reset ordering number of all elements below the Accessibility Services.
     */
    private void resetOrdering() {
        Preference pref = findPreference(KEY_SIDE_BUTTON);
        if (pref != null) {
            pref.setOrder(mDynamicListOrderStart + mDynamicListOrderEnd);
        }
    }

    /**
     * Finds all the {@link Preference} in {@param prefGroup} that match {@param key} and disables
     * them.
     */
    private void disableAllPrefsWithKey(PreferenceGroup prefGroup, String key) {
        // Figure out all the prefs that match the criteria.
        for (int i = 0, count = prefGroup.getPreferenceCount(); i < count; i++) {
            Preference pref = prefGroup.getPreference(i);
            if ((pref.getKey() != null) && (pref.getKey().startsWith(key))) {
                pref.setEnabled(false);
            }
        }
    }

    /** Generates the text content to show in the enable accessiblity dialog. */
    private CharSequence generateContent(AccessibilityServiceInfo info, String title) {
        // Insert warning for user here about encryption should clockwork ever add encryption.
        // See AOSP code for ToggleAccessibilityServicePreferenceFragment for details.

        SpannableStringBuilder ssb =
                new SpannableStringBuilder(getString(R.string.capabilities_list_title, title));

        // This capability is implicit for all services.
        ssb.append('\n');
        ssb.append(
                getString(R.string.capability_title_receiveAccessibilityEvents),
                new StyleSpan(Typeface.BOLD), // just bold the headers
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.append('\n');
        ssb.append(getString(R.string.capability_desc_receiveAccessibilityEvents));

        // Service specific capabilities.
        for (AccessibilityServiceInfo.CapabilityInfo capability : info.getCapabilityInfos()) {
            ssb.append('\n');
            ssb.append(
                    getString(capability.titleResId),
                    new StyleSpan(Typeface.BOLD), // just bold the headers
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.append('\n');
            ssb.append(getString(capability.descResId));
        }

        return ssb;
    }
}
