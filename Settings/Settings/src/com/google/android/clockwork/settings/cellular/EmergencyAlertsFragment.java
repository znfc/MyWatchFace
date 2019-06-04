package com.google.android.clockwork.settings.cellular;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.telephony.SubscriptionManager;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;

/** Preference for toggling options for emergency alerts of the device. */
public class EmergencyAlertsFragment extends SettingsPreferenceFragment {
    private static final String KEY_PREF_EXTREME_THREATS = "pref_extremeThreats";
    private static final String KEY_PREF_SEVERE_THREATS = "pref_severeThreats";
    private static final String KEY_PREF_AMBER_ALERTS = "pref_amberAlerts";
    private static final String KEY_PREF_ETWS_TEST_ALERTS = "pref_etwsTestAlerts";
    private static final String KEY_PREF_CMAS_TEST_ALERTS = "pref_cmasTestAlerts";
    private static final String KEY_PREF_ALERT_REMINDER = "pref_alertReminder";
    private static final String KEY_PREF_ALERT_VIBRATE = "pref_alertVibrate";
    private static final String KEY_PREF_ALERT_SOUND_DURATION = "pref_alertSoundDuration";

    private SwitchPreference mSevereThreatsPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.prefs_emergency_alerts);

        mSevereThreatsPreference = (SwitchPreference) findPreference(KEY_PREF_SEVERE_THREATS);

        SwitchPreference etwsTestPref = (SwitchPreference) findPreference(KEY_PREF_ETWS_TEST_ALERTS);
        SwitchPreference cmasTestPref = (SwitchPreference) findPreference(KEY_PREF_CMAS_TEST_ALERTS);

        initExtremeThreatsPreference((SwitchPreference) findPreference(KEY_PREF_EXTREME_THREATS));
        initSevereThreatsPreference(mSevereThreatsPreference);
        initAmberAlertsPreference((SwitchPreference) findPreference(KEY_PREF_AMBER_ALERTS));
        initEtwsTestAlertsPreference(etwsTestPref);
        initCmasTestAlertsPreference(cmasTestPref);
        initAlertReminderPreference((SwitchPreference) findPreference(KEY_PREF_ALERT_REMINDER));
        initAlertVibratePreference((SwitchPreference) findPreference(KEY_PREF_ALERT_VIBRATE));
        initAlertSoundDurationPreference(
                (ListPreference) findPreference(KEY_PREF_ALERT_SOUND_DURATION));

        // Show extra settings when developer options is enabled in settings or in Res config.
        boolean enableDevSettings = Settings.Global.getInt(getContext().getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0;
        boolean enableResConfig = getResources().getBoolean(R.bool.config_showEtwsCmasSettings);
        if (!(enableDevSettings || enableResConfig)) {
            getPreferenceScreen().removePreference(etwsTestPref);
            getPreferenceScreen().removePreference(cmasTestPref);
        }
    }

    protected void initExtremeThreatsPreference(final SwitchPreference pref) {
        final boolean value = Utils.getBooleanProperty(getContext(),
                SubscriptionManager.CB_EXTREME_THREAT_ALERT);
        pref.setChecked(value);
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            Utils.setBooleanProperty(getContext(), SubscriptionManager.CB_EXTREME_THREAT_ALERT,
                    (Boolean) newVal);
            mSevereThreatsPreference.setChecked(false);
            Utils.setBooleanProperty(getContext(), SubscriptionManager.CB_SEVERE_THREAT_ALERT,
                    false);
            return true;
        });
    }

    protected void initSevereThreatsPreference(final SwitchPreference pref) {
        final boolean value = Utils.getBooleanProperty(getContext(),
                SubscriptionManager.CB_SEVERE_THREAT_ALERT);
        pref.setChecked(value);
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            // The severe threats setting can only be modified when extreme threats is also
            // enabled, by regulation.
            if (Utils.getBooleanProperty(getContext(),
                        SubscriptionManager.CB_EXTREME_THREAT_ALERT)) {
                Utils.setBooleanProperty(getContext(), SubscriptionManager.CB_SEVERE_THREAT_ALERT,
                        (Boolean) newVal);
            }
            return true;
        });
    }

    protected void initAmberAlertsPreference(final SwitchPreference pref) {
        final boolean value = Utils.getBooleanProperty(getContext(),
                SubscriptionManager.CB_AMBER_ALERT);
        pref.setChecked(value);
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            Utils.setBooleanProperty(getContext(), SubscriptionManager.CB_AMBER_ALERT,
                    (Boolean) newVal);
            return true;
        });
    }

    protected void initEtwsTestAlertsPreference(final SwitchPreference pref) {
        final boolean value = Utils.getBooleanProperty(getContext(),
                SubscriptionManager.CB_ETWS_TEST_ALERT);
        pref.setChecked(value);
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            Utils.setBooleanProperty(getContext(), SubscriptionManager.CB_ETWS_TEST_ALERT,
                    (Boolean) newVal);
            return true;
        });
    }

    protected void initCmasTestAlertsPreference(final SwitchPreference pref) {
        final boolean value = Utils.getBooleanProperty(getContext(),
                SubscriptionManager.CB_CMAS_TEST_ALERT);
        pref.setChecked(value);
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            Utils.setBooleanProperty(getContext(), SubscriptionManager.CB_CMAS_TEST_ALERT,
                    (Boolean) newVal);
            return true;
        });
    }

    protected void initAlertReminderPreference(final SwitchPreference pref) {
        final int value = Utils.getIntegerProperty(getContext(),
                SubscriptionManager.CB_ALERT_REMINDER_INTERVAL);
        pref.setChecked(value != 0);
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            int reminderInterval
                    = getResources().getInteger(R.integer.emergency_alerts_reminder_interval);
            int offInterval = getResources().getInteger(R.integer.emergency_alerts_reminder_off);

            Utils.setIntegerProperty(getContext(), SubscriptionManager.CB_ALERT_REMINDER_INTERVAL,
                    (Boolean) newVal ? reminderInterval : offInterval);
            return true;
        });
    }

    protected void initAlertVibratePreference(final SwitchPreference pref) {
        final boolean value = Utils.getBooleanProperty(getContext(),
                SubscriptionManager.CB_ALERT_VIBRATE);
        pref.setChecked(value);
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            Utils.setBooleanProperty(getContext(), SubscriptionManager.CB_ALERT_VIBRATE,
                    (Boolean) newVal);
            return true;
        });
    }

    protected void initAlertSoundDurationPreference(final ListPreference pref) {
        pref.setValue(Integer.toString(Utils.getIntegerProperty(
                getContext(), SubscriptionManager.CB_ALERT_SOUND_DURATION)));
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            Utils.setIntegerProperty(
                    getContext(),
                    SubscriptionManager.CB_ALERT_SOUND_DURATION,
                    Integer.valueOf((String) newVal));
            return true;
        });
    }
}
