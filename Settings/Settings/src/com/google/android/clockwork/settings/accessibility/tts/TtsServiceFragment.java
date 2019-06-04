package com.google.android.clockwork.settings.accessibility.tts;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TtsEngines;
import android.speech.tts.Voice;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Set;

/**
 * Settings fragment which handles settings of a specific text-to-speech engine.
 * <p>
 * Loads a specified TTS engine, and then checks the default locale/language for the TTS engine.
 * Will also trigger a voice integrity check. Updates activity based off of the data from the voice
 * integrity check.
 */
public class TtsServiceFragment extends SettingsPreferenceFragment {
    private static final String TAG = "TtsServiceFragment";
    private static final boolean DBG = true;

    private static String KEY_LANGUAGE = "pref_accessibility_tts_engine_language";
    private static String KEY_DEFAULT = "pref_accessibility_tts_engine_default";
    private static String KEY_LISTEN_TO_SAMPLE = "pref_accessibility_tts_engine_listenToSample";

    /** Key for extra that contains a list of the available locales for the current TTS engine. */
    static final String EXTRA_AVAILABLE_LOCALES = "locales";

    /** Result code for sample text results. */
    private static final int GET_SAMPLE_TEXT = 1983;
    /** Result code for voice data integrity check results. */
    private static final int VOICE_DATA_INTEGRITY_CHECK = 1977;

    /** Helper to work with TTS engines. */
    private TtsEngines mEnginesHelper;
    /** Intent specified by the engine of the Intent to pull up the Settings of the engine. */
    @Nullable
    private Intent mEngineSettingsIntent = null;
    /** The TTS engine object created from {@link #mEngine}. */
    private TextToSpeech mTts;
    /** The TTS engine used in the service activity. */
    private String mEngine;
    /** The sample text to speak. */
    @Nullable
    private String mSampleText = null;

    /** Default locale used by selected TTS engine, null if not connected to any engine. */
    @Nullable
    private Locale mCurrentDefaultLocale;

    /**
     * List of available locals of selected TTS engine, as returned by
     * {@link TextToSpeech.Engine#ACTION_CHECK_TTS_DATA} activity. If empty, then activity
     * was not yet called.
     */
    private ArrayList<String> mAvailableStrLocals;

    /** The initialization listener used when we are initalizing the engine. */
    private final TextToSpeech.OnInitListener mInitListener = new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(int status) {
            onInitEngine(status);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mEnginesHelper = new TtsEngines(getContext().getApplicationContext());

        getActivity().setVolumeControlStream(TextToSpeech.Engine.DEFAULT_STREAM);
        mEngine = getArguments().getString(TtsFragment.EXTRA_NAME);

        mTts = new TextToSpeech(getContext().getApplicationContext(), mInitListener, mEngine);

        mEngineSettingsIntent = mEnginesHelper.getSettingsIntent(mEngine);

        addPreferencesFromResource(R.xml.prefs_accessibility_tts_engine);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSettings();
    }

    @Override
    public void onDestroy() {
        if (mTts != null) {
            mTts.shutdown();
            mTts = null;
        }
        super.onDestroy();
    }

    /**
     * Update the displayed Settings options. Used when a setting has changed, necessitating an UI
     * update.
     */
    private void updateSettings() {
        Voice voice = mTts.getVoice(); // check if the TTS engine has initialized and can talk

        // TODO: reenable when GoogleTTS engine has a Wear-compatible settings activity.
//        if (mEngineSettingsIntent != null) {
//            mAdapter.addSetting(
//                    getString(R.string.tts_engine_settings_section),
//                    R.drawable.ic_cc_settings,
//                    mEngineSettingsIntent);
//        }

        // Language Settings
        ListPreference languagePref = (ListPreference) findPreference(KEY_LANGUAGE);
        if (mAvailableStrLocals != null && mAvailableStrLocals.size() > 0) {
            languagePref.setEnabled(true);
            initLanguage(languagePref);
        } else {
            languagePref.setEnabled(false);
        }

        // Install TTS Data
        // TODO: reenable when GoogleTTS engine has a Wear-compatible settings activity.
//        mAdapter.addSetting(
//                getString(R.string.tts_install_data_title),
//                R.drawable.ic_settings_download,
//                new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).setPackage(mEngine));

        Preference samplePref = findPreference(KEY_LISTEN_TO_SAMPLE);
        if (voice == null) {
            samplePref.setEnabled(false);
        } else {
            samplePref.setEnabled(true);
            samplePref.setOnPreferenceClickListener((p) -> {
                // Get the sample text from the TTS engine; onActivityResult will do
                // the actual speaking
                speakSampleText();
                return true;
            });
        }

        CheckBoxPreference defaultPref = (CheckBoxPreference) findPreference(KEY_DEFAULT);
        boolean isDefault = TextUtils.equals(mEnginesHelper.getDefaultEngine(), mEngine);
        defaultPref.setChecked(isDefault);
        if (mEnginesHelper.getEngines().size() > 1) {
            defaultPref.setEnabled(true);
            defaultPref.setOnPreferenceChangeListener((p, newVal) -> {
                if ((Boolean) newVal) {
                    // TODO: Add check to ensure engine and locale is configured and loaded
                    Settings.Secure.putString(getContext().getContentResolver(),
                            Settings.Secure.TTS_DEFAULT_SYNTH, mEngine);
                }
                return true;
            });
        } else {
            // There is only one engine, no need to disable it.
            defaultPref.setEnabled(false);
        }
    }

