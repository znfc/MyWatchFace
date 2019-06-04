package com.google.android.clockwork.settings.enterprise;

import android.app.AppGlobals;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.ArraySet;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;

/**
 * Fragment for displaying enterprise-set list of default applications.
 */
public class EnterpriseSetDefaultAppsListFragment extends SettingsPreferenceFragment {
    static final String TAG = "EnterprisePrivacySettings";

    private Resources mResources;
    private PackageManager mPackageManager;
    private IPackageManager mPackageManagerBinder;
    private UserManager mUserManager;
    private List<UserInfo> mUsers = Collections.emptyList();
    private List<EnumMap<EnterpriseDefaultApps, List<ApplicationInfo>>> mApps = Collections.emptyList();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_enterprise_default_apps);

        Context context = getContext();
        mResources = context.getResources();
        mPackageManager = context.getPackageManager();
        mPackageManagerBinder = AppGlobals.getPackageManager();
        mUserManager = context.getSystemService(UserManager.class);

        buildAppList();
    }

    private void buildAppList() {
        mUsers = new ArrayList<>();
        mApps = new ArrayList<>();
        for (UserHandle user : mUserManager.getUserProfiles()) {
            boolean hasDefaultsForUser = false;
            EnumMap<EnterpriseDefaultApps, List<ApplicationInfo>> userMap = null;

            for (EnterpriseDefaultApps typeOfDefault : EnterpriseDefaultApps.values()) {
                List<UserAppInfo> apps =
                        findPersistentPreferredActivities(user.getIdentifier(),
                                typeOfDefault.getIntents());
                if (apps.isEmpty()) {
                    continue;
                }
                if (!hasDefaultsForUser) {
                    hasDefaultsForUser = true;
                    mUsers.add(apps.get(0).userInfo);
                    userMap = new EnumMap<>(EnterpriseDefaultApps.class);
                    mApps.add(userMap);
                }
                ArrayList<ApplicationInfo> applicationInfos = new ArrayList<>();
                for (UserAppInfo userAppInfo : apps) {
                    applicationInfos.add(userAppInfo.appInfo);
                }
                userMap.put(typeOfDefault, applicationInfos);
            }
        }
        updateUi();
    }

    private void updateUi() {
        final PreferenceScreen screen = getPreferenceScreen();
        if (screen == null) {
            return;
        }
        if (mApps.size() == 0) {
            return;
        }
        EnumMap<EnterpriseDefaultApps, List<ApplicationInfo>> apps = mApps.get(0);
        for (EnterpriseDefaultApps typeOfDefault : EnterpriseDefaultApps.values()) {
            final List<ApplicationInfo> appsForCategory = apps.get(typeOfDefault);
            if (appsForCategory == null || appsForCategory.isEmpty()) {
                continue;
            }
            final Preference preference = new Preference(getContext());
            preference.setTitle(getTitle(mResources, typeOfDefault, appsForCategory.size()));
            preference.setSummary(buildSummaryString(mResources, appsForCategory));
            preference.setOrder(typeOfDefault.ordinal());
            preference.setSelectable(false);
            screen.addPreference(preference);
        }
    }

    private List<UserAppInfo> findPersistentPreferredActivities(int userId, Intent[] intents) {
        final List<UserAppInfo> preferredActivities = new ArrayList<>();
        final Set<UserAppInfo> uniqueApps = new ArraySet<>();
        final UserInfo userInfo = mUserManager.getUserInfo(userId);
        for (final Intent intent : intents) {
            try {
                final ResolveInfo resolveInfo =
                        mPackageManagerBinder.findPersistentPreferredActivity(intent, userId);
                if (resolveInfo != null) {
                    ComponentInfo componentInfo = null;
                    if (resolveInfo.activityInfo != null) {
                        componentInfo = resolveInfo.activityInfo;
                    } else if (resolveInfo.serviceInfo != null) {
                        componentInfo = resolveInfo.serviceInfo;
                    } else if (resolveInfo.providerInfo != null) {
                        componentInfo = resolveInfo.providerInfo;
                    }
                    if (componentInfo != null) {
                        UserAppInfo info = new UserAppInfo(userInfo, componentInfo.applicationInfo);
                        if (uniqueApps.add(info)) {
                            preferredActivities.add(info);
                        }
                    }
                }
            } catch (RemoteException exception) {
            }
        }
        return preferredActivities;
    }

    private CharSequence buildSummaryString(Resources resources, List<ApplicationInfo> apps) {
        final CharSequence[] appNames = new String[apps.size()];
        for (int i = 0; i < apps.size(); i++) {
            appNames[i] = apps.get(i).loadLabel(mPackageManager);
        }
        if (apps.size() == 1) {
            return appNames[0];
        } else if (apps.size() == 2) {
            return resources.getString(R.string.app_names_concatenation_template_2, appNames[0],
                    appNames[1]);
        } else {
            return resources.getString(R.string.app_names_concatenation_template_3, appNames[0],
                    appNames[1], appNames[2]);
        }
    }

    private String getTitle(Resources resources, EnterpriseDefaultApps typeOfDefault, int appCount) {
        switch (typeOfDefault) {
            case BROWSER:
                return resources.getString(R.string.default_browser_title);
            case CALENDAR:
                return resources.getString(R.string.default_calendar_app_title);
            case CONTACTS:
                return resources.getString(R.string.default_contacts_app_title);
            case PHONE:
                return resources.getQuantityString(R.plurals.default_phone_app_title, appCount);
            case MAP:
                return resources.getString(R.string.default_map_app_title);
            case EMAIL:
                return resources.getQuantityString(R.plurals.default_email_app_title, appCount);
            case CAMERA:
                return resources.getQuantityString(R.plurals.default_camera_app_title, appCount);
            default:
                throw new IllegalStateException("Unknown type of default " + typeOfDefault);
        }
    }
}
