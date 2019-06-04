package com.google.android.clockwork.settings.cellular;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.phone.common.Constants;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;

public class TwinningFragment extends SettingsPreferenceFragment {
    private static final String KEY_PREF_CALL = "pref_call";
    private static final String KEY_PREF_TEXT = "pref_text";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_twinning);

        initCall((SwitchPreference) findPreference(KEY_PREF_CALL));
        initText((SwitchPreference) findPreference(KEY_PREF_TEXT));
    }

    private void initCall(SwitchPreference pref) {
        pref.setChecked(Utils.isCallTwinningEnabled(getContext()));
        pref.setOnPreferenceChangeListener((p, newValue) -> {
            Utils.setCallTwinningState(getContext().getApplicationContext(),
                    (Boolean) newValue ? Constants.STATE_ON : Constants.STATE_OFF, null, null);
            return true;
        });
    }

    private void initText(SwitchPreference pref) {
        pref.setChecked(Utils.isTextTwinningEnabled(getContext()));
        pref.setOnPreferenceChangeListener((p, newValue) -> {
            Utils.setTextTwinningState(getContext().getApplicationContext(),
                    (Boolean) newValue ? Constants.STATE_ON : Constants.STATE_OFF);
            Utils.setTextBridgingState(getContext().getApplicationContext(),
                    (Boolean) newValue ? Constants.STATE_OFF : Constants.STATE_ON);
            return true;
        });
    }
}
