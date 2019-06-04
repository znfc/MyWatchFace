package com.google.android.clockwork.settings.enterprise;

import android.app.AppGlobals;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ComponentInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.Manifest;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.provider.Settings;
import android.text.format.DateUtils;
import android.util.ArraySet;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;
import com.google.android.clockwork.settings.connectivity.ConnectivityManagerWrapperImpl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Enterprise privacy settings.
 */
public class EnterprisePrivacySettingsFragment extends SettingsPreferenceFragment {
    private static final String TAG = "EnterprisePrivacySettings";

    private static final String KEY_NETWORK_LOGS = "network_logs";
    private static final String KEY_BUG_REPORTS = "bug_reports";
    private static final String KEY_SECURITY_LOGS = "security_logs";
    private static final String KEY_NUMBER_ENTERPRISE_INSTALLED_PACKAGES = "number_enterprise_installed_packages";
    private static final String KEY_NUMBER_LOCATION_ACCESS_PACKAGES = "number_location_access_packages";
    private static final String KEY_NUMBER_MICROPHONE_ACCESS_PACKAGES = "number_microphone_access_packages";
    private static final String KEY_NUMBER_CAMERA_ACCESS_PACKAGES = "number_camera_access_packages";
    private static final String KEY_DEFAULT_APPS = "number_enterprise_set_default_apps";
    private static final String KEY_ALWAYS_ON_VPN_PRIMARY_USER = "always_on_vpn_primary_user";
    private static final String KEY_ALWAYS_ON_VPN_MANAGED_PROFILE = "always_on_vpn_managed_profile";
    private static final String KEY_INPUT_METHOD = "input_method";
    private static final String KEY_GLOBAL_HTTP_PROXY = "global_http_proxy";
    private static final String KEY_CA_CERTS_CURRENT_USER = "ca_certs_current_user";
    private static final String KEY_CA_CERTS_MANAGED_PROFILE = "ca_certs_managed_profile";
    private static final String KEY_FAILED_PASSWORD_WIPE_CURRENT_USER = "failed_password_wipe_current_user";
    private static final String KEY_FAILED_PASSWORD_WIPE_MANAGED_PROFILE = "failed_password_wipe_managed_profile";

    private Resources mResources;
    private ContentResolver mContentResolver;
    private PackageManager mPackageManager;
    private IPackageManager mPackageManagerBinder;
    private DevicePolicyManagerWrapper mDevicePolicyManager;
    private EnterprisePrivacyFeatureProvider mFeatureProvider;