    /**
     * Called when the TTS engine is initialized.
     */
    public void onInitEngine(int status) {
        if (status == TextToSpeech.SUCCESS) {
            if (DBG) Log.d(TAG, "TTS engine for settings screen initialized.");
            checkVoiceData(mEngine);
            checkDefaultLocale();
        } else {
            if (DBG) Log.d(TAG, "TTS engine for settings screen failed to initialize successfully.");
        }
    }

    /**
     * Check the default locale and request the sample text if it is valid. Should be called on init
     * and whenever language/locale is changed.
     */
    private void checkDefaultLocale() {
        Voice defaultVoice = null;
        Locale defaultLocale = null;

        // Check if a default locale is available
        if ((defaultVoice = mTts.getDefaultVoice()) == null
                || (defaultLocale = defaultVoice.getLocale()) == null) {
            Log.e(TAG, "Failed to get default language from engine " + mTts.getCurrentEngine());
            updateSettings();
            updateEngineStatus(R.string.tts_status_not_supported);
            return;
        }

        // ISO-3166 alpha 3 country codes are out of spec. If we won't normalize,
        // we may end up with English (USA)and German (DEU).
        final Locale oldDefaultLocale = mCurrentDefaultLocale;
        mCurrentDefaultLocale = mEnginesHelper.parseLocaleString(defaultLocale.toString());
        if (!Objects.equals(oldDefaultLocale, mCurrentDefaultLocale)) {
            mSampleText = null;
        }

        int defaultAvailable = mTts.setLanguage(defaultLocale);
        if (evaluateDefaultLocale() && mSampleText == null) {
            getSampleText();
        }
    }

    /**
     * @return {@code true} if the current default is supported by the TTS engine.
     */
    private boolean evaluateDefaultLocale() {
        // Check if we are connected to the engine, and CHECK_VOICE_DATA returned list
        // of available languages.
        if (mCurrentDefaultLocale == null || mAvailableStrLocals == null) {
            return false;
        }

        boolean notInAvailableLanguages = true;
        try {
            // Check if language is listed in CheckVoices Action result as available voice.
            String defaultLocaleStr = mCurrentDefaultLocale.getISO3Language();
            if (!TextUtils.isEmpty(mCurrentDefaultLocale.getISO3Country())) {
                defaultLocaleStr += "-" + mCurrentDefaultLocale.getISO3Country();
            }
            if (!TextUtils.isEmpty(mCurrentDefaultLocale.getVariant())) {
                defaultLocaleStr += "-" + mCurrentDefaultLocale.getVariant();
            }

            for (String loc : mAvailableStrLocals) {
                if (loc.equalsIgnoreCase(defaultLocaleStr)) {
                    notInAvailableLanguages = false;
                    break;
                }
            }
        } catch (MissingResourceException e) {
            if (DBG) Log.wtf(TAG, "MissingResourceException", e);
            updateEngineStatus(R.string.tts_status_not_supported);
            updateSettings();
            return false;
        }

        int defaultAvailable = mTts.setLanguage(mCurrentDefaultLocale);
        // update displayed status and settings based on the new language/locale set
        if (defaultAvailable == TextToSpeech.LANG_NOT_SUPPORTED ||
                defaultAvailable == TextToSpeech.LANG_MISSING_DATA ||
                notInAvailableLanguages) {
            if (DBG) Log.d(TAG, "Default locale for this TTS engine is not supported.");
            updateEngineStatus(R.string.tts_status_not_supported);
            updateSettings();
            return false;
        } else {
            if (isNetworkRequiredForSynthesis()) {
                updateEngineStatus(R.string.tts_status_requires_network);
            } else {
                updateEngineStatus(R.string.tts_status_ok);
            }
            updateSettings();
            return true;
        }
    }

