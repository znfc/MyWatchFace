package com.google.android.clockwork.settings.apps;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.Log;

import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

import com.google.android.apps.wearable.settings.R;
import com.google.common.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * @author shreerag@google.com
 */
public class AppStorageSettingsFragment extends AppsListFragment {
    private static final String TAG = "AppStorageSettings";

    private static final boolean DEFAULT_SORT_DATA = true;

    private static final Comparator<AppEntry> DATA_COMPARATOR = new Comparator<AppEntry>() {
        @Override
        public int compare(AppEntry object1, AppEntry object2) {
            if (getDataSize(object1) < getDataSize(object2)) {
                return 1;
            }
            if (getDataSize(object1) > getDataSize(object2)) {
                return -1;
            }
            return ApplicationsState.ALPHA_COMPARATOR.compare(object1, object2);
        }
    };

    private static final Comparator<AppEntry> SIZE_COMPARATOR = new Comparator<AppEntry>() {
        @Override
        public int compare(AppEntry object1, AppEntry object2) {
            if (getAppSize(object1) < getAppSize(object2)) {
                return 1;
            }
            if (getAppSize(object1) > getAppSize(object2)) {
                return -1;
            }
            return ApplicationsState.ALPHA_COMPARATOR.compare(object1, object2);
        }
    };

    @VisibleForTesting
    boolean mCurrentSort = DEFAULT_SORT_DATA; //True for data, false for app

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
        return ApplicationsState.FILTER_EVERYTHING;
    }

    @Override
    protected void onPostLoadedApps() {
        initSortPref(getPreferenceScreen());
    }

    private static long getDataSize(AppEntry entry) {
        return entry.dataSize + entry.cacheSize;
    }

    private static long getAppSize(AppEntry entry) {
        return entry.apkFile.length();
    }

    @Override
    protected int getAppsTitleResId() {
        return R.string.app_info_label_storage;
    }

    @Override
    protected Comparator<AppEntry> getAppEntryComparator() {
        return mCurrentSort ? DATA_COMPARATOR : SIZE_COMPARATOR;
    }

    /** Sets up each preference entry to open a {@link UsageAccessInfoFragment}. */
    @Override
    protected void onAppPrefCreated(Preference pref, ApplicationsState.AppEntry appEntry) {
        super.onAppPrefCreated(pref, appEntry);
        pref.setSummary(AppDetailsAboutFragment.getSizeStr(getContext(),
                mCurrentSort ? getDataSize(appEntry) : getAppSize(appEntry)));
        pref.setFragment(AppDetailsAboutFragment.class.getName());
    }

    @VisibleForTesting
    void initSortPref(PreferenceScreen prefScreen) {

        SwitchPreference pref = new SwitchPreference(getContext());

        pref.setChecked(mCurrentSort);
        pref.setTitle(mCurrentSort ? R.string.storage_sort_data : R.string.storage_sort_app);

        pref.setOnPreferenceChangeListener((p, newVal) -> {
            mCurrentSort = (Boolean) newVal;
            updateAppList();
            return true;
        });

        prefScreen.addPreference(pref);
        pref.setOrder(-1);
    }
}
