package com.google.android.clockwork.settings.cellular;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import android.net.NetworkPolicy;
import android.net.NetworkPolicyManager;
import android.net.NetworkStatsHistory;
import android.net.NetworkTemplate;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.INetworkManagementService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.telephony.TelephonyManager;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.text.format.Time;
import android.util.Log;
import android.util.Pair;

import com.android.settingslib.NetworkPolicyEditor;
import com.android.settingslib.net.ChartData;
import com.android.settingslib.net.ChartDataLoader;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;
import com.google.android.clockwork.settings.utils.FeatureManager;

import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.stream.IntStream;


/**
 * Settings fragment to expose cellular data usage
 */
public class DataUsageFragment extends SettingsPreferenceFragment {
    private static final String TAG = DataUsageActivity.class.getSimpleName();

    private static final String KEY_PREF_DATA_USAGE_LIMIT_ENABLE
            = "pref_dataUsageLimitEnable";
    private static final String KEY_PREF_DATA_USAGE_LIMIT_VALUE
            = "pref_dataUsageLimitValue";
    private static final String KEY_PREF_DATA_CONNECTIVITY_ENABLE
            = "pref_dataConnectivityEnable";
    private static final String KEY_PREF_DATA_DAY_RANGE = "pref_dataUsageCycleDay";
    private static final String KEY_PREF_DATA_USAGE_WARNING_LEVEL = "pref_dataUsageWarningLevel";
    private static final String KEY_PREF_DATA_USAGE_APP_USAGE = "pref_dataUsageAppUsage";

    private static final int ENABLE_DATA_LIMIT_REQUEST_CODE = 1;
    private static final int SET_DATA_LIMIT_VALUE_REQUEST_CODE = 2;
    private static final int CHOOSE_CYCLE_DAY_REQUEST_CODE = 3;
    private static final int SET_WARNING_LEVEL_REQUEST_CODE = 4;

    private static final int FIRST_DAY = 1;
    private static final int LAST_DAY = 31;

    private static final long KB_IN_BYTES = 1000;
    private static final long MB_IN_BYTES = KB_IN_BYTES * 1000;
    private static final long GB_IN_BYTES = MB_IN_BYTES * 1000;

    // The limit bytes should should be 1.2 * warning bytes.
    private static final float LIMIT_BYTES_MULTIPLIER = 1.2f;
    private static final long MIN_LIMIT_BYTES = 5 * GB_IN_BYTES;
    private static final long FOUR_WEEKS_IN_MILLIS = 4 * DateUtils.WEEK_IN_MILLIS;

    private static final int LOADER_CHART_DATA = 0;

    private INetworkStatsService mStatsService;
    private INetworkStatsSession mStatsSession;
    private ChartData mChartData;
    private DataUsageNetworkPolicy mPolicy;
    private DataLimitPreference mDataLimitValue;
    private TelephonyManager mTelephonyManager;
    private boolean mIsLocalEdition;

