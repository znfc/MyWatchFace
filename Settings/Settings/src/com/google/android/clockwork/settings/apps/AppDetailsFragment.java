package com.google.android.clockwork.settings.apps;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.support.wearable.preference.AcceptDenyDialogPreference;
import android.text.TextUtils;
import android.util.Log;

import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.Utils;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.common.logging.CwEventLogger;
import com.google.android.clockwork.packagemanager.PackageManagerSharedUtil;
import com.google.android.clockwork.settings.DeviceAdminAdd;
import com.google.android.clockwork.settings.common.LogUtils;
import com.google.android.clockwork.settings.common.SettingsPreferenceLogConstants;
import com.google.common.annotations.VisibleForTesting;
import com.google.protos.wireless.android.clockwork.apps.logs.CwEnums;

import java.util.ArrayList;
import java.util.List;

public class AppDetailsFragment extends AppInfoBase {
    private static final String TAG = "AppDetailsFragment";

    private static final String KEY_PREF_UNINSTALL = "pref_uninstall";
    private static final String KEY_PREF_FORCE_STOP = "pref_force_stop";
    private static final String KEY_PREF_PERMISSIONS = "pref_permissions";
    private static final String KEY_PREF_ADVANCED = "pref_advanced";
    private static final String KEY_PREF_ABOUT = "pref_about";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_app_details);

        refreshPrefs(mAppEntry);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        // Add logging unique to app details fragment, even if in general it's not all that useful
        // for settings pages dervied from AppInfoBase
        CwEnums.CwSettingsUiEvent event =
                SettingsPreferenceLogConstants.getLoggingId(preference.getKey());
        LogUtils.logPreferenceSelection(getActivity(), event);
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @VisibleForTesting
    void setStateAndSession(ApplicationsState state, ApplicationsState.Session session) {
        mState = state;
        mSession = session;
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
    public void refreshPrefs(AppEntry appEntry) {
        if (appEntry == null) {
            return;
        }

        final String packageName = appEntry.info.packageName;
        boolean isStopped = (appEntry.info.flags & ApplicationInfo.FLAG_STOPPED) != 0;

        getPreferenceScreen().setTitle(appEntry.label);

        refreshUninstall(getActivity(),
                (AcceptDenyDialogPreference) findPreference(KEY_PREF_UNINSTALL), appEntry,
                mPackageInfo, true);

        AcceptDenyDialogPreference forceStopPref = (AcceptDenyDialogPreference)
                findPreference(KEY_PREF_FORCE_STOP);
        forceStopPref.setOnDialogClosedListener((positiveResult) -> {
            if (positiveResult && getContext() != null && !checkForceStopDisallowed()) {
                ActivityManager am = (ActivityManager) getContext().getSystemService(
                        Context.ACTIVITY_SERVICE);
                am.forceStopPackage(packageName);
                goBack(getActivity());
            }
        });
        if (!isStopped) {
            forceStopPref.setEnabled(true);
            forceStopPref.setSelectable(true);
        } else {
            forceStopPref.setEnabled(false);
            forceStopPref.setSelectable(false);
        }

        findPreference(KEY_PREF_PERMISSIONS).getIntent()
                .putExtra(Intent.EXTRA_PACKAGE_NAME, packageName);

        // Ensure advanced fragment has the package name
        findPreference(KEY_PREF_ADVANCED).getExtras()
                .putString(AppInfoBase.ARG_PACKAGE_NAME, packageName);

        findPreference(KEY_PREF_ABOUT).getExtras()
                .putString(AppInfoBase.ARG_PACKAGE_NAME, packageName);
    }

    static void refreshUninstall(Activity activity, AcceptDenyDialogPreference uninstallPref,
            AppEntry appEntry, PackageInfo pInfo, boolean icon) {

        PackageManager packageManager = activity.getPackageManager();
        String packageName = appEntry.info.packageName;
        boolean isSystem = (appEntry.info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        boolean isUpdated = (appEntry.info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
        boolean isEnabled = appEntry.info.enabled;

        ArrayList<ResolveInfo> homeActivities = new ArrayList<ResolveInfo>();
        packageManager.getHomeActivities(homeActivities);
        boolean isDisableAllowed = isDisableAllowed(activity, packageName, pInfo, homeActivities);
        if (isSystem) {
            // Is a system app, either show Disable/Enable or Remove Upgrades
            if (isUpdated) {
                // System app that has been updated, only show remove upgrades.
                uninstallPref.setTitle(R.string.app_label_remove_upgrades);
                if (icon) uninstallPref.setIcon(R.drawable.ic_cc_settings_uninstall_upgrades);
                uninstallPref.setDialogTitle(R.string.app_alert_title_remove_upgrades);
                if (isDisableAllowed) {
                    uninstallPref.setOnDialogClosedListener((positiveResult) -> {
                        if (positiveResult && !checkUninstallDisallowed(activity, packageName)) {
                            PackageManagerSharedUtil.startUninstallService(activity, packageName);
                            goBack(activity);
                        }
                    });
                } else {
                    uninstallPref.setEnabled(false);
                }
            } else {
                if (isEnabled) {
                    // System app that has not been updated, only show Disable.
                    uninstallPref.setTitle(R.string.app_label_disable);
                    if (icon) uninstallPref.setIcon(R.drawable.ic_cc_settings_clear);
                    uninstallPref.setDialogTitle(R.string.app_alert_title_disable);
                    uninstallPref.setDialogMessage(R.string.app_alert_summary_disable);
                    if (isDisableAllowed) {
                        uninstallPref.setOnDialogClosedListener((positiveResult) -> {
                            if (positiveResult && packageManager != null
                                    && !checkUninstallDisallowed(activity, packageName)) {
                                packageManager.setApplicationEnabledSetting(packageName,
                                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER, 0);
                                goBack(activity);
                            }
                        });
                    } else {
                        uninstallPref.setEnabled(false);
                    }
                } else {
                    // System app that has not been updated, only show Enable.
                    uninstallPref.setTitle(R.string.app_label_enable);
                    if (icon) uninstallPref.setIcon(R.drawable.ic_cc_settings_add);
                    uninstallPref.setDialogTitle(R.string.app_alert_title_enable);
                    uninstallPref.setOnDialogClosedListener((positiveResult) -> {
                        if (positiveResult && packageManager != null
                                && !checkUninstallDisallowed(activity, packageName)) {
                            packageManager.setApplicationEnabledSetting(
                                    packageName, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, 0);
                            goBack(activity);
                        }
                    });
                }
            }
        } else {
            // Only show uninstall
            uninstallPref.setTitle(R.string.app_label_uninstall); // Should already be this in XML.
            uninstallPref.setDialogTitle(R.string.app_alert_title_uninstall);
            if (icon) uninstallPref.setIcon(R.drawable.ic_cc_settings_uninstall);
            uninstallPref.setOnDialogClosedListener((positiveResult) -> {
                if (positiveResult && !checkUninstallDisallowed(activity, packageName)) {
                    DevicePolicyManager dpm =
                            (DevicePolicyManager) activity
                                    .getSystemService(Context.DEVICE_POLICY_SERVICE);
                    if (dpm.packageHasActiveAdmins(packageName)) {
                        Intent uninstallDAIntent = new Intent(activity, DeviceAdminAdd.class);
                        uninstallDAIntent.putExtra(DeviceAdminAdd.EXTRA_DEVICE_ADMIN_PACKAGE_NAME,
                                packageName);
                        activity.startActivityForResult(uninstallDAIntent, 0);
                    } else {
                        PackageManagerSharedUtil.startUninstallService(activity, packageName);
                    }
                    goBack(activity);
                }
            });
        }
    }

    static boolean checkUninstallDisallowed(Context c, String packageName) {
        final EnforcedAdmin admin = RestrictedLockUtils.checkIfUninstallBlocked(c, packageName,
                UserHandle.myUserId());
        if (admin != null) {
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(c, admin);
            return true;
        }
        return false;
    }

    private boolean checkForceStopDisallowed() {
        final EnforcedAdmin admin = RestrictedLockUtils.checkIfRestrictionEnforced(getContext(),
                UserManager.DISALLOW_APPS_CONTROL, UserHandle.myUserId());
        if (admin != null) {
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getContext(), admin);
            return true;
        }
        return false;
    }

    private static void goBack(Activity a) {
        if (a != null) {
            a.finish();
        }
    }

    // Code is similar to InstalledAppDetails#handleDisableable
    static boolean isDisableAllowed(Context c, final String packageName, PackageInfo pInfo,
            ArrayList<ResolveInfo> homeActivities) {
        // Try to prevent the user from bricking their phone by not allowing
        // disabling of any launcher app in the system.
        if (homeActivities != null) {
            for (int i = 0, size = homeActivities.size(); i < size; i++) {
                if (TextUtils.equals(packageName, homeActivities.get(i).activityInfo.packageName)) {
                    return false;
                }
            }
        }

        // Try to prevent the user from losing essential function, do not allow disabling of
        // voice search apps, like Mobvoi .
        Intent intent = new Intent(Intent.ACTION_ASSIST);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        List<ResolveInfo> assistActivities =
                c.getPackageManager().queryIntentActivities(intent, 0 /* no flag */);
        for (int i = 0, size = assistActivities.size(); i < size; i++) {
            if (TextUtils.equals(packageName, assistActivities.get(i).activityInfo.packageName)) {
                return false;
            }
        }

        // Try to prevent the user from losing essential function, do not allow disabling of
        // a specifically defined set of packages.
        String[] disallowedDisable = c.getResources().getStringArray(
                R.array.cannot_disable_package);
        if (disallowedDisable != null) {
            for (int i = 0, size = disallowedDisable.length; i < size; i++) {
                if (TextUtils.equals(packageName, disallowedDisable[i])) {
                    return false;
                }
            }
        }

        // If mPackageInfo is null because we were not able to retrieve it in AppInfoBase, then we
        // will not allow disable.
        if (c == null || pInfo == null || c.getPackageManager() == null ||
                Utils.isSystemPackage(c.getResources(), c.getPackageManager(), pInfo)) {
            return false;
        }

        return true;
    }
}
