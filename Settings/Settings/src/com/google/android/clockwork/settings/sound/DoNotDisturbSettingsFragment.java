package com.google.android.clockwork.settings.sound;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.apps.wearable.settings.R;

public class DoNotDisturbSettingsFragment extends SettingsPreferenceFragment {
    private static String KEY_PREF_DND_OPTIONS_CALLS = "pref_dndOptions_calls";
    private static String KEY_PREF_DND_OPTIONS_REMINDERS = "pref_dndOptions_reminders";
    private static String KEY_PREF_DND_OPTIONS_EVENTS = "pref_dndOptions_events";
    public static String ACTION_DO_NOT_DISTURB_SETTINGS
            = "com.google.android.clockwork.settings.DO_NOT_DISTURB_SETTINGS";

    private NotificationManager mNotificationManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mNotificationManager = (NotificationManager) getActivity()
                .getSystemService(Context.NOTIFICATION_SERVICE);

        addPreferencesFromResource(R.xml.prefs_dnd);

        NotificationManager.Policy initialPolicy = mNotificationManager.getNotificationPolicy();

        if (!isAltMode(getContext())) {
            initDndOption(
                (SwitchPreference) findPreference(KEY_PREF_DND_OPTIONS_CALLS),
                NotificationManager.Policy.PRIORITY_CATEGORY_CALLS,
                initialPolicy);
        } else {
            getPreferenceScreen().removePreference(findPreference(KEY_PREF_DND_OPTIONS_CALLS));
        }
        initDndOption(
                (SwitchPreference) findPreference(KEY_PREF_DND_OPTIONS_REMINDERS),
                NotificationManager.Policy.PRIORITY_CATEGORY_REMINDERS,
                initialPolicy);
        initDndOption(
                (SwitchPreference) findPreference(KEY_PREF_DND_OPTIONS_EVENTS),
                NotificationManager.Policy.PRIORITY_CATEGORY_EVENTS,
                initialPolicy);
    }

    protected void initDndOption(SwitchPreference pref, final int categoryType,
            NotificationManager.Policy initialPolicy) {
        pref.setChecked((initialPolicy.priorityCategories & categoryType) != 0);

        pref.setOnPreferenceChangeListener((p, newVal) -> {
            boolean checked = (Boolean) newVal;

            NotificationManager.Policy policy = mNotificationManager.getNotificationPolicy();

            mNotificationManager.setNotificationPolicy(new NotificationManager.Policy(
                    checked ? policy.priorityCategories | categoryType
                            : policy.priorityCategories & ~categoryType,
                    policy.priorityCallSenders,
                    policy.priorityMessageSenders));

            return true;
        });
    }

    /**
     * Returns true if the watch is paired with an "alt" phone (iOS), and false otherwise.
     */
    public static boolean isAltMode(Context context) {
        return SettingsContract.getIntValueForKey(
            context.getContentResolver(),
            SettingsContract.BLUETOOTH_MODE_URI,
            SettingsContract.KEY_BLUETOOTH_MODE,
            SettingsContract.BLUETOOTH_MODE_UNKNOWN) == SettingsContract.BLUETOOTH_MODE_ALT;
    }
}
