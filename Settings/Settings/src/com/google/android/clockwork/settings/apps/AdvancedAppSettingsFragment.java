package com.google.android.clockwork.settings.apps;

import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.PreferenceFragment;
import android.support.wearable.preference.AcceptDenySwitchPreference;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;

import java.util.List;

/**
 * App settings screen that handles advanced permissions.  Heavily based on DrawOverlayDetails.java
 * and WriteSettingsDetails.java in the handset's Settings app.
 */
public class AdvancedAppSettingsFragment extends SettingsPreferenceFragment {
    public static final String TAG = "AdvancedAppSettings";

    private static String KEY_PREF_DRAW_OVERLAY = "pref_advancedPermissions_drawOverlay";
    private static String KEY_PREF_WRITE_SETTINGS = "pref_advancedPermissions_writeSettings";

    private AppOpsManager mAppOpsManager;
    private PackageManager mPackageManager;
    private String mPackageName;
    private PackageInfo mPackageInfo;
    private PermissionState mWriteSettingsState;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.prefs_advanced_permissions);

        final Activity activity = getActivity();
        mAppOpsManager = (AppOpsManager) activity.getSystemService(Context.APP_OPS_SERVICE);
        mPackageManager = activity.getPackageManager();

        final Bundle args = getArguments();
        mPackageName = (args != null) ? args.getString(AppInfoBase.ARG_PACKAGE_NAME) : null;
        if (mPackageName == null) {
            Intent intent = (args == null) ?
                    activity.getIntent() : (Intent) args.getParcelable("intent");
            if (intent != null && intent.getData() != null) {
                mPackageName = intent.getData().getSchemeSpecificPart();
            }
        }

        if (TextUtils.isEmpty(mPackageName)) {
            Log.e(TAG, "Package name is not present. Please set a Uri with the format " +
                "package:<package name>");
            closeFragment();
        }

        // Get application info
        try {
            mPackageInfo = mPackageManager.getPackageInfo(mPackageName,
                    PackageManager.GET_DISABLED_COMPONENTS |
                            PackageManager.GET_UNINSTALLED_PACKAGES |
                            PackageManager.GET_SIGNATURES |
                            PackageManager.GET_PERMISSIONS |
                            PackageManager.MATCH_UNINSTALLED_PACKAGES);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Exception when retrieving package:" + mPackageName, e);
            // Remove this fragment if PackageInfo can't be retrieved (b/29587000)
            closeFragment();
        }

        initDrawOverlay((AcceptDenySwitchPreference) findPreference(KEY_PREF_DRAW_OVERLAY));
        initWriteSettings((AcceptDenySwitchPreference) findPreference(KEY_PREF_WRITE_SETTINGS));
    }

    private void initDrawOverlay(AcceptDenySwitchPreference pref) {
        if (mPackageInfo != null) {
            PermissionState permissionState = getPermissionInfo(
                    mPackageName,
                    mPackageInfo.applicationInfo.uid,
                    android.Manifest.permission.SYSTEM_ALERT_WINDOW,
                    AppOpsManager.OP_SYSTEM_ALERT_WINDOW);
            pref.setChecked(permissionState.isPermissible());
            // you cannot ask a user to grant you a permission you did not have!
            pref.setEnabled(permissionState.permissionDeclared);
            pref.setOnPreferenceChangeListener((p, newVal) -> {
                mAppOpsManager.setMode(
                        AppOpsManager.OP_SYSTEM_ALERT_WINDOW,
                        mPackageInfo.applicationInfo.uid,
                        mPackageName,
                        ((Boolean) newVal) ? AppOpsManager.MODE_ALLOWED : AppOpsManager.MODE_ERRORED);
                return true;
            });
        } else {
            pref.setChecked(false);
            pref.setEnabled(false);
        }
    }

    private void initWriteSettings(AcceptDenySwitchPreference pref) {
        if (mPackageInfo != null) {
            PermissionState permissionState = getPermissionInfo(
                    mPackageName,
                    mPackageInfo.applicationInfo.uid,
                    android.Manifest.permission.WRITE_SETTINGS,
                    AppOpsManager.OP_WRITE_SETTINGS);
            pref.setChecked(permissionState.isPermissible());
            // you cannot ask a user to grant you a permission you did not have!
            pref.setEnabled(permissionState.permissionDeclared);
            pref.setOnPreferenceChangeListener((p, newVal) -> {
                mAppOpsManager.setMode(
                        AppOpsManager.OP_WRITE_SETTINGS,
                        mPackageInfo.applicationInfo.uid,
                        mPackageName,
                        ((Boolean) newVal) ? AppOpsManager.MODE_ALLOWED : AppOpsManager.MODE_ERRORED);
                return true;
            });
        } else {
            pref.setChecked(false);
            pref.setEnabled(false);
        }
    }

    private PermissionState getPermissionInfo(String pkg, int uid, String permission,
            int appOppsCode) {
        PermissionState permissionState = new PermissionState(pkg, new UserHandle(UserHandle
                .getUserId(uid)));
        permissionState.packageInfo = mPackageInfo;
        // Check static permission state (whatever that is declared in package manifest)
        String[] requestedPermissions = permissionState.packageInfo.requestedPermissions;
        int[] permissionFlags = permissionState.packageInfo.requestedPermissionsFlags;
        if (requestedPermissions != null) {
            for (int i = 0; i < requestedPermissions.length; i++) {
                if (TextUtils.equals(requestedPermissions[i], permission)) {
                    permissionState.permissionDeclared = true;
                    if ((permissionFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0) {
                        permissionState.staticPermissionGranted = true;
                        break;
                    }
                }
            }
        }
        // Check app op state.
        List<AppOpsManager.PackageOps> ops =
                mAppOpsManager.getOpsForPackage(uid, pkg, new int[] { appOppsCode });
        if (ops != null && ops.size() > 0 && ops.get(0).getOps().size() > 0) {
            permissionState.appOpMode = ops.get(0).getOps().get(0).getMode();
        }
        return permissionState;
    }

    private void closeFragment() {
        getActivity().getFragmentManager().beginTransaction().remove(this).commit();
    }

    private static class PermissionState {
        final String packageName;
        final UserHandle userHandle;
        PackageInfo packageInfo;
        boolean staticPermissionGranted;
        boolean permissionDeclared;
        int appOpMode;

        PermissionState(String packageName, UserHandle userHandle) {
            this.packageName = packageName;
            this.appOpMode = AppOpsManager.MODE_DEFAULT;
            this.userHandle = userHandle;
        }

        boolean isPermissible() {
            // defining the default behavior as permissible as long as the package requested this
            // permission (this means pre-M gets approval during install time; M apps gets approval
            // during runtime.
            if (appOpMode == AppOpsManager.MODE_DEFAULT) {
                return staticPermissionGranted;
            }
            return appOpMode == AppOpsManager.MODE_ALLOWED;
        }
    }
}
