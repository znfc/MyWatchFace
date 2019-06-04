package com.google.android.clockwork.settings.system;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.SwitchPreference;
import android.text.format.DateFormat;

import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.datetime.ZoneGetter;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.DateTimeConfig;
import com.google.android.clockwork.settings.DefaultDateTimeConfig;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.SettingsIntents;
import com.google.android.clockwork.settings.TimeService;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/** Settings fragment which handles date and time settings */
public class DateTimeSettingsFragment extends SettingsPreferenceFragment {
    private static final String KEY_PREF_AUTO_DATE_TIME = "pref_autoDateTime";
    private static final String KEY_PREF_MANUAL_DATE = "pref_manualDate";
    private static final String KEY_PREF_MANUAL_TIME = "pref_manualTime";
    private static final String KEY_PREF_AUTO_TIME_ZONE = "pref_autoTimeZone";
    private static final String KEY_PREF_MANUAL_TIME_ZONE = "pref_manualTimeZone";
    private static final String KEY_PREF_HOUR_FORMAT = "pref_hourFormat";

    private AlarmManager mAlarmManager;
    private DateTimeConfig mDateTimeConfig;
    private SwitchPreference mHourFormat;
    private DatePickerPreference mManualDate;
    private TimePickerPreference mManualTime;
    private ListPreference mManualTimeZone;

    /**
     * Used for showing the current time format to illustrate the difference between 12- and 24-hour
     * time formats. The date value is dummy (independent of the actual date).
     */
    private Calendar mDummyDate;

    private final BroadcastReceiver mTimeChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateTimeAndDateDisplay();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDateTimeConfig = DefaultDateTimeConfig.INSTANCE.get(getContext());
        mDummyDate = Calendar.getInstance();
        mAlarmManager = getContext().getSystemService(AlarmManager.class);

        addPreferencesFromResource(R.xml.prefs_date_time);
        addPreferencesFromResource(R.xml.prefs_date_time_customization);