    /**
     * Ask the current default engine to return a string of sample text to be spoken to the user.
     * Results will be returned to activity with type {@link #GET_SAMPLE_TEXT}.
     */
    private void getSampleText() {
        Intent intent = new Intent(TextToSpeech.Engine.ACTION_GET_SAMPLE_TEXT);

        intent.putExtra("language", mCurrentDefaultLocale.getLanguage());
        intent.putExtra("country", mCurrentDefaultLocale.getCountry());
        intent.putExtra("variant", mCurrentDefaultLocale.getVariant());
        intent.setPackage(mEngine);

        try {
            if (DBG) Log.d(TAG, "Getting sample text: " + intent.toUri(0));
            startActivityForResult(intent, GET_SAMPLE_TEXT);
        } catch (ActivityNotFoundException ex) {
            Log.e(TAG, "Failed to get sample text, no activity found for " + intent + ")");
        }
    }

    /**
     * Redirect activity results to methods to handle the result.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case GET_SAMPLE_TEXT:
                onSampleTextReceived(resultCode, data);
                return;
            case VOICE_DATA_INTEGRITY_CHECK:
                onVoiceDataIntegrityCheckDone(data);
                return;
        }
    }

    /**
     * @return fallback default sample string for the engine to speak.
     */
    @Nullable
    private String getDefaultSampleString() {
        Voice voice = null;
        Locale locale = null;
        if (mTts != null && (voice = mTts.getVoice()) != null
                && (locale = voice.getLocale()) != null) {
            try {
                final String currentLang = locale.getISO3Language();
                Resources res = getResources();
                String[] strings = res.getStringArray(R.array.tts_demo_strings);
                String[] langs = res.getStringArray(R.array.tts_demo_string_langs);

                for (int i = 0; i < strings.length; ++i) {
                    if (langs[i].equals(currentLang) && !TextUtils.isEmpty(strings[i])) {
                        return strings[i];
                    }
                }
            } catch (MissingResourceException e) {
                if (DBG) Log.wtf(TAG, "MissingResourceException", e);
                // Ignore and fall back to default sample string
            }
        }
        return getString(R.string.tts_default_sample_string);
    }

    /**
     * @return {@code true} if the currently set {@link android.speech.tts.Voice} for the engine
     * requires network to synthesize speech.
     */
    private boolean isNetworkRequiredForSynthesis() {
        Voice voice = mTts.getVoice();
        if (voice == null) {
            return false;
        }
        Set<String> features = voice.getFeatures();
        if (features == null) {
            return false;
        }
        return features.contains(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS) &&
                !features.contains(TextToSpeech.Engine.KEY_FEATURE_EMBEDDED_SYNTHESIS);
    }

    /**
     * Process result from sample text request.
     *
     * @param resultCode the result code from the sample text request.
     * @param data the data from the sample text request.
     * @see android.speech.tts.TextToSpeech.Engine#ACTION_GET_SAMPLE_TEXT
     */
    private void onSampleTextReceived(int resultCode, Intent data) {
        String sample = null;

        if (resultCode == TextToSpeech.LANG_AVAILABLE && data != null) {
            if (data != null && data.getStringExtra("sampleText") != null) {
                sample = data.getStringExtra("sampleText");
            }
            if (DBG) Log.d(TAG, "Got sample text: " + sample);
        } else {
            if (DBG) Log.d(TAG, "Using default sample text :" + sample);
        }

        // fall back onto default sample string if not given sample string from engine.
        if (sample == null) {
            sample = getDefaultSampleString();
        }

        // only update settings if sample text is available
        mSampleText = sample;
        if (mSampleText != null) {
            updateSettings();
        } else {
            Log.e(TAG, "Did not have a sample string for the requested language. Using default");
        }
    }

    /**
     * Speak the sample text previously obtained.
     */
    private void speakSampleText() {
        if (DBG) Log.d(TAG, "Speaking sample text");
        final boolean networkRequired = isNetworkRequiredForSynthesis();
        if (!networkRequired || networkRequired &&
                (mTts.isLanguageAvailable(mCurrentDefaultLocale) >= TextToSpeech.LANG_AVAILABLE)) {
            mTts.speak(mSampleText, TextToSpeech.QUEUE_FLUSH, null, "Sample");
        } else {
            Log.w(TAG, "Network required for sample synthesis for requested language");
        }
    }

