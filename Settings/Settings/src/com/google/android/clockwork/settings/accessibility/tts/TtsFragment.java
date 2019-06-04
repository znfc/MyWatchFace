package com.google.android.clockwork.settings.accessibility.tts;

import android.content.Context;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TtsEngines;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;
import com.google.android.clockwork.settings.common.SettingsPreferenceUtil;

/**
 * Settings fragment which handles platform text-to-speech settings.
 */
public class TtsFragment extends SettingsPreferenceFragment {
    private static final String TAG = "TtsFragment";

    private static String KEY_TTS_ENGINE_PREFIX = "pref_accessibility_tts_engine_";
    private static String KEY_TTS_ENGINE_LOGGING = "pref_accessibility_tts_engine_selected";
    private static String KEY_RATES = "pref_accessibility_tts_rates";

    private TtsEngines mEnginesHelper;

    /**
     * In order to have the TTS always in the beginning, we need to set Order on all of the
     * Prefs.
     */
    private int mDynamicListOrderEnd;

    /* package */ static final String EXTRA_NAME = "name";

    public static int getDisplayedEnginesCount(Context ctx) {
        return new TtsEngines(ctx.getApplicationContext()).getEngines().size();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mEnginesHelper = new TtsEngines(getContext().getApplicationContext());
        addPreferencesFromResource(R.xml.prefs_accessibility_tts);

        setupSpeechRate((ListPreference) findPreference(KEY_RATES));
    }

    @Override
    public void onResume() {
        super.onResume();

        setupTtsEngines(getPreferenceScreen());
        resetOrdering();
    }

    private void setupTtsEngines(PreferenceScreen allPrefs) {
        // Grab current engine so we can set the proper flag.
        String defaultEngine = mEnginesHelper.getDefaultEngine();

        // Remove all TTS prefs, could get called multiple times from onResume().
        SettingsPreferenceUtil.removeAllPrefsWithKey(allPrefs, KEY_TTS_ENGINE_PREFIX);

        mDynamicListOrderEnd = 0;
        for (TextToSpeech.EngineInfo engine : mEnginesHelper.getEngines()) {
            Preference enginePref = new Preference(getContext());
            enginePref.setKey(KEY_TTS_ENGINE_PREFIX + engine.label);
            enginePref.setTitle(engine.label);
            if (TextUtils.equals(defaultEngine, engine.name)) {
                enginePref.setIcon(R.drawable.radio_button_selected);
            } else {
                enginePref.setIcon(R.drawable.radio_button_normal);
            }
            enginePref.setFragment(
                    "com.google.android.clockwork.settings.accessibility.tts.TtsServiceFragment");
            enginePref.getExtras().putString(EXTRA_NAME, engine.name);
            enginePref.getExtras().putString(EXTRA_LOGGING_KEY, KEY_TTS_ENGINE_LOGGING);
            allPrefs.addPreference(enginePref);
            enginePref.setOrder(mDynamicListOrderEnd++);
        }
    }

    private void setupSpeechRate(ListPreference ratesPref) {
        // Setup default rate
        int defaultRate;
        try {
            defaultRate = Settings.Secure.getInt(getContext().getContentResolver(),
                    Settings.Secure.TTS_DEFAULT_RATE);
        } catch (Settings.SettingNotFoundException e) {
            // Default rate setting not found, initialize it
            defaultRate = TextToSpeech.Engine.DEFAULT_RATE;
        }

        ratesPref.setValue(Integer.toString(defaultRate));
        ratesPref.setOnPreferenceChangeListener((p, newVal) -> {
            try {
                Settings.Secure.putInt(
                        getContext().getContentResolver(),
                        Settings.Secure.TTS_DEFAULT_RATE,
                        Integer.parseInt((String) newVal));
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist default TTS rate setting", e);
            }
            return true;
        });
    }

    private void resetOrdering() {
        findPreference(KEY_RATES).setOrder(mDynamicListOrderEnd);
    }
}
