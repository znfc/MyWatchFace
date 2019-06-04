package com.google.android.clockwork.settings.enterprise;

import android.app.AppGlobals;
import android.app.admin.DevicePolicyManager;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import java.util.List;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;
import com.google.android.clockwork.settings.connectivity.ConnectivityManagerWrapperImpl;

/**
 * Base fragment for displaying a list of applications on a device.
 * Inner static classes are concrete implementations.
 * Based on AOSP Settings: src/com/android/settings/enterprise/ApplicationListFragment.java
 */
public abstract class ApplicationListFragment extends SettingsPreferenceFragment {
    static final String TAG = "EnterprisePrivacySettings";

    private final String[] mPermissions;
    private PackageManager mPackageManager;
    protected EnterprisePrivacyFeatureProvider mFeatureProvider;

    public ApplicationListFragment(String[] permissions) {
        mPermissions = permissions;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_app_list_disclosure_settings);

        Context context = getContext();
        mPackageManager = context.getPackageManager();
        mFeatureProvider = new EnterprisePrivacyFeatureProviderImpl(context,
                new DevicePolicyManagerWrapperImpl(context
                        .getSystemService(DevicePolicyManager.class)),
                mPackageManager,
                AppGlobals.getPackageManager(),
                context.getSystemService(UserManager.class),
                new ConnectivityManagerWrapperImpl(context
                        .getSystemService(ConnectivityManager.class)),
                context.getResources());

        buildApplicationList(this::onListOfAppsResult);
    }

    public void buildApplicationList(
            EnterprisePrivacyFeatureProvider.ListOfAppsCallback callback) {
        mFeatureProvider.listAppsWithAdminGrantedPermissions(mPermissions, callback);
    }

    public void onListOfAppsResult(List<UserAppInfo> result) {
        final PreferenceScreen screen = getPreferenceScreen();
        if (screen == null) {
            return;
        }
        final Context context = getContext();
        for (int position = 0; position < result.size(); position++) {
            final UserAppInfo item = result.get(position);
            final Preference preference = new Preference(context);
            preference.setTitle(item.appInfo.loadLabel(mPackageManager));
            preference.setIcon(item.appInfo.loadIcon(mPackageManager));
            preference.setOrder(position);
            preference.setSelectable(false);
            screen.addPreference(preference);
        }
    }

    public static class AdminGrantedPermissionCamera extends ApplicationListFragment {
        public AdminGrantedPermissionCamera() {
            super(new String[] {Manifest.permission.CAMERA});
        }
    }

    public static class AdminGrantedPermissionLocation extends ApplicationListFragment {
        public AdminGrantedPermissionLocation() {
            super(new String[] {Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION});
        }
    }

    public static class AdminGrantedPermissionMicrophone extends ApplicationListFragment {
        public AdminGrantedPermissionMicrophone() {
            super(new String[] {Manifest.permission.RECORD_AUDIO});
        }
    }

    public static class EnterpriseInstalledPackages extends ApplicationListFragment {
        public EnterpriseInstalledPackages() {
            super(null);
        }

        @Override
        public void buildApplicationList(
                EnterprisePrivacyFeatureProvider.ListOfAppsCallback callback) {
            mFeatureProvider.listPolicyInstalledApps(callback);
        }
    }
}
