package com.google.android.clockwork.settings.apps;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.text.TextUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.google.android.apps.wearable.settings.R;

public class AppPermissionsSettingsFragment extends AppsListFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (TextUtils.isEmpty(getPreferenceScreen().getTitle())) {
            getPreferenceScreen().setTitle(R.string.app_permissions);
        }
    }

    @Override
    protected void onAppPrefCreated(Preference pref, ApplicationsState.AppEntry appEntry) {
        super.onAppPrefCreated(pref, appEntry);
        pref.setIntent(new Intent(Intent.ACTION_MANAGE_APP_PERMISSIONS)
                .putExtra("hideInfoButton", true)
                .putExtra(Intent.EXTRA_PACKAGE_NAME, appEntry.info.packageName));
    }

    @Override
    protected int getAppsTitleResId() {
        return R.string.app_permissions;
    }

    @Override
    protected int getSystemAppsTitleResId() {
        return R.string.app_permissions_sys;
    }
}
