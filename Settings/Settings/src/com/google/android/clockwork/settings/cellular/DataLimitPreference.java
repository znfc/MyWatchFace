package com.google.android.clockwork.settings.cellular;

import android.content.Context;
import android.content.Intent;
import android.preference.ListPreference;
import android.text.format.Formatter;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.List;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.NetworkPolicyNotificationReceiver;
import com.google.android.clockwork.settings.NetworkPolicyNotificationIntentService;

public class DataLimitPreference extends ListPreference {
    public DataLimitPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DataLimitPreference(Context context) {
        super(context);
        init(context);
    }

    public void setValue(long dataLimit) {
        super.setValue(Long.toString(dataLimit));
        setSummary(dataLimit);
    }

    @Override
    public void setValue(String value) {
        setSummary(Long.valueOf(value));

        super.setValue(value);

        Intent clearIntent = new Intent(getContext(), NetworkPolicyNotificationReceiver.class);
        clearIntent.setAction(
                NetworkPolicyNotificationIntentService.ACTION_CLEAR_STORED_DISMISSALS);
        getContext().sendBroadcast(clearIntent);
    }

    private void setSummary(long dataLimit) {
        setSummary(dataLimit < 0 ? null : Formatter.formatFileSize(getContext(), dataLimit));
    }

    private void init(Context context) {
        int[] levels = getContext().getResources().getIntArray(R.array.cellular_data_limit_levels);
        String[] entries = new String[levels.length];
        String[] entryValues = new String[levels.length];

        for (int i = 0; i < levels.length; ++i) {
            final long limitBytes = ((long) levels[i]) * 1000L * 1000L;
            entries[i] = Formatter.formatFileSize(getContext(), limitBytes);
            entryValues[i] = Long.toString(limitBytes);
        }

        setEntries(entries);
        setEntryValues(entryValues);
    }
}
