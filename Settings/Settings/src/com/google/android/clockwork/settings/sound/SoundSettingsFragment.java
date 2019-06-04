package com.google.android.clockwork.settings.sound;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.accessibility.AccessibilityManager;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;

public class SoundSettingsFragment extends SettingsPreferenceFragment {
    private static final String KEY_PREF_RING_VOLUME = "pref_ringVolume";
    private static final String KEY_PREF_ACCESSIBILITY_VOLUME = "pref_accessibilityVolume";
    private static final String KEY_PREF_VIBRATE_FOR_CALLS = "pref_vibrateForCalls";

    private static final String CELL_BROADCAST_RECEIVER_PACKAGE_NAME =
            "com.android.cellbroadcastreceiver";

    private PackageManager mPackageManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPackageManager = getActivity().getPackageManager();

        initSpeakerPreferences();
    }

    protected void initSpeakerPreferences() {
        if (mPackageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_OUTPUT)) {
            addPreferencesFromResource(R.xml.prefs_sound);

            TelephonyManager tm = (TelephonyManager) getActivity()
                    .getSystemService(Context.TELEPHONY_SERVICE);
            initRingVolume((VolumePreference) findPreference(KEY_PREF_RING_VOLUME),
                    tm.isVoiceCapable());
            initVibrateForCalls((SwitchPreference) findPreference(KEY_PREF_VIBRATE_FOR_CALLS));

            initCellularPreferences();

            initAccessibilityPreferences();
        }
    }

    protected void initCellularPreferences() {
        try {
            if (mPackageManager.getPackageInfo(CELL_BROADCAST_RECEIVER_PACKAGE_NAME, 0) != null) {
                addPreferencesFromResource(R.xml.prefs_sound_cellular);
            }
        } catch (PackageManager.NameNotFoundException e) {
            return;
        }
    }

    protected void initAccessibilityPreferences() {
        AccessibilityManager am = (AccessibilityManager)
                getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (!am.isAccessibilityVolumeStreamActive()) {
            Preference preference = findPreference(KEY_PREF_ACCESSIBILITY_VOLUME);
            if (preference != null) {
                getPreferenceScreen().removePreference(preference);
            }
        }
    }

    protected void initRingVolume(VolumePreference pref, boolean voiceCapable) {
        if (voiceCapable) {
            pref.setStream(AudioManager.STREAM_RING);
        }
    }

    protected void initVibrateForCalls(SwitchPreference pref) {
        pref.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.VIBRATE_WHEN_RINGING, 0) != 0);

        pref.setOnPreferenceChangeListener((p, newVal) ->
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.VIBRATE_WHEN_RINGING, ((Boolean) newVal) ? 1 : 0));
    }
}
