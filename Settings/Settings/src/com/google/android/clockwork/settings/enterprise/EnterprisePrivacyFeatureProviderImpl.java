/*
 * Based on android phone settings:
 * packages/apps/Settings/src/com/android/settings/enterprise/EnterprisePrivacyFeatureProviderImpl.java
 */
package com.google.android.clockwork.settings.enterprise;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ComponentInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.util.ArraySet;
import android.view.View;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.connectivity.ConnectivityManagerWrapper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class EnterprisePrivacyFeatureProviderImpl implements EnterprisePrivacyFeatureProvider {

    private final Context mContext;
    private final DevicePolicyManagerWrapper mDevicePolicyManager;
    private final PackageManager mPackageManager;
    private final IPackageManager mPackageManagerBinder;
    private final UserManager mUserManager;
    private final ConnectivityManagerWrapper mConnectivityManager;
    private final Resources mResources;

    private static final int MY_USER_ID = UserHandle.myUserId();

    public EnterprisePrivacyFeatureProviderImpl(
            Context context,
            DevicePolicyManagerWrapper devicePolicyManager,
            PackageManager packageManager,
            IPackageManager packageManagerBinder,
            UserManager userManager,
            ConnectivityManagerWrapper connectivityManagerWrapper,
            Resources resources) {
        mContext = context.getApplicationContext();
        mDevicePolicyManager = devicePolicyManager;
        mPackageManager = packageManager;
        mPackageManagerBinder = packageManagerBinder;
        mUserManager = userManager;
        mConnectivityManager = connectivityManagerWrapper;
        mResources = resources;
    }

    @Override
    public boolean hasDeviceOwner() {
        if (!mPackageManager.hasSystemFeature(PackageManager.FEATURE_DEVICE_ADMIN)) {
            return false;
        }
        return mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser() != null;
    }

    private int getManagedProfileUserId() {
        for (final UserInfo userInfo : mUserManager.getProfiles(MY_USER_ID)) {
            if (userInfo.isManagedProfile()) {
                return userInfo.id;
            }
        }
        return UserHandle.USER_NULL;
    }

    @Override
    public boolean isInCompMode() {
        return hasDeviceOwner() && getManagedProfileUserId() != UserHandle.USER_NULL;
    }

    @Override
    public String getDeviceOwnerOrganizationName() {
        final CharSequence organizationName = mDevicePolicyManager.getDeviceOwnerOrganizationName();
        return (organizationName == null) ? null : organizationName.toString();
    }

    @Override
    public CharSequence getDeviceOwnerDisclosure() {
        if (!hasDeviceOwner()) {
            return null;
        }

        final SpannableStringBuilder disclosure = new SpannableStringBuilder();
        final CharSequence organizationName = mDevicePolicyManager.getDeviceOwnerOrganizationName();
        if (organizationName != null) {
            disclosure.append(mResources.getString(R.string.do_disclosure_with_name,
                    organizationName));
        } else {
            disclosure.append(mResources.getString(R.string.do_disclosure_generic));
        }
        return disclosure;
    }

    @Override
    public Date getLastSecurityLogRetrievalTime() {
        final long timestamp = mDevicePolicyManager.getLastSecurityLogRetrievalTime();
        return timestamp < 0 ? null : new Date(timestamp);
    }

    @Override
    public Date getLastBugReportRequestTime() {
        final long timestamp = mDevicePolicyManager.getLastBugReportRequestTime();
        return timestamp < 0 ? null : new Date(timestamp);
    }

    @Override
    public Date getLastNetworkLogRetrievalTime() {
        final long timestamp = mDevicePolicyManager.getLastNetworkLogRetrievalTime();
        return timestamp < 0 ? null : new Date(timestamp);
    }

    @Override
    public boolean isSecurityLoggingEnabled() {
        return mDevicePolicyManager.isSecurityLoggingEnabled(null);
    }

    @Override
    public boolean isNetworkLoggingEnabled() {
        return mDevicePolicyManager.isNetworkLoggingEnabled(null);
    }

    @Override
    public boolean isAlwaysOnVpnSetInCurrentUser() {
        return mConnectivityManager.isAlwaysOnVpnSet(MY_USER_ID);
    }

    @Override
    public boolean isAlwaysOnVpnSetInManagedProfile() {
        final int managedProfileUserId = getManagedProfileUserId();
        return managedProfileUserId != UserHandle.USER_NULL &&
                mConnectivityManager.isAlwaysOnVpnSet(managedProfileUserId);
    }

    @Override
    public boolean isGlobalHttpProxySet() {
        return mConnectivityManager.getGlobalProxy() != null;
    }

    @Override
    public int getMaximumFailedPasswordsBeforeWipeInCurrentUser() {
        ComponentName owner = mDevicePolicyManager.getDeviceOwnerComponentOnCallingUser();
        if (owner == null) {
            owner = mDevicePolicyManager.getProfileOwnerAsUser(MY_USER_ID);
        }
        if (owner == null) {
            return 0;
        }
        return mDevicePolicyManager.getMaximumFailedPasswordsForWipe(owner, MY_USER_ID);
    }

    @Override
    public int getMaximumFailedPasswordsBeforeWipeInManagedProfile() {
        final int userId = getManagedProfileUserId();
        if (userId == UserHandle.USER_NULL) {
            return 0;
        }
        final ComponentName profileOwner = mDevicePolicyManager.getProfileOwnerAsUser(userId);
        if (profileOwner == null) {
            return 0;
        }
        return mDevicePolicyManager.getMaximumFailedPasswordsForWipe(profileOwner, userId);
    }

    @Override
    public String getImeLabelIfOwnerSet() {
        if (!mDevicePolicyManager.isCurrentInputMethodSetByOwner()) {
            return null;
        }
        final String packageName = Settings.Secure.getStringForUser(mContext.getContentResolver(),
                Settings.Secure.DEFAULT_INPUT_METHOD, MY_USER_ID);
        if (packageName == null) {
            return null;
        }
        try {
            return mPackageManager.getApplicationInfoAsUser(packageName, 0 /* flags */, MY_USER_ID)
                    .loadLabel(mPackageManager).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @Override
    public int getNumberOfOwnerInstalledCaCertsForCurrentUser() {
        final List<String> certs = mDevicePolicyManager.getOwnerInstalledCaCerts(
                new UserHandle(MY_USER_ID));
        if (certs == null) {
            return 0;
        }
        return certs.size();
    }

    @Override
    public int getNumberOfEnterpriseSetDefaultApps() {
        int num = 0;
        for (UserHandle user : mUserManager.getUserProfiles()) {
            for (EnterpriseDefaultApps app : EnterpriseDefaultApps.values()) {
                num += findPersistentPreferredActivities(
                        user.getIdentifier(),app.getIntents()).size();
            }
        }
        return num;
    }

    @Override
    public int getNumberOfOwnerInstalledCaCertsForManagedProfile() {
        final int userId = getManagedProfileUserId();
        if (userId == UserHandle.USER_NULL) {
            return 0;
        }
        final List<String> certs = mDevicePolicyManager.getOwnerInstalledCaCerts(
                new UserHandle(userId));
        if (certs == null) {
            return 0;
        }
        return certs.size();
    }

    @Override
    public int getNumberOfActiveDeviceAdminsForCurrentUserAndManagedProfile() {
        int activeAdmins = 0;
        for (final UserInfo userInfo : mUserManager.getProfiles(MY_USER_ID)) {
            final List<ComponentName> activeAdminsForUser
                    = mDevicePolicyManager.getActiveAdminsAsUser(userInfo.id);
            if (activeAdminsForUser != null) {
                activeAdmins += activeAdminsForUser.size();
            }
        }
        return activeAdmins;
    }

    public List<UserAppInfo> findPersistentPreferredActivities(int userId, Intent[] intents) {
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
            } catch (RemoteException exception) {}
        }
        return preferredActivities;
    }

    @Override
    public void listPolicyInstalledApps(ListOfAppsCallback callback) {
        final AppLister lister = new AppLister(mDevicePolicyManager, mPackageManager,
                mPackageManagerBinder, mUserManager, PackageManager.INSTALL_REASON_POLICY,
                null /* adminGrantedPermissions */, callback);
        lister.execute();
    }

    @Override
    public void listAppsWithAdminGrantedPermissions(String[] permissions, ListOfAppsCallback callback) {
        final AppLister lister = new AppLister(mDevicePolicyManager, mPackageManager,
                mPackageManagerBinder, mUserManager, AppLister.IGNORE_INSTALL_REASON,
                permissions, callback);
        lister.execute();
    }

    protected static class EnterprisePrivacySpan extends ClickableSpan {
        private final Context mContext;

        public EnterprisePrivacySpan(Context context) {
            mContext = context;
        }

        @Override
        public void onClick(View widget) {
            mContext.startActivity(new Intent(Settings.ACTION_ENTERPRISE_PRIVACY_SETTINGS));
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof EnterprisePrivacySpan
                    && ((EnterprisePrivacySpan) object).mContext == mContext;
        }
    }
}
