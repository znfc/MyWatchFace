package com.google.android.clockwork.settings.system;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.wearable.preference.AcceptDenySwitchPreference;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AlignmentSpan;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.BatterySaverUtil;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;

/**
 * System settings.
 */
public class BatterySaverFragment extends SettingsPreferenceFragment {

    private static final String KEY_PREF_BATTERY_SAVER = "pref_batterySaver";
    private static final String KEY_PREF_AUTO_BATTERY_SAVER = "pref_autoBatterySaver";

    public static BatterySaverFragment newInstance() {
        return new BatterySaverFragment();
    }

    private AcceptDenySwitchPreference mBatterySaverPref;
    private PowerManager mPowerManager;

    private final BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                boolean plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
                boolean talkbackEnabled = BatterySaverUtil.isTalkbackEnabled(getContext());
                boolean hasSpeaker = BatterySaverUtil.hasSpeaker(getContext());
                mBatterySaverPref.setEnabled(!plugged && (!talkbackEnabled || hasSpeaker));
            } else if (PowerManager.ACTION_POWER_SAVE_MODE_CHANGING.equals(action)) {
                mBatterySaverPref.setChecked(mPowerManager.isPowerSaveMode());
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_battery_saver);

        mPowerManager = getActivity().getSystemService(PowerManager.class);
        BatteryManager batteryManager = getActivity().getSystemService(BatteryManager.class);
        mBatterySaverPref = (AcceptDenySwitchPreference) findPreference(KEY_PREF_BATTERY_SAVER);
        initBatterySaverMode(mBatterySaverPref, mPowerManager);
        initAutoBatterySaver(
                (AcceptDenySwitchPreference) findPreference(KEY_PREF_AUTO_BATTERY_SAVER));

        getPreferenceScreen().setTitle(getString(
                R.string.pref_batterySaverMode_title,
                batteryManager.getIntProperty(batteryManager.BATTERY_PROPERTY_CAPACITY)));

        IntentFilter batteryFilter = new IntentFilter();
        batteryFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        batteryFilter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGING);
        getContext().registerReceiver(mBatteryReceiver, batteryFilter);
    }

    @Override
    public void onDestroy() {
        getContext().unregisterReceiver(mBatteryReceiver);
        super.onDestroy();
    }

    private void initBatterySaverMode(AcceptDenySwitchPreference pref,
            PowerManager powerManager) {
        if (disableBatterySaverForTalkback()) {
            pref.setChecked(false);
            pref.setEnabled(false);
            pref.setSummaryOff(getString(R.string.pref_batterySaver_offForTalkback));
            return;
        }
        pref.setChecked(powerManager.isPowerSaveMode());
    }

    private void initAutoBatterySaver(AcceptDenySwitchPreference pref) {

        if (BatterySaverUtil.useTwm(getContext())) {
            // No auto saver pref if using TWM
            getPreferenceScreen().removePreference(pref);
            return;
        }

        int defaultLevel = getResources().getInteger(R.integer.config_lowPowerModeTriggerLevel);
        pref.setTitle(getString(R.string.pref_autoBatterySaver, defaultLevel));
        if (disableBatterySaverForTalkback()) {
            pref.setChecked(false);
            pref.setEnabled(false);
            pref.setSummaryOff(getString(R.string.pref_batterySaver_offForTalkback));
            return;
        }
        int currentLevel = Settings.Global.getInt(getContext().getContentResolver(),
                Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL, defaultLevel);
        pref.setChecked(currentLevel > 0);
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            boolean enable = (Boolean) newVal;
            BatterySaverUtil.configurePowerSaverMode(getContext(), enable);
            pref.setChecked(enable);
            return true;
        });
    }

    private boolean disableBatterySaverForTalkback() {
        return BatterySaverUtil.isTalkbackEnabled(getContext())
                && !BatterySaverUtil.hasSpeaker(getContext());
    }
}