        initManualDate(mManualDate = (DatePickerPreference) findPreference(KEY_PREF_MANUAL_DATE));
        initManualTime(mManualTime = (TimePickerPreference) findPreference(KEY_PREF_MANUAL_TIME));
        initManualTimeZone(
                mManualTimeZone = (ListPreference) findPreference(KEY_PREF_MANUAL_TIME_ZONE));
        initHourFormat(mHourFormat = (SwitchPreference) findPreference(KEY_PREF_HOUR_FORMAT));
        initAutoDateTime((SwitchPreference) findPreference(KEY_PREF_AUTO_DATE_TIME));
        initAutoTimeZone((SwitchPreference) findPreference(KEY_PREF_AUTO_TIME_ZONE));
    }

    private void initAutoDateTime(final SwitchPreference pref) {
        int currentMode;

        if (isClockworkAutoTimeEnabled()) {
            mDateTimeConfig.setAutoTime(SettingsContract.SYNC_TIME_FROM_PHONE);
            currentMode = SettingsContract.SYNC_TIME_FROM_PHONE;
        } else {
            currentMode = SettingsContract.AUTO_TIME_OFF;
        }
        // If Device Owner policy is applied, default to "sync from phone"
        if ((currentMode == SettingsContract.AUTO_TIME_OFF)
                && (RestrictedLockUtils.checkIfAutoTimeRequired(getContext()) != null)) {
            mDateTimeConfig.setAutoTime(SettingsContract.SYNC_TIME_FROM_PHONE);
            currentMode = SettingsContract.SYNC_TIME_FROM_PHONE;
        }
        pref.setChecked(currentMode == SettingsContract.SYNC_TIME_FROM_PHONE);
        updateDateTimePrefStates(currentMode);
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            boolean autoTimeEnabled = (Boolean) newVal;

            // sync from phone and network were combined in b/35318277
            // but the states must be maintained for backwards compatibility
            int mode = autoTimeEnabled ?
                    SettingsContract.SYNC_TIME_FROM_PHONE : SettingsContract.AUTO_TIME_OFF;

            // If Device Owner policy is applied and user tries to switch to "sync from phone" or
            // "off", show DevicePolicyManager dialog
            EnforcedAdmin admin = RestrictedLockUtils.checkIfAutoTimeRequired(getContext());
            if ((mode == SettingsContract.AUTO_TIME_OFF) && (admin != null)) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getContext(), admin);
                return false;
            }
            mDateTimeConfig.setAutoTime(mode);

            if (mode != SettingsContract.AUTO_TIME_OFF) {
                set24Hours(null);
            }

            updateDateTimePrefStates(mode);
            DateTimeSettingsHelper.sendTimeServiceIntent(getContext(),
                    SettingsIntents.ACTION_EVALUATE_TIME_SYNCING);
            updateTimeAndDateDisplay();
            return true;
        });
    }

    private boolean isClockworkAutoTimeEnabled() {
        // sync from phone and network were combined in b/35318277
        // but the states must be maintained for backwards compatibility
        int currentMode = mDateTimeConfig.getClockworkAutoTimeMode();
        return currentMode != SettingsContract.AUTO_TIME_OFF;
    }

    private void updateDateTimePrefStates(int mode) {
        mManualDate.setEnabled(mode == SettingsContract.AUTO_TIME_OFF);
        mManualTime.setEnabled(mode == SettingsContract.AUTO_TIME_OFF);
        mHourFormat.setEnabled(mode != SettingsContract.SYNC_TIME_FROM_PHONE);
    }

    private void initManualDate(DatePickerPreference pref) {
        pref.setDateSelectedListener((year, month, day) -> {
            DateTimeSettingsHelper.setDate(
                Calendar.getInstance(),
                mAlarmManager,
                year,
                month,
                day);
            updateTimeAndDateDisplay();
        });
    }

    private void initManualTime(TimePickerPreference pref) {
        pref.setTimeSelectedListener((hour, min) -> {
            DateTimeSettingsHelper.setTime(mAlarmManager, hour, min);
            updateTimeAndDateDisplay();
        });
    }

    private void initAutoTimeZone(final SwitchPreference pref) {
        int currentMode;

        if (isClockworkAutoTimeZoneEnabled()) {
            mDateTimeConfig.setAutoTimeZone(SettingsContract.SYNC_TIME_ZONE_FROM_PHONE);
            currentMode = SettingsContract.SYNC_TIME_ZONE_FROM_PHONE;
        } else {
            currentMode = SettingsContract.AUTO_TIME_ZONE_OFF;
        }
        pref.setChecked(currentMode == SettingsContract.SYNC_TIME_ZONE_FROM_PHONE);
        updateTimeZonePrefStates(currentMode);
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            boolean autoTimeZoneEnabled = (Boolean) newVal;

            // sync from phone and network were combined in b/35318281
            // but the states must be maintained for backwards compatibility
            int mode = autoTimeZoneEnabled ?
                    SettingsContract.SYNC_TIME_ZONE_FROM_PHONE :
                    SettingsContract.AUTO_TIME_ZONE_OFF;

            mDateTimeConfig.setAutoTimeZone(mode);
            updateTimeZonePrefStates(mode);
            DateTimeSettingsHelper.sendTimeServiceIntent(getContext(),
                    SettingsIntents.ACTION_EVALUATE_TIME_ZONE_SYNCING);
            updateTimeAndDateDisplay();
            return true;
        });
    }

    private boolean isClockworkAutoTimeZoneEnabled() {
        // sync from phone and network were combined in b/35318281
        // but the states must be maintained for backwards compatibility
        int currentMode = mDateTimeConfig.getClockworkAutoTimeZoneMode();
        return currentMode != SettingsContract.AUTO_TIME_ZONE_OFF;
    }

    private void updateTimeZonePrefStates(int mode) {
        mManualTimeZone.setEnabled(mode == SettingsContract.AUTO_TIME_ZONE_OFF);
    }

    private void initManualTimeZone(ListPreference pref) {
        List<Map<String, Object>> mTimeZones = ZoneGetter.getZonesList(getContext());
        Collections.sort(mTimeZones, new TimeZonesComparator(ZoneGetter.KEY_OFFSET));

        List<String> entries = new ArrayList<>();
        List<String> entryValues = new ArrayList<>();

        for (Map<String, Object> timeZone : mTimeZones) {
            entries.add(
                    ((String) timeZone.get(ZoneGetter.KEY_DISPLAYNAME)) + '\n'
                    + DateTimeSettingsHelper.formatOffset(
                            (Integer) timeZone.get(ZoneGetter.KEY_OFFSET), getContext()));
            entryValues.add((String) timeZone.get(ZoneGetter.KEY_ID));
        }

        pref.setEntries(entries.toArray(new String[entries.size()]));
        pref.setEntryValues(entryValues.toArray(new String[entryValues.size()]));
        pref.setValue(TimeZone.getDefault().getID());
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            DateTimeSettingsHelper.setTimeZone(mAlarmManager, (String) newVal);
            updateTimeAndDateDisplay();
            return true;
        });
    }

    private void initHourFormat(SwitchPreference pref) {
        pref.setChecked(mDateTimeConfig.isClockwork24HrTimeEnabled());
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            set24Hours((Boolean) newVal);
            updateTimeAndDateDisplay();
            return true;
        });
    }

    private void set24Hours(Boolean is24Hours) {
        getContext().startService(new Intent(getContext(), TimeService.class)
                .setAction(SettingsIntents.ACTION_SET_24HOUR)
                .putExtra(SettingsIntents.EXTRA_IS_24_HOUR, is24Hours));
    }

    /* Update the time and date labels on the settings items to display the correct time */
    private void updateTimeAndDateDisplay() {
        final Calendar now = Calendar.getInstance();
        TimeZone tz = now.getTimeZone();
        mDummyDate.setTimeZone(tz);
        // We use 13:00 to illustrate the 12/24 hour options.
        mDummyDate.set(now.get(Calendar.YEAR), 11, 31, 13, 0, 0);
        // Set the label on the 24 hour option with the dummy date
        mHourFormat.setSummary(DateFormat.getTimeFormat(getContext()).format(mDummyDate.getTime()));
        // Set the labels on the other setting items with the current date and time
        mManualDate.setSummary(DateFormat.getLongDateFormat(getContext()).format(now.getTime()));
        mManualTime.setSummary(DateFormat.getTimeFormat(getContext()).format(now.getTime()));
        mManualTimeZone.setSummary(
                DateTimeSettingsHelper.getTimeZoneOffsetAndName(tz, now, getContext()));
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_TIME_TICK);
        intentFilter.addAction(Intent.ACTION_TIME_CHANGED);
        intentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        getContext().registerReceiver(mTimeChangeReceiver, intentFilter, null, null);

        updateTimeAndDateDisplay();
    }

    @Override
    public void onPause() {
        getContext().unregisterReceiver(mTimeChangeReceiver);
        super.onPause();
    }

    private static class TimeZonesComparator implements Comparator<Map<?, ?>> {
        private String mSortingKey;

        public TimeZonesComparator(String sortingKey) {
            mSortingKey = sortingKey;
        }

        public int compare(Map<?, ?> map1, Map<?, ?> map2) {
            Object value1 = map1.get(mSortingKey);
            Object value2 = map2.get(mSortingKey);
            // This should never happen, but just in case, put non-comparable items at the end
            if (!isComparable(value1)) {
                return isComparable(value2) ? 1 : 0;
            } else if (!isComparable(value2)) {
                return -1;
            }
            return ((Comparable) value1).compareTo(value2);
        }

        private boolean isComparable(Object value) {
            return (value != null) && (value instanceof Comparable);
        }
    }
}
