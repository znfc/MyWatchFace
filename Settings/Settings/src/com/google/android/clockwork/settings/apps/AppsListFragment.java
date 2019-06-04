package com.google.android.clockwork.settings.apps;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.settingslib.applications.ApplicationsState;
import com.google.android.apps.wearable.settings.R;
import com.google.common.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A list of apps installed on the device.
 * <p>
 * Includes filtering for system apps and appends an option to the end of the list to list system
 * apps. Subclasses should override onAppPrefCreated() to handle app taps.
 */
public abstract class AppsListFragment extends PreferenceFragment {
    private static final String TAG = "AppsListFrag";

    private static final String KEY_PREF_APP_PREFIX = "pref_app_";
    private static final String KEY_PREF_MORE_APPS = "pref_moreApps";
    private static final String EXTRA_IS_SYSTEM_APPS_VIEW = "isSystemAppsView";

    private ApplicationsState mState;
    private ApplicationsState.Session mSession;

    @VisibleForTesting boolean mIsSystemAppView = false;
    private boolean mHasSystemApps = false;

    private ApplicationsState.Callbacks mCallback = new ApplicationsState.Callbacks() {
        @Override
        public void onRunningStateChanged(boolean running) {}

        @Override
        public void onPackageListChanged() {
            updateAppList();
        }

        @Override
        public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> apps) {
            showLoadedApps(getPreferenceScreen(), apps);
        }

        @Override
        public void onPackageIconChanged() {
            updateAppList();
        }

        @Override
        public void onPackageSizeChanged(String packageName) {
            updateAppList();
        }

        @Override
        public void onAllSizesComputed() {
            updateAppList();
        }

        @Override
        public void onLauncherInfoChanged() {
            updateAppList();
        }

