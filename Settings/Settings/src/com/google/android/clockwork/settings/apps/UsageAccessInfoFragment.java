package com.google.android.clockwork.settings.apps;

import android.app.AppOpsManager;
import android.os.Bundle;
import android.preference.SwitchPreference;
import com.android.settingslib.applications.ApplicationsState;
import com.google.android.apps.wearable.settings.R;
import java.util.ArrayList;

/**
 * Setting screen to allow user to enable or disable usage access for a specific app. This allows
 * the user to allow apps to access the android.app.usage API after apps declare the
 * android.permission.PACKAGE_USAGE_STATS permission.
 */
public class UsageAccessInfoFragment extends AppInfoBase {
    private static final String KEY_PREF_USAGE_ACCESS = "usage_access";

    private AppOpsManager mAppOpsManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_usage_access);

        mAppOpsManager = getContext().getSystemService(AppOpsManager.class);
        refreshPrefs(mAppEntry);
    }

    @Override
    public void onRunningStateChanged(boolean running) {
        super.onRunningStateChanged(running);
        refreshPrefs(mAppEntry);
    }

    @Override
    public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> apps) {
        super.onRebuildComplete(apps);
        refreshPrefs(mAppEntry);
    }

    @Override
    public void refreshPrefs(ApplicationsState.AppEntry appEntry) {
        if (appEntry == null) {
            return;
        }

        final String packageName = appEntry.info.packageName;

        getPreferenceScreen().setTitle(appEntry.label);

        initUsageAccessPref((SwitchPreference) findPreference(KEY_PREF_USAGE_ACCESS));
    }

    /** Setup the switch used to change app's usage access setting. */
    protected void initUsageAccessPref(SwitchPreference pref) {
        pref.setChecked(
                ((UsageAccessAppsListFragment.PermissionState) mAppEntry.extraInfo).isAllowed());
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            setAppUsageAccess(mAppEntry, (Boolean) newVal);
            return true;
        });
    }

    /**
     * Set the app usage access setting for the given app.
     * <p>
     * If granted, the app will be able to use the android.app.usage API.
     *
     * @param entry the app to change the app usage access setting.
     * @param grant {@code true} to allow the app usage access (i.e. ability to use
     *              android.app.usage API).
     */
    private void setAppUsageAccess(ApplicationsState.AppEntry entry, boolean grant) {
        mAppOpsManager.setMode(AppOpsManager.OP_GET_USAGE_STATS,
                entry.info.uid, entry.info.packageName,
                grant ? AppOpsManager.MODE_ALLOWED : AppOpsManager.MODE_IGNORED);
    }
}
