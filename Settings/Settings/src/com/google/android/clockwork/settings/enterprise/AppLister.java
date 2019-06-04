package com.google.android.clockwork.settings.enterprise;

import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;

import java.util.ArrayList;
import java.util.List;

import com.google.android.clockwork.settings.enterprise.EnterprisePrivacyFeatureProvider.ListOfAppsCallback;

/**
 * Lister to get a list of installed apps filtered based on an install reason and/or a list of
 * admin-granted permissions.
 */
public class AppLister extends AsyncTask<Void, Void, List<UserAppInfo>> {

    private final DevicePolicyManagerWrapper mDevicePolicyManager;
    private final PackageManager mPackageManager;
    private final IPackageManager mPackageManagerBinder;
    private final UserManager mUserManager;

    private final int mInstallReason;
    private String[] mPermissions;
    private ListOfAppsCallback mCallback;

    /**
     * Count all installed packages, irrespective of install reason.
     */
    public static final int IGNORE_INSTALL_REASON = -1;

    AppLister(DevicePolicyManagerWrapper devicePolicyManager, PackageManager packageManager,
            IPackageManager packageManagerBinder, UserManager userManager, int installReason,
            String[] adminGrantedPermissions, ListOfAppsCallback callback) {
        mDevicePolicyManager = devicePolicyManager;
        mPackageManager = packageManager;
        mPackageManagerBinder = packageManagerBinder;
        mUserManager = userManager;
        mInstallReason = installReason;
        mPermissions = adminGrantedPermissions;
        mCallback = callback;
    }

    @Override
    protected List<UserAppInfo> doInBackground(Void... params) {
        final List<UserAppInfo> result = new ArrayList<>();
        for (UserInfo user : mUserManager.getProfiles(UserHandle.myUserId())) {
            final List<ApplicationInfo> list =
                    mPackageManager.getInstalledApplicationsAsUser(PackageManager.GET_DISABLED_COMPONENTS
                            | PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS
                            | (user.isAdmin() ? PackageManager.MATCH_ANY_USER : 0),
                            user.id);
            for (ApplicationInfo info : list) {
                if (includeInCount(info)) {
                    result.add(new UserAppInfo(user, info));
                }
            }
        }
        return result;
    }

    protected boolean includeInCount(ApplicationInfo info) {
        return includeInCount(mInstallReason, info)
                && (mPermissions == null || includeInCount(mPermissions, info));
    }

    protected boolean includeInCount(int installReason, ApplicationInfo info) {
        final int userId = UserHandle.getUserId(info.uid);
        if (installReason != IGNORE_INSTALL_REASON
                && mPackageManager.getInstallReason(info.packageName,
                new UserHandle(userId)) != installReason) {
            return false;
        }
        if ((info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
            return true;
        }
        if ((info.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
            return true;
        }
        Intent launchIntent = new Intent(Intent.ACTION_MAIN, null)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage(info.packageName);
        List<ResolveInfo> intents = mPackageManager.queryIntentActivitiesAsUser(
                launchIntent,
                PackageManager.GET_DISABLED_COMPONENTS
                | PackageManager.MATCH_DIRECT_BOOT_AWARE
                | PackageManager.MATCH_DIRECT_BOOT_UNAWARE,
                userId);
        return intents != null && intents.size() != 0;
    }

    protected boolean includeInCount(String[] permissions, ApplicationInfo info) {
        if (info.targetSdkVersion >= Build.VERSION_CODES.M) {
            // The app uses run-time permissions. Check whether one or more of the permissions were
            // granted by enterprise policy.
            for (final String permission : permissions) {
                if (mDevicePolicyManager.getPermissionGrantState(null /* admin */, info.packageName,
                            permission) == DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED) {
                    return true;
                }
            }
            return false;
        }

        // The app uses install-time permissions. Check whether the app requested one or more of the
        // permissions and was installed by enterprise policy, implicitly granting permissions.
        if (mPackageManager.getInstallReason(info.packageName,
                    new UserHandle(UserHandle.getUserId(info.uid)))
                != PackageManager.INSTALL_REASON_POLICY) {
            return false;
        }
        try {
            for (final String permission : permissions) {
                if (mPackageManagerBinder.checkUidPermission(permission, info.uid)
                        == PackageManager.PERMISSION_GRANTED) {
                    return true;
                }
            }
        } catch (RemoteException exception) {
        }
        return false;
    }

    @Override
    protected void onPostExecute(List<UserAppInfo> list) {
        mCallback.onListOfAppsResult(list);
    }
}
