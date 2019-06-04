package com.google.android.clockwork.settings.personal.device_administration;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;

/**
 * Device Administration settings and MDM settings.
 */
public class OverallDeviceAdministrationSettingsFragment extends SettingsPreferenceFragment {

    private static final String KEY_PREF_ADM_SETTINGS = "pref_android_device_manager_settings";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_overall_device_administration);
        Preference admSettingsPref = findPreference(KEY_PREF_ADM_SETTINGS);
        admSettingsPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent admSettingsIntent = new Intent();
                admSettingsIntent.setAction("com.google.android.gms.mdm.MDM_SETTINGS_ACTIVITY");
                startActivityForResult(admSettingsIntent, 0);
                return true;
            }
        });
    }
}
