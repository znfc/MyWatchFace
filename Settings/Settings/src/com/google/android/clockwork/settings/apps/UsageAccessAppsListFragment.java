package com.google.android.clockwork.settings.apps;

import android.Manifest;
import android.app.ActivityThread;
import android.app.AppOpsManager;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.Preference;
import android.util.Log;
import com.android.internal.util.ArrayUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.google.android.apps.wearable.settings.R;

/**
 * Shows a list of apps that opens screen to allow user to adjust the usage access settings of the
 * selected app.
 * <p>
 * List filters to only apps the declare {@link Manifest.permission.PACKAGE_USAGE_STATS}.
 */
public class UsageAccessAppsListFragment extends AppsListFragment {
    private static final String TAG = "UsageAccessAppsListFrag";

    private IPackageManager mIPackageManager;
    private AppOpsManager mAppOpsManager;
    /** The package name of this package (i.e. the Settings package) so that we can skip it. */
    private String mPackageName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mIPackageManager = ActivityThread.getPackageManager();
        mAppOpsManager = getContext().getSystemService(AppOpsManager.class);
        mPackageName = getContext().getPackageName();
        super.onCreate(savedInstanceState);
    }

    /**
     * Filters apps to exclude apps that do not declare the given permission (see
     * {@link #getPermission()}).
     * <p>
     * Also skips the main android app and the Settings (i.e. this) app.
     * <p>
     * Filter will add a {@link PermissionState} object to {@link ApplicationsState.AppEntry.info}.
     */
    @Override
    protected ApplicationsState.AppFilter getAppFilter() {
        return new ApplicationsState.AppFilter() {
            @Override
            public void init() {
            }

            @Override
            public boolean filterApp(ApplicationsState.AppEntry entry) {
                entry.extraInfo = createPermissionStateFor(entry.info.packageName, entry.info.uid);
                return !shouldIgnorePackage(entry.info.packageName)
                        && ((PermissionState) entry.extraInfo).isPermissible();
            }
        };
    }

    /** Sets up each preference entry to open a {@link UsageAccessInfoFragment}. */
    @Override
    protected void onAppPrefCreated(Preference pref, ApplicationsState.AppEntry appEntry) {
        super.onAppPrefCreated(pref, appEntry);
        pref.setFragment(UsageAccessInfoFragment.class.getName());
    }

    @Override
    protected int getAppsTitleResId() {
        return R.string.usage_access_title;
    }

    /**
     * @return AppOps code. Should be what's used to store the state of the permission.
     */
    public int getAppOpsOpCode() {
        return AppOpsManager.OP_GET_USAGE_STATS;
    }

    /**
     * @return Manifest permission string.
     */
    public String getPermission() {
        return Manifest.permission.PACKAGE_USAGE_STATS;
    }

    /**
     * @return {@code true} if the app has asked for the given permission.
     */
    private boolean hasRequestedAppOpPermission(String permission, String packageName) {
        try {
            String[] packages = mIPackageManager.getAppOpPermissionPackages(permission);
            return ArrayUtils.contains(packages, packageName);
        } catch (RemoteException exc) {
            Log.e(TAG, "PackageManager dead. Cannot get permission info");
            return false;
        }
    }

    /**
     * @return {@code true} if the given uid has permission defined in {@link #getPermission()}.
     */
    private boolean hasPermission(int uid) {
        try {
            int result = mIPackageManager.checkUidPermission(getPermission(), uid);
            return result == PackageManager.PERMISSION_GRANTED;
        } catch (RemoteException e) {
            Log.e(TAG, "PackageManager dead. Cannot get permission info");
            return false;
        }
    }

    /**
     * @return the app op mode (e.g. the state of the permission).
     */
    private int getAppOpMode(int uid, String packageName) {
        return mAppOpsManager.checkOpNoThrow(getAppOpsOpCode(), uid, packageName);
    }

    /**
     * @return a {@link PermissionState} object built from the given package name and uid.
     */
    private PermissionState createPermissionStateFor(String packageName, int uid) {
        return new PermissionState(
                hasRequestedAppOpPermission(getPermission(), packageName),
                hasPermission(uid),
                getAppOpMode(uid, packageName));
    }

    /*
     * Checks for packages that should be ignored for further processing
     */
    private boolean shouldIgnorePackage(String packageName) {
        return packageName.equals("android") || packageName.equals(mPackageName);
    }

    /**
     * Collection of information to be used as {@link ApplicationsState.AppEntry#extraInfo} objects.
     */
    protected static class PermissionState {
        public final boolean permissionRequested;
        public final boolean permissionGranted;
        public final int appOpMode;

        private PermissionState(boolean permissionRequested, boolean permissionGranted,
                int appOpMode) {
            this.permissionRequested = permissionRequested;
            this.permissionGranted = permissionGranted;
            this.appOpMode = appOpMode;
        }

        /**
         * @return {@code true} if the permission is granted
         */
        public boolean isAllowed() {
            if (appOpMode == AppOpsManager.MODE_DEFAULT) {
                return permissionGranted;
            } else {
                return appOpMode == AppOpsManager.MODE_ALLOWED;
            }
        }

        /**
         * @return {@code true} if the permission is relevant
         */
        public boolean isPermissible() {
            return permissionRequested;
        }

        @Override
        public String toString() {
            return "[permissionGranted: " + permissionGranted
                    + ", permissionRequested: " + permissionRequested
                    + ", appOpMode: " + appOpMode
                    + "]";
        }
    }
}
