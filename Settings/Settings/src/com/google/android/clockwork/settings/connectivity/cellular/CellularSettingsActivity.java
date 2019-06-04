package com.google.android.clockwork.settings.connectivity.cellular;

import android.os.Bundle;
import android.support.wearable.preference.WearablePreferenceActivity;

import com.google.android.apps.wearable.settings.R;

/**
 * Cellular settings activity, split out to run on com.android.phone.
 */
public class CellularSettingsActivity extends WearablePhonePreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            startPreferenceFragment(new CellularSettingsFragment(), false);
        }
    }
}