    private final LoaderCallbacks<ChartData> mChartDataCallbacks
            = new LoaderCallbacks<ChartData>() {
        @Override
        public Loader<ChartData> onCreateLoader(int id, Bundle args) {
            return new ChartDataLoader(getActivity(), mStatsSession, args);
        }

        @Override
        public void onLoadFinished(Loader<ChartData> loader, ChartData data) {
            mChartData = data;
            updateCycleSummary(findPreference(KEY_PREF_DATA_DAY_RANGE));
        }

        @Override
        public void onLoaderReset(Loader<ChartData> loader) {
            mChartData = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPolicy = new DataUsageNetworkPolicy(getContext());
        mTelephonyManager = (TelephonyManager) getContext().getSystemService(
                Context.TELEPHONY_SERVICE);

        mStatsService = INetworkStatsService.Stub.asInterface(
                ServiceManager.getService(Context.NETWORK_STATS_SERVICE));
        try {
            mStatsSession = mStatsService.openSession();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        mIsLocalEdition = FeatureManager.INSTANCE.get(getContext()).isLocalEditionDevice();

        addPreferencesFromResource(R.xml.prefs_data_usage);

        initDataLimitEnable((SwitchPreference) findPreference(KEY_PREF_DATA_USAGE_LIMIT_ENABLE));
        // This is added for LE launch. We should consider add it to RoW.
        initDataConnectivityEnable(
                (SwitchPreference) findPreference(KEY_PREF_DATA_CONNECTIVITY_ENABLE));
        initDataLimitValue(mDataLimitValue =
                (DataLimitPreference) findPreference(KEY_PREF_DATA_USAGE_LIMIT_VALUE));
        initDataUsageCycleDay((ListPreference) findPreference(KEY_PREF_DATA_DAY_RANGE));
        initDataUsageWarningLevel(
                (DataLimitPreference) findPreference(KEY_PREF_DATA_USAGE_WARNING_LEVEL));
        initDataUsageAppUsage(findPreference(KEY_PREF_DATA_USAGE_APP_USAGE));
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(LOADER_CHART_DATA,
                ChartDataLoader.buildArgs(mPolicy.networkTemplate(), null), mChartDataCallbacks);
    }

    @Override
    public void onDestroy() {
        TrafficStats.closeQuietly(mStatsSession);
        super.onDestroy();
    }

    private void initDataUsageCycleDay(final ListPreference pref) {
        pref.setTitle(formatDateRange(getContext(), mPolicy.cycleStart(), mPolicy.cycleEnd()));

        if (!mPolicy.isModifiable()) {
            pref.setEnabled(false);
        } else {
            updateCycleSummary(pref);
            String[] entries = IntStream.rangeClosed(FIRST_DAY, LAST_DAY)
                    .mapToObj(Integer::toString).toArray(String[]::new);
            pref.setEntries(entries);
            pref.setEntryValues(entries);
            pref.setValue(Integer.toString(mPolicy.cycleDay()));
            pref.setOnPreferenceChangeListener((p, newValue) -> {
                int cycleDay = Integer.valueOf((String) newValue);
                String cycleTimezone = new Time().timezone;
                mPolicy.updateCycleDay(cycleDay, cycleTimezone);
                updateCycleSummary(pref);
                return true;
            });
        }
    }

    private void initDataUsageWarningLevel(final DataLimitPreference pref) {
        pref.setValue(mPolicy.warningBytes());
        pref.setOnPreferenceChangeListener((p, newValue) -> {
            final long resultLimitBytes = Long.valueOf((String) newValue);
            if (resultLimitBytes > 0) {
                mPolicy.updateWarningBytes(resultLimitBytes);
            }
            return true;
        });
    }

    private void initDataUsageAppUsage(Preference pref) {
        pref.setOnPreferenceClickListener((p) -> {
            startActivity(new Intent(getContext(), AppUsageActivity.class)
                    .putExtra(AppUsageActivity.EXTRA_NETWORK_TEMPLATE, mPolicy.networkTemplate())
                    .putExtra(AppUsageActivity.EXTRA_CYCLE_START, mPolicy.cycleStart())
                    .putExtra(AppUsageActivity.EXTRA_CYCLE_END, mPolicy.cycleEnd()));
                return true;
        });
    }

    private void initDataLimitEnable(SwitchPreference pref) {
        pref.setChecked(mPolicy.limitEnabled());
        pref.setOnPreferenceChangeListener((p, newValue) -> {
            if ((Boolean) newValue) {
                long minLimitBytes = (long) (mPolicy.warningBytes() * LIMIT_BYTES_MULTIPLIER);
                long limitBytes = Math.max(MIN_LIMIT_BYTES, minLimitBytes);
                mDataLimitValue.setValue(limitBytes);
                // mDataLimitValue only notifies on change, so change directly too to be safe
                mPolicy.updateLimitBytes(limitBytes);
            } else {
                mPolicy.disableLimit();
            }
            return true;
        });
    }

    private void initDataConnectivityEnable(SwitchPreference pref) {
        if (mIsLocalEdition) {
            pref.setChecked(mTelephonyManager.getDataEnabled());
            pref.setOnPreferenceChangeListener((p, newValue) -> {
                mTelephonyManager.setDataEnabled((Boolean) newValue);
                return true;
            });
        } else {
            getPreferenceScreen().removePreference(pref);
        }
    }

    private void initDataLimitValue(DataLimitPreference pref) {
        pref.setValue(mPolicy.limitBytes());
        pref.setOnPreferenceChangeListener((p, newValue) -> {
            mPolicy.updateLimitBytes(Long.valueOf((String) newValue));
            return true;
        });
    }

    private void updateCycleSummary(Preference pref) {
        NetworkStatsHistory.Entry entry = null;
        if (mChartData != null) {
            final long now = System.currentTimeMillis();
            entry = mChartData.network.getValues(mPolicy.cycleStart(), mPolicy.cycleEnd(),
                    now, null);
        }
        final long totalBytes = entry != null ? entry.rxBytes + entry.txBytes : 0;

        pref.setTitle(formatDateRange(getContext(), mPolicy.cycleStart(), mPolicy.cycleEnd()));
        pref.setSummary(Formatter.formatFileSize(getContext(), totalBytes));
    }

    private static class DataUsageNetworkPolicy {
        private NetworkPolicyManager mPolicyManager;
        private NetworkPolicyEditor mPolicyEditor;
        private NetworkTemplate mTemplate;
        private long mCycleStart;
        private long mCycleEnd;

        public DataUsageNetworkPolicy(Context context) {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(
                    Context.TELEPHONY_SERVICE);
            mTemplate = NetworkTemplate.buildTemplateMobileAll(telephonyManager.getSubscriberId());
            mTemplate = NetworkTemplate.normalize(mTemplate,
                    telephonyManager.getMergedSubscriberIds());

            mPolicyManager = NetworkPolicyManager.from(context);
            mPolicyEditor = new NetworkPolicyEditor(mPolicyManager);
            mPolicyEditor.read();

            computeCycleRange();
        }

        /**
         * Check if we are allowed to modify the network policy
         */
        public boolean isModifiable() {
            final NetworkPolicy policy = mPolicyEditor.getPolicy(mTemplate);
            return policy != null && isBandwidthControlEnabled()
                   && ActivityManager.getCurrentUser() == UserHandle.USER_OWNER;
        }

        public boolean limitEnabled() {
            return mPolicyEditor.hasLimitedPolicy(mTemplate);
        }

        public long limitBytes() {
            return mPolicyEditor.getPolicyLimitBytes(mTemplate);
        }

        public long warningBytes() {
            return mPolicyEditor.getPolicyWarningBytes(mTemplate);
        }

        public long cycleStart() {
            return mCycleStart;
        }

        public long cycleEnd() {
            return mCycleEnd;
        }

        public int cycleDay() {
            return mPolicyEditor.getPolicyCycleDay(mTemplate);
        }

        public NetworkTemplate networkTemplate() {
            return mTemplate;
        }

        public void disableLimit() {
            mPolicyEditor.setPolicyLimitBytes(mTemplate, NetworkPolicy.LIMIT_DISABLED);
        }

        public void updateLimitBytes(long limitBytes) {
            if (limitBytes > 0) {
                mPolicyEditor.setPolicyLimitBytes(mTemplate, limitBytes);
            }
        }
        public void updateWarningBytes(long warningBytes) {
            if (warningBytes > 0) {
                mPolicyEditor.setPolicyWarningBytes(mTemplate, warningBytes);
            }
        }

        public void updateCycleDay(int cycleDay, String cycleTimezone) {
            mPolicyEditor.setPolicyCycleDay(mTemplate, cycleDay, cycleTimezone);
            computeCycleRange();
        }

        private boolean isBandwidthControlEnabled() {
            INetworkManagementService networkService = INetworkManagementService.Stub.asInterface(
                    ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE));
            try {
                return networkService.isBandwidthControlEnabled();
            } catch (RemoteException e) {
                Log.w(TAG, "problem talking with INetworkManagementService: " + e);
                return false;
            }
        }

        // Compute the data usage cycle date range for a NetworkPolicy. Example: "Jul9 - Aug 8"
        private void computeCycleRange() {
            final NetworkPolicy policy = mPolicyEditor.getPolicy(mTemplate);
            final long now = System.currentTimeMillis();
            mCycleStart = now;
            mCycleEnd = now + 1;
            if (policy != null) {
                // find the next cycle boundary
                final Pair<ZonedDateTime, ZonedDateTime> cycle = NetworkPolicyManager
                        .cycleIterator(policy).next();
                mCycleStart = cycle.first.toInstant().toEpochMilli();
                mCycleEnd = cycle.second.toInstant().toEpochMilli();
            } else {
                // no policy defined cycles; show entry for each four-week period
                mCycleStart = mCycleEnd - FOUR_WEEKS_IN_MILLIS;
            }
        }
    }

    private static String formatDateRange(Context context, long start, long end) {
        final StringBuilder sb = new StringBuilder(50);
        final java.util.Formatter formatter = new java.util.Formatter(sb, Locale.getDefault());
        final int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH;
        return DateUtils.formatDateRange(context, formatter, start, end, flags, null).toString();
    }
}
