package com.google.android.clockwork.settings.cellular;

import android.os.Bundle;
import android.support.wearable.preference.WearablePreferenceActivity;

import com.google.android.apps.wearable.settings.R;

/**
 * Data Usage settings split out as activity to run
 * on system server for proper permissions access.
 */
public class DataUsageActivity extends WearablePreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            startPreferenceFragment(new DataUsageFragment(), false);
        }
    }
}
