package com.google.android.clockwork.settings.personal;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.util.Log;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.utils.FeatureManager;
import java.util.ArrayList;
import java.util.List;

/** Preference for selecting default Voice Assistant provider */
class VoiceAssistantPreference extends ListPreference {

    private static final String TAG = "VoiceAssistantPreference";

    private static final String KEY_PREF_VOICE_ASSISTANT = "pref_voiceAssistant";

    public VoiceAssistantPreference(Context context) {
        super(context);
        init(context);
    }

    public VoiceAssistantPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (FeatureManager.INSTANCE.get(context).isLocalEditionDevice()) {
            init(context);
        }
    }

    private void init(Context context) {
        setTitle(R.string.pref_voiceAssistant);
        setDialogTitle(R.string.pref_voiceAssistant);
        setSummary("%s");
        setIcon(R.drawable.ic_cc_settings_mic_white);

        PackageManager pm = context.getPackageManager();

        final List<String> voiceProviderPackages = new ArrayList<>();
        final List<String> voiceProviderLabels = new ArrayList<>();
        String selectedProvider = null;
        for (String packageName :
                context.getResources()
                        .getStringArray(R.array.config_le_system_voice_assistant_packages)) {
            try {
                ApplicationInfo applicationInfo = pm.getApplicationInfo(packageName, 0);
                String applicationLabel = String.valueOf(pm.getApplicationLabel(applicationInfo));
                /*
                 * All the time only one provider should be enabled. If multiple providers were
                 * enabled here, the first one on the list would be selected.
                 */
                if (applicationInfo.enabled && selectedProvider == null) {
                    selectedProvider = packageName;
                    if (getValue() != null) {
                        Log.e(TAG, "Multiple voice providers are enabled at the same time.");
                    }
                    setValue(selectedProvider);
                }
                voiceProviderPackages.add(packageName);
                voiceProviderLabels.add(applicationLabel);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Voice provider package '" + packageName + "' not found.", e);
            }
        }

        setEntries(voiceProviderLabels.toArray(new String[voiceProviderLabels.size()]));
        setEntryValues(voiceProviderPackages.toArray(new String[voiceProviderPackages.size()]));

        setOnPreferenceChangeListener(
                (p, newValue) -> {
                    if (((ListPreference) p).getValue().equals(newValue)) {
                        return false;
                    }

                    int newProviderIndex = findIndexOfValue((String) newValue);
                    updatePreferredActivities(pm, voiceProviderPackages, newProviderIndex);

                    return true;
                });

        /*
         * Don't write the preference into XML file as it is not used anywhere. Using the XML file
         * causes race conditions and other issues.
         */
        setPersistent(false);
        setNegativeButtonText(null);
    }

    private void updatePreferredActivities(
            PackageManager pm, List<String> providerPackages, int index) {
        for (int i = 0; i < providerPackages.size(); ++i) {
            if (i == index) {
                Log.i(TAG, "Enabling voice provider: " + providerPackages.get(i));
                pm.setApplicationEnabledSetting(
                        providerPackages.get(i), pm.COMPONENT_ENABLED_STATE_ENABLED, 0);
            } else {
                Log.i(TAG, "Disabling voice provider: " + providerPackages.get(i));
                /*
                 * Disabling the application this way hides it also from applications list so user
                 * cannot enable it in Settings.
                 */
                pm.setApplicationEnabledSetting(
                        providerPackages.get(i), pm.COMPONENT_ENABLED_STATE_DISABLED, 0);
            }
        }
    }
}