    /**
     * Update the current displayed engine status to the user.
     *
     * @param resourceId the resource ID of the engine status string.
     */
    private void updateEngineStatus(int resourceId) {
        Locale locale = mCurrentDefaultLocale;
        if (locale == null) {
            locale = Locale.getDefault();
        }
        // TODO: Find a better solution than Toasts
        Toast.makeText(getContext(), getString(resourceId, locale.getDisplayName()),
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Check whether the voice data for the engine is ok. Results will be returned to activity with
     * type {@link #VOICE_DATA_INTEGRITY_CHECK}.
     *
     * @param engine name of the engine that to be checked.
     */
    private void checkVoiceData(@NonNull String engine) {
        Intent intent = new Intent(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        intent.setPackage(engine);
        try {
            if (DBG) Log.d(TAG, "Updating engine: Checking voice data: " + intent.toUri(0));
            startActivityForResult(intent, VOICE_DATA_INTEGRITY_CHECK);
        } catch (ActivityNotFoundException ex) {
            Log.e(TAG, "Failed to check TTS data, no activity found for " + intent + ")");
        }
    }

    /**
     * Process results from voice data integrity check.
     *
     * @param data results from the voice data integrity check.
     */
    private void onVoiceDataIntegrityCheckDone(Intent data) {
        if (DBG) Log.d(TAG, "Voice integrity check: " + data);
        final String engine = mTts.getCurrentEngine();

        if (engine == null) {
            Log.e(TAG, "Voice data check complete, but no engine bound");
            return;
        }

        if (data == null){
            Log.e(TAG, "Engine failed voice data integrity check (null return)" +
                    mTts.getCurrentEngine());
            return;
        }

        Settings.Secure.putString(getContext().getContentResolver(),
                Settings.Secure.TTS_DEFAULT_SYNTH, engine);

        mAvailableStrLocals = data.getStringArrayListExtra(
                TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES);
        if (mAvailableStrLocals == null) {
            Log.e(TAG, "Voice data check complete, but no available voices found");
            // Set mAvailableStrLocals to empty list
            mAvailableStrLocals = new ArrayList<>();
        }
        if (evaluateDefaultLocale()) {
            getSampleText();
        }

        updateSettings();
    }

    private void initLanguage(ListPreference languagePreference) {
        Locale currentLocale = null;
        if (!mEnginesHelper.isLocaleSetToDefaultForEngine(mEngine)) {
            currentLocale = mEnginesHelper.getLocalePrefForEngine(mEngine);
        }

        // First convert from String to Locale, then sort by Locale displayName and then convert
        // to 2 lists of "DisplayName" and "Locale"
        ArrayList<Locale> availableLocales = new ArrayList<>(mAvailableStrLocals.size());

        // For each available language if Locale is parseable, add it to the list.
        if (mAvailableStrLocals != null) {
            for (int i = 0; i < mAvailableStrLocals.size(); i++) {
                Locale locale = mEnginesHelper.parseLocaleString(mAvailableStrLocals.get(i));
                if (locale != null) {
                    availableLocales.add(locale);
                }
            }
        }

        // Sort by Locale display name, instead of available languages.
        Collections.sort(availableLocales, new Comparator<Locale>() {
            @Override
            public int compare(Locale lhs, Locale rhs) {
                return lhs.getDisplayName().compareToIgnoreCase(rhs.getDisplayName());
            }
        });

        CharSequence defaultValue = null;
        CharSequence[] languageNames = new CharSequence[availableLocales.size() + 1];
        CharSequence[] languageLocales = new CharSequence[availableLocales.size() + 1];

        // Add the "Use system language" option.
        languageNames[0] = getString(R.string.tts_lang_use_system);
        languageLocales[0] = "";
        if (mEnginesHelper.isLocaleSetToDefaultForEngine(mEngine)) {
            defaultValue = languageLocales[0];
        }

        for (int i = 0; i < availableLocales.size(); i++) {
            Locale locale = availableLocales.get(i);
            languageNames[i + 1] =locale.getDisplayName();
            languageLocales[i + 1] = locale.toString();
            if (locale.equals(currentLocale)) {
                defaultValue = languageLocales[i + 1];
            }
        }

        languagePreference.setEntries(languageNames);
        languagePreference.setEntryValues(languageLocales);
        languagePreference.setValue(defaultValue.toString());
        languagePreference.setOnPreferenceChangeListener((p, newVal) -> {
            CharSequence localeString = (CharSequence) newVal;
            mEnginesHelper.updateLocalePrefForEngine( // set the locale
                    mEngine, !TextUtils.isEmpty(localeString)
                            ? mEnginesHelper.parseLocaleString(localeString.toString())
                            : null);
            // Will update the state locally.
            checkDefaultLocale();
            return true;
        });
    }
}