    private static final int MY_USER_ID = UserHandle.myUserId();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_enterprise_privacy);

        Context context = getContext();
        mResources = context.getResources();
        mPackageManager = context.getPackageManager();
        mFeatureProvider = new EnterprisePrivacyFeatureProviderImpl(context,
                new DevicePolicyManagerWrapperImpl(context
                    .getSystemService(DevicePolicyManager.class)),
                context.getPackageManager(),
                AppGlobals.getPackageManager(),
                context.getSystemService(UserManager.class),
                new ConnectivityManagerWrapperImpl(context
                    .getSystemService(ConnectivityManager.class)),
                context.getResources());

        updateNetworkLogsPref();
        updateBugReportsPref();
        updateSecurityLogsPref();
        updateEnterpriseInstalledPackagesPref();
        updateAdminGrantedLocationPermissionsPref();
        updateAdminGrantedMicrophonePermissionsPref();
        updateAdminGrantedCameraPermissionsPref();

        updateDefaultAppsPref();
        updatePrimaryUserVpnPref();
        updateManagedProfileVpnPref();
        updateImePref();
        updateGlobalProxyPref();
        updatePrimaryUserCaCertsPref();
        updateManagedProfileCaCertsPref();

        updatePrimaryUserFailedPasswordWipePref();
        updateManagedProfileFailedPasswordWipePref();
    }

    private void updateNetworkLogsPref() {
        final boolean available = mFeatureProvider.isNetworkLoggingEnabled()
                || mFeatureProvider.getLastNetworkLogRetrievalTime() != null;
        Preference networkLogsPref = findPreference(KEY_NETWORK_LOGS);
        if (networkLogsPref != null) {
            if (available) {
                updateState(networkLogsPref, mFeatureProvider.getLastNetworkLogRetrievalTime());
            } else {
                getPreferenceScreen().removePreference(networkLogsPref);
            }
        }
    }

    private void updateBugReportsPref() {
        Preference bugReportsPref = findPreference(KEY_BUG_REPORTS);
        if (bugReportsPref != null) {
            updateState(bugReportsPref, mFeatureProvider.getLastBugReportRequestTime());
        }
    }

    private void updateSecurityLogsPref() {
        final boolean available = mFeatureProvider.isSecurityLoggingEnabled()
                || mFeatureProvider.getLastSecurityLogRetrievalTime() != null;
        Preference securityLogsPref = findPreference(KEY_SECURITY_LOGS);
        if (securityLogsPref != null) {
            if (available) {
                updateState(securityLogsPref, mFeatureProvider.getLastSecurityLogRetrievalTime());
            } else {
                getPreferenceScreen().removePreference(securityLogsPref);
            }
        }
    }

    private void updateEnterpriseInstalledPackagesPref() {
        Preference enterpriseInstalledPackagesPref =
                findPreference(KEY_NUMBER_ENTERPRISE_INSTALLED_PACKAGES);
        if (enterpriseInstalledPackagesPref != null) {
            mFeatureProvider.listPolicyInstalledApps(
                    (apps) -> {
                        if (apps.size() == 0) {
                            getPreferenceScreen().removePreference(enterpriseInstalledPackagesPref);
                        } else {
                            enterpriseInstalledPackagesPref.setSummary(mResources.getQuantityString(
                                    R.plurals.enterprise_privacy_number_packages_lower_bound,
                                    apps.size(), apps.size()));
                        }
                    });
        }
    }

    private void updateAdminGrantedLocationPermissionsPref() {
        Preference adminGrantedLocationPermissionsPref =
                findPreference(KEY_NUMBER_LOCATION_ACCESS_PACKAGES);
        final boolean mHasApps;
        if (adminGrantedLocationPermissionsPref != null) {
            mFeatureProvider.listAppsWithAdminGrantedPermissions(new String[] {
                Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                (apps) -> {
                    if (apps.isEmpty()) {
                        getPreferenceScreen().removePreference(adminGrantedLocationPermissionsPref);
                    } else {
                        adminGrantedLocationPermissionsPref.setSummary(
                                mResources.getQuantityString(
                                        R.plurals.enterprise_privacy_number_packages_lower_bound,
                                        apps.size(), apps.size()));
                    }
                });
        }
    }

    private void updateAdminGrantedMicrophonePermissionsPref() {
        Preference adminGrantedMicrophonePermissionsPref =
                findPreference(KEY_NUMBER_MICROPHONE_ACCESS_PACKAGES);
        final boolean mHasApps;
        if (adminGrantedMicrophonePermissionsPref != null) {
            mFeatureProvider.listAppsWithAdminGrantedPermissions(new String[] {
                Manifest.permission.RECORD_AUDIO},
                (apps) -> {
                    if (apps.isEmpty()) {
                        getPreferenceScreen().removePreference(adminGrantedMicrophonePermissionsPref);
                    } else {
                        adminGrantedMicrophonePermissionsPref.setSummary(
                                mResources.getQuantityString(
                                        R.plurals.enterprise_privacy_number_packages_lower_bound,
                                        apps.size(), apps.size()));
                    }
                });
        }
    }

    private void updateAdminGrantedCameraPermissionsPref() {
        Preference adminGrantedCameraPermissionsPref =
                findPreference(KEY_NUMBER_CAMERA_ACCESS_PACKAGES);
        final boolean mHasApps;
        if (adminGrantedCameraPermissionsPref != null) {
            mFeatureProvider.listAppsWithAdminGrantedPermissions(new String[] {
                Manifest.permission.CAMERA},
                (apps) -> {
                    if (apps.isEmpty()) {
                        getPreferenceScreen().removePreference(adminGrantedCameraPermissionsPref);
                    } else {
                        adminGrantedCameraPermissionsPref.setSummary(
                                mResources.getQuantityString(
                                        R.plurals.enterprise_privacy_number_packages_lower_bound,
                                        apps.size(), apps.size()));
                    }
                });
        }
    }

    private void updateDefaultAppsPref() {
        final int num = mFeatureProvider.getNumberOfEnterpriseSetDefaultApps();
        Preference defaultAppsPref = findPreference(KEY_DEFAULT_APPS);
        if (num > 0) {
            if (defaultAppsPref != null) {
                defaultAppsPref.setSummary(mResources.getQuantityString(
                        R.plurals.enterprise_privacy_number_packages, num, num));
            }
        } else {
            getPreferenceScreen().removePreference(defaultAppsPref);
        }
    }

    private void updatePrimaryUserVpnPref() {
        final boolean primaryUserVpnAvailable = mFeatureProvider.isAlwaysOnVpnSetInCurrentUser();
        Preference primaryUserVpnPref = findPreference(KEY_ALWAYS_ON_VPN_PRIMARY_USER);
        if (primaryUserVpnPref != null) {
            if (primaryUserVpnAvailable) {
                primaryUserVpnPref.setTitle(mFeatureProvider.isInCompMode()
                        ? R.string.enterprise_privacy_always_on_vpn_personal
                        : R.string.enterprise_privacy_always_on_vpn_device);
            } else {
                getPreferenceScreen().removePreference(primaryUserVpnPref);
            }
        }
    }

    private void updateManagedProfileVpnPref() {
        final boolean available = mFeatureProvider.isAlwaysOnVpnSetInManagedProfile();
        Preference managedProfileVpnPref = findPreference(KEY_ALWAYS_ON_VPN_MANAGED_PROFILE);
        if (managedProfileVpnPref != null) {
            if (!available) {
                getPreferenceScreen().removePreference(managedProfileVpnPref);
            }
        }
    }

    private void updateImePref() {
        final boolean available = mFeatureProvider.getImeLabelIfOwnerSet() != null;
        Preference imePref = findPreference(KEY_INPUT_METHOD);
        if (imePref != null) {
            if (available) {
                imePref.setSummary(mResources.getString(
                        R.string.enterprise_input_method_name,
                        mFeatureProvider.getImeLabelIfOwnerSet()));
            } else {
                getPreferenceScreen().removePreference(imePref);
            }
        }
    }

    private void updateGlobalProxyPref() {
        final boolean available = mFeatureProvider.isGlobalHttpProxySet();
        Preference globalProxyPref = findPreference(KEY_GLOBAL_HTTP_PROXY);
        if (globalProxyPref != null) {
            if (!available) {
                getPreferenceScreen().removePreference(globalProxyPref);
            }
        }
    }

    private void updatePrimaryUserCaCertsPref() {
        final int numCerts =
                mFeatureProvider.getNumberOfOwnerInstalledCaCertsForCurrentUser();
        final boolean available = numCerts > 0;
        Preference primaryUserCaCertsPref = findPreference(KEY_CA_CERTS_CURRENT_USER);
        if (primaryUserCaCertsPref != null) {
            if (available) {
                primaryUserCaCertsPref.setTitle(mFeatureProvider.isInCompMode()
                        ? R.string.enterprise_privacy_ca_certs_personal
                        : R.string.enterprise_privacy_ca_certs_device);
                primaryUserCaCertsPref.setSummary(mResources.getQuantityString(
                        R.plurals.enterprise_privacy_number_ca_certs, numCerts, numCerts));
            } else {
                getPreferenceScreen().removePreference(primaryUserCaCertsPref);
            }
        }
    }

    private void updateManagedProfileCaCertsPref() {
        final boolean available =
                mFeatureProvider.getNumberOfOwnerInstalledCaCertsForManagedProfile() > 0;
        Preference managedProfileCaCertsPref = findPreference(KEY_CA_CERTS_MANAGED_PROFILE);
        if (managedProfileCaCertsPref != null) {
            if (!available) {
                getPreferenceScreen().removePreference(managedProfileCaCertsPref);
            }
        }
    }

    private void updatePrimaryUserFailedPasswordWipePref() {
        final int failedPasswordsBeforeWipe =
                mFeatureProvider.getMaximumFailedPasswordsBeforeWipeInCurrentUser();
        final boolean available = failedPasswordsBeforeWipe > 0;
        Preference primaryUserFailedPasswordWipePref =
                findPreference(KEY_FAILED_PASSWORD_WIPE_CURRENT_USER);
        if (primaryUserFailedPasswordWipePref != null) {
            if (available) {
                primaryUserFailedPasswordWipePref.setSummary(mResources.getQuantityString(
                        R.plurals.enterprise_privacy_number_failed_password_wipe,
                        failedPasswordsBeforeWipe, failedPasswordsBeforeWipe));
            } else {
                getPreferenceScreen().removePreference(primaryUserFailedPasswordWipePref);
            }
        }
    }

    private void updateManagedProfileFailedPasswordWipePref() {
        final int failedPasswordsBeforeWipe =
                mFeatureProvider.getMaximumFailedPasswordsBeforeWipeInManagedProfile();
        final boolean available = failedPasswordsBeforeWipe > 0;
        Preference managedProfileFailedPasswordWipePref =
                findPreference(KEY_FAILED_PASSWORD_WIPE_MANAGED_PROFILE);
        if (managedProfileFailedPasswordWipePref != null) {
            if (available) {
                managedProfileFailedPasswordWipePref.setSummary(mResources.getQuantityString(
                        R.plurals.enterprise_privacy_number_failed_password_wipe,
                        failedPasswordsBeforeWipe, failedPasswordsBeforeWipe));
            } else {
                getPreferenceScreen().removePreference(managedProfileFailedPasswordWipePref);
            }
        }

    }

    private void updateState(Preference preference, Date timestamp) {
        preference.setSummary(timestamp == null ?
                mResources.getString(R.string.enterprise_privacy_none) :
                DateUtils.formatDateTime(getContext(), timestamp.getTime(),
                    DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE));
    }
}
