package com.google.android.clockwork.settings.apps;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.host.GKeys;
import com.google.android.clockwork.settings.CardPreviewModeConfig;
import com.google.android.clockwork.settings.DefaultMuteWhenOffBodyConfig;
import com.google.android.clockwork.settings.MuteWhenOffBodyConfig;
import com.google.android.clockwork.settings.common.RadioButtonPreference;
import com.google.android.clockwork.settings.common.RadioGroupPreferenceScreenHelper;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;

public class AppsSettingsFragment extends SettingsPreferenceFragment {
    private static final String KEY_PREF_VIBRATION_LEVEL = "pref_vibrationLevel";
    private static final String KEY_PREF_VIBRATION_LEVEL_NORMAL = "pref_vibrationLevel_normal";
    private static final String KEY_PREF_VIBRATION_LEVEL_LONG = "pref_vibrationLevel_long";
    private static final String KEY_PREF_VIBRATION_LEVEL_DOUBLE = "pref_vibrationLevel_double";
    private static final String KEY_PREF_CARD_PREVIEWS = "pref_cardPreviews";
    private static final String KEY_PREF_CARD_PREVIEWS_LOW = "pref_cardPreviews_low";
    private static final String KEY_PREF_CARD_PREVIEWS_NORMAL = "pref_cardPreviews_normal";
    private static final String KEY_PREF_CARD_PREVIEWS_HIGH = "pref_cardPreviews_high";
    private static final String KEY_PREF_APP_NOTIFICATIONS = "pref_appNotifications";
    private static final String KEY_PREF_MUTE_WHEN_OFF_BODY = "pref_muteWhenOffBody";
    private static final String KEY_PREF_VIP_CONTACTS = "pref_vip_contacts";

    // NOTE: this should match the StreamVibrator.VIBRATION_PATTERN_SEPARATOR in the Home app.
    private static final String VIBRATION_PATTERN_SEPARATOR = ",\\s*";

    // Android Wear sensor type for Low-Latency Off-Body sensor.

    private PackageManager mPackageManager;
    private CardPreviewModeConfig mCardPreviewModeConfig;
    private VibrationModeConfig mVibrationModeConfig;
    private Vibrator mVibrator;
    private SensorManager mSensorManager;
    private MuteWhenOffBodyConfig mMuteWhenOffBodyConfig;

    private volatile Integer mPendingVibrationModeToSave = null;