        @Override
        public void onLoadEntriesCompleted() {
            updateAppList();
        }
    };

    /**
     * Updates the apps list by requesting a new list of apps and then showing the laoded apps.
     */
    protected void updateAppList() {
        ApplicationsState.AppFilter filter = new ApplicationsState.CompoundFilter(getAppFilter(),
                ApplicationsState.FILTER_NOT_HIDE);
        ArrayList<ApplicationsState.AppEntry> apps = mSession.rebuild(
                filter, getAppEntryComparator());
        if (apps != null) {
            showLoadedApps(getPreferenceScreen(), apps);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mState == null) {
            mState = ApplicationsState.getInstance(getActivity().getApplication());
        }
        mSession = mState.newSession(mCallback);

        Bundle args = getArguments();
        if (args != null) {
            mIsSystemAppView = getArguments().getBoolean(EXTRA_IS_SYSTEM_APPS_VIEW, false);
        }

        addPreferencesFromResource(R.xml.prefs_app_info);
        getPreferenceScreen().setTitle(
                mIsSystemAppView ? getSystemAppsTitleResId() : getAppsTitleResId());
    }

    /**
     * Generates filter to be used when loading apps.
     * <p>
     * By default, the filter will alternately filter out filter out all system apps if
     * {@link #EXTRA_IS_SYSTEM_APPS_VIEW} is not {@code true}. If any system app is filtered,
     * {@link #onPostLoadedApps()} will add a preference to the bottom of the list to open the same
     * fragment with {@link #EXTRA_IS_SYSTEM_APPS_VIEW} set to {@code true}, and this filter will
     * filter out apps that aren't system apps.
     *
     * @return filter to be used to filter out apps list.
     */
    protected ApplicationsState.AppFilter getAppFilter() {
        return new ApplicationsState.AppFilter() {
            @Override
            public void init() {
            }

            @Override
            public boolean filterApp(ApplicationsState.AppEntry entry) {
                boolean isSystem = (entry.info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                if (isSystem && !mHasSystemApps) {
                    mHasSystemApps = true;
                }
                return mIsSystemAppView == isSystem;
            }
        };
    }

    @VisibleForTesting
    void setStateAndSession(ApplicationsState state, ApplicationsState.Session session) {
        mState = state;
        mSession = session;
    }

    @Override
    public void onResume() {
        super.onResume();
        mSession.onResume();
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

    /**
     * @return the default resource ID to use for the title of the screen.
     */
    protected abstract int getAppsTitleResId();

    /**
     * The title to use when the screen is the system apps screen. Defaults to be the same as the
     * normal title screen.
     */
    protected int getSystemAppsTitleResId() {
        return getAppsTitleResId();
    }

    /**
     * Called after apps are loaded by {@link #showLoadedApps}.
     * <p>
     * By default, if any system app is filtered and {@link #EXTRA_IS_SYSTEM_APPS_VIEW} set to
     * {@code false}, a preference would be added to the bottom of the list to open the same
     * fragment with {@link #EXTRA_IS_SYSTEM_APPS_VIEW} set to {@code true}.
     */
    protected void onPostLoadedApps() {
        if (!mIsSystemAppView && mHasSystemApps) {
            Preference moreAppsPref = new Preference(getActivity());
            moreAppsPref.setKey(KEY_PREF_MORE_APPS);

            moreAppsPref.setTitle(R.string.system_apps_settings);
            moreAppsPref.setIcon(R.drawable.ic_cc_settings_morehorizontal);
            moreAppsPref.setFragment(getClass().getName());
            moreAppsPref.getExtras().putBoolean(EXTRA_IS_SYSTEM_APPS_VIEW, true);
            moreAppsPref.setOrder(Integer.MAX_VALUE);

            getPreferenceScreen().addPreference(moreAppsPref);
        }
    }

    /**
     * Main method to handle the processing to display a loaded list of apps. The method clears all
     * of its current contents and then populates it with the new apps list.
     * <p>
     * Calls {@link #onPostLoadedApps()} after all apps are added.
     */
    @VisibleForTesting
    void showLoadedApps(PreferenceScreen prefScreen, List<ApplicationsState.AppEntry> apps) {
        prefScreen.removeAll();

        if (apps == null || apps.isEmpty()) {
            // There are no apps to show
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "There are no apps to show.");
            }
        } else {
            for (int i = 0, size = apps.size(); i < size; i++) {
                final ApplicationsState.AppEntry appEntry = apps.get(i);

                Preference appPref = addToAppsPref(prefScreen, appEntry);
                appPref.setOrder(i);
            }
        }

        onPostLoadedApps();
    }

    /**
     * Creates and add a Preference object representing the app entry to the PreferenceScreen.
     * <p>
     * Method should call onAppPrefCreated when the object is created.
     */
    protected Preference addToAppsPref(PreferenceScreen prefScreen,
            ApplicationsState.AppEntry appEntry) {
        Preference appPref = new Preference(getActivity());

        appPref.setTitle(appEntry.label);
        mState.ensureIcon(appEntry);
        appPref.setIcon(appEntry.icon);
        appPref.getExtras().putString(AppInfoBase.ARG_PACKAGE_NAME, appEntry.info.packageName);

        onAppPrefCreated(appPref, appEntry);
        prefScreen.addPreference(appPref);
        return appPref;
    }

    /**
     * Called when a Preference representing an app entry is created and about to be added to the
     * list. Generally this is where things should be overridden to launch a child fragment. Extras
     * will already have been added to the preference with the app entry's package name.
     *
     * @param pref the Preference object created to represent the app entry. The object already has
     *          its key, title, icon, and extra with the package name added.
     */
    protected void onAppPrefCreated(Preference pref, ApplicationsState.AppEntry appEntry) {
        // do nothing by default
    }

    /**
     * @return comparator to use to sort the apps list.
     */
    protected Comparator<ApplicationsState.AppEntry> getAppEntryComparator() {
        return ApplicationsState.ALPHA_COMPARATOR;
    }
}
