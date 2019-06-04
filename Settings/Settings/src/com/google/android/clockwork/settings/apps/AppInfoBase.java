package com.google.android.clockwork.settings.apps;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.PreferenceFragment;
import android.text.TextUtils;
import android.util.Log;

import com.android.settingslib.applications.ApplicationsState;
import com.google.android.apps.wearable.settings.R;

import java.util.ArrayList;

/**
 * Base class for any PreferenceFragment that is specific to an application and needs information
 * from ApplicationsState.
 * PackageName could be specified as:
 * - Either an Intent extra with key "package"
 * - As a URI: package://<pkgName>
 * In addition, the Intent could be passed in within the arguments of the Fragment as well.
 */
public abstract class AppInfoBase extends PreferenceFragment
        implements ApplicationsState.Callbacks {
    private static final String TAG = "AppInfoBase";
    public static final String ARG_PACKAGE_NAME = "package";

    protected ApplicationsState mState;
    protected ApplicationsState.Session mSession;
    protected ApplicationsState.AppEntry mAppEntry;
    protected PackageInfo mPackageInfo;
    protected PackageManager mPm;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle args = getArguments();
        String packageName = (args != null) ? args.getString(ARG_PACKAGE_NAME) : null;
        if (packageName == null) {
            Intent intent = (args == null) ?
                    getActivity().getIntent() : (Intent) args.getParcelable("intent");
            if (intent != null && intent.getData() != null) {
                packageName = intent.getData().getSchemeSpecificPart();
            }
        }

        if (TextUtils.isEmpty(packageName)) {
            throw new IllegalArgumentException("Package name is not present. Please set a Uri "
                    + "with the format package:<package name>");
        }

        mPm = getActivity().getPackageManager();
        if(mState == null) {
            mState = ApplicationsState.getInstance(getActivity().getApplication());
        }
        if(mSession == null) {
            mSession = mState.newSession(this /* ApplicationsState.Callback */);
        }

        mAppEntry = mState.getEntry(packageName, UserHandle.myUserId());
        if (mAppEntry != null) {
            // Get application info again to refresh changed properties of application
            try {
                mPackageInfo = mPm.getPackageInfo(mAppEntry.info.packageName,
                        PackageManager.GET_DISABLED_COMPONENTS |
                                PackageManager.GET_UNINSTALLED_PACKAGES |
                                PackageManager.GET_SIGNATURES |
                                PackageManager.GET_PERMISSIONS);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Exception when retrieving package:" + mAppEntry.info.packageName, e);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mSession.onResume();
        refreshAppSize();
    }

    @Override
    public void onPause() {
        mSession.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mSession.onDestroy();
        super.onDestroy();
    }

    public abstract void refreshPrefs(ApplicationsState.AppEntry appEntry);

    // Callbacks from ApplicationsState.Callbacks
    @Override
    public void onRunningStateChanged(boolean running) {}

    @Override
    public void onPackageListChanged() {
        refreshPrefs(mAppEntry);
    }

    @Override
    public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> apps) {}

    @Override
    public void onPackageIconChanged() {}

    @Override
    public void onPackageSizeChanged(String packageName) {}

    @Override
    public void onAllSizesComputed() {}

    @Override
    public void onLauncherInfoChanged() {}

    @Override
    public void onLoadEntriesCompleted() {}

    protected void refreshAppSize() {
        if (mAppEntry != null && mAppEntry.info != null) {
            mState.requestSize(
                    mAppEntry.info.packageName, UserHandle.getUserId(mAppEntry.info.uid));
        }
    }
}
