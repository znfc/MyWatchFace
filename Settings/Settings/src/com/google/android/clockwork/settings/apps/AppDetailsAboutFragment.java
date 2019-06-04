package com.google.android.clockwork.settings.apps;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageDataObserver;
import android.os.Bundle;
import android.os.Environment;
import android.os.UserHandle;
import android.preference.Preference;
import android.support.wearable.preference.AcceptDenyDialogPreference;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Toast;

import com.google.android.apps.wearable.settings.R;

import com.android.settingslib.applications.ApplicationsState;

import com.google.common.collect.ImmutableSet;

import static com.android.settingslib.applications.ApplicationsState.SIZE_INVALID;

public class AppDetailsAboutFragment extends AppInfoBase
        implements ApplicationsState.Callbacks {
    private static final String TAG = "AppAboutFragment";

    private static final String KEY_PREF_VERSION = "pref_version";
    private static final String KEY_PREF_STORAGE = "pref_storage";
    private static final String KEY_PREF_DATA = "pref_data";
    private static final String KEY_PREF_CACHE = "pref_cache";
    private static final String KEY_PREF_CLEAR_DATA = "pref_clear_data";
    private static final String KEY_PREF_CLEAR_CACHE = "pref_clear_cache";
    private static final String KEY_PREF_UNINSTALL = "pref_uninstall";

    // TODO: Add support for this to the manifest
    private static final ImmutableSet<String> CLEAR_DATA_NOT_SUPPORTED = ImmutableSet.of(
            "com.google.android.gms"
    );

    // Resource strings

    private IPackageDataObserver mClearDataObserver = new IPackageDataObserver.Stub() {
        public void onRemoveCompleted(final String packageName, final boolean succeeded) {
            showToast(R.string.app_info_toast_clear_data);
            refreshAppSize();
        }
    };

    private IPackageDataObserver mClearCacheObserver = new IPackageDataObserver.Stub() {
        public void onRemoveCompleted(final String packageName, final boolean succeeded) {
            showToast(R.string.app_info_toast_clear_cache);
            refreshAppSize();
        }
    };

    private void showToast(int stringResource) {
        if (getActivity() != null) {
            final Activity activity = getActivity();
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity,stringResource, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_app_details_about);
        refreshPrefs(mAppEntry);
    }

    @Override
    public void refreshPrefs(ApplicationsState.AppEntry appEntry) {
        initVersion();
        initActions();
        initUninstall();
        refreshSizeInfoAndActions();

    }

    private void initVersion() {
        if (mAppEntry != null) {
            Preference versionPref = findPreference(KEY_PREF_VERSION);
            versionPref.setSummary(mAppEntry.getVersion(getContext()));
        }

        refreshSizeInfoAndActions();
    }

    private void initActions() {
        AcceptDenyDialogPreference clearDataPref =
                (AcceptDenyDialogPreference) findPreference(KEY_PREF_CLEAR_DATA);

        if (mAppEntry == null) {
            Log.w(TAG, "AppEntry is null before init, not expected.");
            return;
        }

        // If the App has its own managing space activity, remove the Dialog, etc.
        if (mAppEntry.info.manageSpaceActivityName != null) {
            clearDataPref.setOnPreferenceClickListener((p) -> {
                if (!ActivityManager.isUserAMonkey()) {
                    Intent intent = new Intent(Intent.ACTION_DEFAULT);
                    intent.setClassName(mAppEntry.info.packageName,
                            mAppEntry.info.manageSpaceActivityName);
                    startActivityForResult(intent, 0 /* unused */);
                }
                return true;
            });
        } else {
            clearDataPref.setOnDialogClosedListener((positiveResult) -> {
                if (positiveResult && getActivity() != null) {
                    ActivityManager am = (ActivityManager)
                            getActivity().getSystemService(Context.ACTIVITY_SERVICE);
                    am.clearApplicationUserData(mAppEntry.info.packageName, mClearDataObserver);
                }
            });
        }

        findPreference(KEY_PREF_CLEAR_CACHE).setOnPreferenceClickListener((p) -> {
            if (mAppEntry != null) {
                mPm.deleteApplicationCacheFiles(mAppEntry.info.packageName, mClearCacheObserver);
            }
            return true;
        });
    }

    private void initUninstall() {
        AppDetailsFragment.refreshUninstall(getActivity(),
                (AcceptDenyDialogPreference) findPreference(KEY_PREF_UNINSTALL), mAppEntry,
                mPackageInfo, false);
    }

    private void refreshSizeInfoAndActions() {
        if (mAppEntry == null) {
            Log.w(TAG, "AppEntry is null before retrieval, not expected.");
            return;
        }
        mAppEntry = mState.getEntry(mAppEntry.info.packageName,
                UserHandle.getUserId(mAppEntry.info.uid));
        if (mAppEntry == null) {
            Log.w(TAG, "AppEntry is null after retrieval, not expected.");
            return;
        }
        Preference storagePref = findPreference(KEY_PREF_STORAGE);
        Preference dataPref = findPreference(KEY_PREF_DATA);
        Preference cachePref = findPreference(KEY_PREF_CACHE);
        Preference clearDataPref = findPreference(KEY_PREF_CLEAR_DATA);
        Preference clearCachePref = findPreference(KEY_PREF_CLEAR_CACHE);

        if (mAppEntry.size == SIZE_INVALID
                || mAppEntry.size == ApplicationsState.SIZE_UNKNOWN) {
            storagePref.setSummary(R.string.computing_size);
            dataPref.setSummary(R.string.computing_size);
            cachePref.setSummary(R.string.computing_size);
            clearDataPref.setEnabled(false);
            clearCachePref.setEnabled(false);
        } else {
            long codeSize = mAppEntry.codeSize;
            long dataSize = mAppEntry.dataSize;
            long cacheSize = mAppEntry.cacheSize;
            if (Environment.isExternalStorageEmulated()) {
                codeSize += mAppEntry.externalCodeSize;
                dataSize += mAppEntry.externalDataSize;
                cacheSize += mAppEntry.externalCacheSize;
            }

            storagePref.setSummary(getSizeStr(getContext(), codeSize));
            dataPref.setSummary(getSizeStr(getContext(), dataSize));
            cachePref.setSummary(getSizeStr(getContext(), cacheSize));

            if (CLEAR_DATA_NOT_SUPPORTED.contains(mAppEntry.info.packageName)
                    || (mAppEntry.dataSize + mAppEntry.externalDataSize) <= 0) {
                clearDataPref.setEnabled(false);
            } else {
                clearDataPref.setEnabled(true);
            }
            if (cacheSize <= 0) {
                clearCachePref.setEnabled(false);
            } else {
                clearCachePref.setEnabled(true);
            }
        }
    }

    // Callbacks from ApplicationsState.Callbacks
    @Override
    public void onPackageSizeChanged(String packageName) {
        super.onPackageSizeChanged(packageName);
        if (TextUtils.equals(mAppEntry.info.packageName, packageName)) {
            refreshSizeInfoAndActions();
        }
    }

    @Override
    public void onAllSizesComputed() {
        super.onAllSizesComputed();
        refreshSizeInfoAndActions();
    }

    static String getSizeStr(Context c, long size) {
        if (size == SIZE_INVALID) {
            return c.getString(R.string.invalid_size_value);
        }
        return Formatter.formatFileSize(c, size);
    }
}