    private final Object mLock = new Object();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_apps);
        addPreferencesFromResource(R.xml.prefs_app_customization);

        mPackageManager = getActivity().getPackageManager();

        mCardPreviewModeConfig = new CardPreviewModeConfig(getActivity());
        mCardPreviewModeConfig.register();

        mVibrationModeConfig = new VibrationModeConfig(getActivity());
        mVibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);

        mSensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        mMuteWhenOffBodyConfig = DefaultMuteWhenOffBodyConfig.getInstance(getContext());

        initVibrationLevel((PreferenceScreen) findPreference(KEY_PREF_VIBRATION_LEVEL));
        initVipContacts(findPreference(KEY_PREF_VIP_CONTACTS));
        initCardPreviews((PreferenceScreen) findPreference(KEY_PREF_CARD_PREVIEWS));

        initAppNotifications(findPreference(KEY_PREF_APP_NOTIFICATIONS));
        initMuteWhenOffBody((SwitchPreference) findPreference(KEY_PREF_MUTE_WHEN_OFF_BODY));
    }

    @Override
    public void onDestroy() {
        mCardPreviewModeConfig.unregister();
        super.onDestroy();
    }

    @Override
    public void onPause() {
        synchronized (mLock) {
            if (mPendingVibrationModeToSave != null) {
                mVibrationModeConfig.setVibrationMode(mPendingVibrationModeToSave);

                mPendingVibrationModeToSave = null;
            }
        }

        super.onPause();
    }

    protected void initAppNotifications(Preference pref) {
        // Check if there exists an activity to handle the intent
        if (mPackageManager.queryIntentActivities(pref.getIntent(), 0).isEmpty()) {
            getPreferenceScreen().removePreference(pref);
        }
    }

    protected void initVibrationLevel(PreferenceScreen pref) {
        if (GKeys.SHOW_VIBRATION_SETTING_PATTERN.get()) {
            ((RadioButtonPreference) pref.findPreference(KEY_PREF_VIBRATION_LEVEL_NORMAL))
                    .setEntryValue(VibrationModeConfig.NORMAL);
            ((RadioButtonPreference) pref.findPreference(KEY_PREF_VIBRATION_LEVEL_LONG))
                    .setEntryValue(VibrationModeConfig.LONG);
            ((RadioButtonPreference) pref.findPreference(KEY_PREF_VIBRATION_LEVEL_DOUBLE))
                    .setEntryValue(VibrationModeConfig.DOUBLE);

            RadioGroupPreferenceScreenHelper helper = new RadioGroupPreferenceScreenHelper(pref);
            helper.enableAutoSummary(pref, null);
            helper.checkByEntryValue(mVibrationModeConfig.getVibrationMode());

            helper.setOnCheckedChangedListener((group, p) -> {
                int mode = p.getEntryValueInt();
                // ContentResolver does not order its updates, so if the user rapidly clicks on
                // several different modes, they may be broadcast out of order and then the Home
                // app will end up loading the wrong vibration pattern. (b/63518866)
                // So we wait until the user leaves this screen before saving.
                synchronized (mLock) {
                    mPendingVibrationModeToSave = mode;
                }

                // Give 'em a demonstration.
                vibrate(mVibrationModeConfig.getVibrationPatternForMode(mode));
            });
        } else {
            getPreferenceScreen().removePreference(pref);
        }
    }

    protected void initVipContacts(Preference preference) {
        // Check if there exists an activity to handle the intent
        if (mPackageManager.queryIntentActivities(preference.getIntent(), 0).isEmpty()) {
            getPreferenceScreen().removePreference(preference);
        }
    }

    protected void initCardPreviews(final PreferenceScreen pref) {
        ((RadioButtonPreference) pref.findPreference(KEY_PREF_CARD_PREVIEWS_LOW))
                .setEntryValue(CardPreviewModeConfig.LOW);
        ((RadioButtonPreference) pref.findPreference(KEY_PREF_CARD_PREVIEWS_NORMAL))
                .setEntryValue(CardPreviewModeConfig.NORMAL);
        ((RadioButtonPreference) pref.findPreference(KEY_PREF_CARD_PREVIEWS_HIGH))
                .setEntryValue(CardPreviewModeConfig.HIGH);

        RadioGroupPreferenceScreenHelper helper = new RadioGroupPreferenceScreenHelper(pref);
        helper.enableAutoSummary(pref, null);
        helper.checkByEntryValue(mCardPreviewModeConfig.getCardPreviewMode());

        helper.setOnCheckedChangedListener((group, p) -> {
            int cardPreviewMode = p.getEntryValueInt();
            mCardPreviewModeConfig.setCardPreviewMode(cardPreviewMode);
            pref.setIcon(cardPreviewMode == CardPreviewModeConfig.LOW
                    ? R.drawable.ic_cc_preview_off
                    : R.drawable.ic_cc_preview);
        });
    }

    protected void initMuteWhenOffBody(SwitchPreference pref) {
        // The mute when off body service require LLOB on device, otherwise it won't start.
        Sensor sensor =
                mSensorManager.getDefaultSensor(Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT, true);
        if (GKeys.ENABLE_MUTE_NOTIFICATIONS_WHEN_OFF_BODY.get() && sensor != null) {
            pref.setChecked(mMuteWhenOffBodyConfig.isMuteWhenOffBodyEnabled());

            pref.setOnPreferenceChangeListener((p, newVal) ->
                mMuteWhenOffBodyConfig.setMuteWhenOffBodyEnabled((Boolean) newVal));
        } else {
            getPreferenceScreen().removePreference(pref);
        }
    }

    private void vibrate(String pattern) {
        long[] vibPattern = parseVibrationPattern(pattern);
        mVibrator.vibrate(vibPattern, -1);
    }

    // NOTE: this should match the StreamVibrator.parseVibrationPattern in the Home app.
    static long[] parseVibrationPattern(String pattern) {
        String[] entries = pattern.split(VIBRATION_PATTERN_SEPARATOR);
        long[] durations = new long[entries.length];
        for (int i = 0; i < entries.length; ++i) {
            durations[i] = Long.parseLong(entries[i]);
        }
        return durations;
    }
}
