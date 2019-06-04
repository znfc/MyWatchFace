package com.google.android.clockwork.settings.apps;

import android.os.Bundle;
import android.preference.Preference;
import android.text.TextUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.google.android.apps.wearable.settings.R;

public class AppInfoSettingsFragment extends AppsListFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (TextUtils.isEmpty(getPreferenceScreen().getTitle())) {
            getPreferenceScreen().setTitle(R.string.apps_settings);
        }
    }

    @Override
    protected void onAppPrefCreated(Preference pref, ApplicationsState.AppEntry appEntry) {
        super.onAppPrefCreated(pref, appEntry);
        pref.setFragment(AppDetailsFragment.class.getName());
    }

    @Override
    protected int getAppsTitleResId() {
        return R.string.apps_settings;
    }

    @Override
    protected int getSystemAppsTitleResId() {
        return R.string.apps_settings_sys;
    }
}
