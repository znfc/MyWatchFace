package com.google.android.clockwork.settings.cellular;

import static android.net.NetworkPolicyManager.POLICY_REJECT_METERED_BACKGROUND;
import static android.net.TrafficStats.UID_REMOVED;
import static android.net.TrafficStats.UID_TETHERING;

import android.app.ActivityManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Loader;
import android.content.pm.UserInfo;
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import android.net.NetworkPolicyManager;
import android.net.NetworkStats;
import android.net.NetworkTemplate;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.wearable.preference.WearablePreferenceActivity;
import android.support.wearable.view.WearableListView;
import android.text.format.Formatter;
import android.util.SparseArray;
import android.util.Log;

import com.android.settingslib.AppItem;
import com.android.settingslib.net.SummaryForAllUidLoader;
import com.android.settingslib.net.UidDetail;
import com.android.settingslib.net.UidDetailProvider;

import com.google.android.apps.wearable.settings.R;
import com.google.android.collect.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppUsageActivity extends WearablePreferenceActivity {
    private static final String TAG = AppUsageActivity.class.getSimpleName();

    public static final String EXTRA_NETWORK_TEMPLATE =
            "com.google.android.clockwork.settings.cellular.NETWORK_TEMPLATE";
    public static final String EXTRA_CYCLE_START =
            "com.google.android.clockwork.settings.cellular.CYCLE_START";
    public static final String EXTRA_CYCLE_END =
            "com.google.android.clockwork.settings.cellular.CYCLE_END";

    private static final int OTHER_USER_RANGE_START = -2000;

    private static final int LOADER_SUMMARY = 1;

    private long mCycleStart;
    private long mCycleEnd;
    private ArrayList<AppItem> mItems = Lists.newArrayList();
    private PreferenceScreen mPreferenceScreen;
    private INetworkStatsService mStatsService;
    private INetworkStatsSession mStatsSession;
    private NetworkPolicyManager mPolicyManager;
    private NetworkTemplate mTemplate;
    private UserManager mUserManager;
    private UidDetailProvider mUidDetailProvider;

    private final LoaderCallbacks<NetworkStats> mSummaryCallbacks = new LoaderCallbacks<
            NetworkStats>() {
        @Override
        public Loader<NetworkStats> onCreateLoader(int id, Bundle args) {
            return new SummaryForAllUidLoader(AppUsageActivity.this, mStatsSession, args);
        }

        @Override
        public void onLoadFinished(Loader<NetworkStats> loader, NetworkStats data) {
            final int[] restrictedUids = mPolicyManager.getUidsWithPolicy(
                    POLICY_REJECT_METERED_BACKGROUND);
            bindStats(data, restrictedUids);
        }

        @Override
        public void onLoaderReset(Loader<NetworkStats> loader) {
            bindStats(null, new int[0]);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTemplate = (NetworkTemplate) getIntent().getParcelableExtra(EXTRA_NETWORK_TEMPLATE);
        mCycleStart = getIntent().getLongExtra(EXTRA_CYCLE_START, -1);
        mCycleEnd = getIntent().getLongExtra(EXTRA_CYCLE_END, -1);

        if (mTemplate == null || mCycleStart < 0 || mCycleEnd < 0) {
            throw new IllegalArgumentException("mTemplate=" + mTemplate + ", mCycleStart="
                    + mCycleStart + ", mCycleEnd=" + mCycleEnd);
        }

        mStatsService = INetworkStatsService.Stub.asInterface(
                            ServiceManager.getService(Context.NETWORK_STATS_SERVICE));
        mPolicyManager = NetworkPolicyManager.from(this);
        mUserManager = (UserManager) getSystemService(Context.USER_SERVICE);

        try {
            mStatsSession = mStatsService.openSession();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        mUidDetailProvider = new UidDetailProvider(this);

        startPreferenceFragment(new PreferenceFragment() {
            @Override
            public void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                addPreferencesFromResource(R.xml.prefs_app_usage);
                mPreferenceScreen = getPreferenceScreen();
            }
        }, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(LOADER_SUMMARY,
                SummaryForAllUidLoader.buildArgs(mTemplate, mCycleStart, mCycleEnd),
                mSummaryCallbacks);
    }

    @Override
    public void onDestroy() {
        TrafficStats.closeQuietly(mStatsSession);
        super.onDestroy();
    }

    private void bindStats(NetworkStats stats, int[] restrictedUids) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "bindStats(), stats: " + stats);
        }

        final int currentUserId = ActivityManager.getCurrentUser();
        final List<UserHandle> profiles = mUserManager.getUserProfiles();
        final SparseArray<AppItem> knownItems = new SparseArray<AppItem>();

        NetworkStats.Entry entry = null;
        final int size = stats != null ? stats.size() : 0;
        for (int i = 0; i < size; i++) {
            entry = stats.getValues(i, entry);

            // Decide how to collapse items together
            final int uid = entry.uid;

            final int collapseKey;
            final int category;
            final int userId = UserHandle.getUserId(uid);
            if (UserHandle.isApp(uid)) {
                if (profiles.contains(new UserHandle(userId))) {
                    if (userId != currentUserId) {
                        // Add to a managed user item.
                        final int managedKey = UidDetailProvider.buildKeyForUser(userId);
                        accumulate(managedKey, knownItems, entry,
                                AppItem.CATEGORY_USER);
                    }
                    // Add to app item.
                    collapseKey = uid;
                    category = AppItem.CATEGORY_APP;
                } else {
                    // If it is a removed user add it to the removed users' key
                    final UserInfo info = mUserManager.getUserInfo(userId);
                    if (info == null) {
                        collapseKey = UID_REMOVED;
                        category = AppItem.CATEGORY_APP;
                    } else {
                        // Add to other user item.
                        collapseKey = UidDetailProvider.buildKeyForUser(userId);
                        category = AppItem.CATEGORY_USER;
                    }
                }
            } else if (uid == UID_REMOVED || uid == UID_TETHERING) {
                collapseKey = uid;
                category = AppItem.CATEGORY_APP;
            } else {
                collapseKey = android.os.Process.SYSTEM_UID;
                category = AppItem.CATEGORY_APP;
            }
            accumulate(collapseKey, knownItems, entry, category);
        }

        for (int i = 0; i < restrictedUids.length; ++i) {
            final int uid = restrictedUids[i];
            // Only splice in restricted state for current user or managed users
            if (!profiles.contains(new UserHandle(UserHandle.getUserId(uid)))) {
                continue;
            }

            AppItem item = knownItems.get(uid);
            if (item == null) {
                item = new AppItem(uid);
                item.total = -1;
                mItems.add(item);
                knownItems.put(item.key, item);
            }
            item.restricted = true;
        }

        Collections.sort(mItems);

        for (AppItem item : mItems) {
            // TODO: load uncached UidDetail asynchronously.
            UidDetail detail = mUidDetailProvider.getUidDetail(item.key, true /* blocking */);

            Preference p = new Preference(this);
            p.setTitle(detail.label);
            p.setSummary(Formatter.formatFileSize(this, item.total));
            p.setIcon(detail.icon);
            mPreferenceScreen.addPreference(p);
        }
    }

    /**
     * Accumulate data usage of a network stats entry for the item mapped by the collapse key.
     * Creates the item if needed.
     *
     * @param collapseKey the collapse key used to map the item.
     * @param knownItems collection of known (already existing) items.
     * @param entry the network stats entry to extract data usage from.
     * @param itemCategory the item is categorized on the list view by this category. Must be
     *            either AppItem.APP_ITEM_CATEGORY or AppItem.MANAGED_USER_ITEM_CATEGORY
     */
    private void accumulate(int collapseKey, final SparseArray<AppItem> knownItems,
            NetworkStats.Entry entry, int itemCategory) {
        final int uid = entry.uid;
        AppItem item = knownItems.get(collapseKey);
        if (item == null) {
            item = new AppItem(collapseKey);
            item.category = itemCategory;
            mItems.add(item);
            knownItems.put(item.key, item);
        }
        item.addUid(uid);
        item.total += entry.rxBytes + entry.txBytes;
    }
}
