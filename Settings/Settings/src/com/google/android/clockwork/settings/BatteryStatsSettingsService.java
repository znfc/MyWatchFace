package com.google.android.clockwork.settings;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.BatteryStats;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.android.internal.os.PowerProfile;
import com.android.internal.util.UserIcons;
import com.google.android.clockwork.battery.Constants;
import com.google.android.clockwork.battery.WearableBatteryStats;
import com.google.android.clockwork.battery.WearableHistoryItem;
import com.google.android.clockwork.battery.WearableUserInfo;
import com.google.android.clockwork.battery.wear.WearableBatterySipper;
import com.google.android.clockwork.common.concurrent.CwStrictMode;
import com.google.android.gms.wearable.DataMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.android.apps.wearable.settings.R;;

/**
 * This service provides access to privileged battery data.  Callers will receive
 * a reply containing general stats and additional messages providing the battery history
 * items.  The logic for processing the battery data is based on
 * {@link com.android.settings.fuelgauge.PowerUsageSummary}
 */
public class BatteryStatsSettingsService extends Service {
    private static final String TAG = "BatteryStatsService";

    // This flag bypasses all filters applied to the set of battery consumers returned
    // to the caller.  By default certain usage thresholds must be met before being
    // considered a true battery consumer.
    private static final boolean DEVELOPMENT = false;

    private static final int MIN_POWER_THRESHOLD_MILLI_AMP = 5;
    private static final int MIN_AVERAGE_POWER_THRESHOLD_MILLI_AMP = 10;
    private static final int SECONDS_IN_HOUR = 60 * 60;

    // Since we can only store 1MB of data in the return {@link Message}, we must
    // send over the battery history data in chunks.
    private static final int HISTORY_CHUNK_SIZE = 500;

    private BatteryStatsHelper mStatsHelper;
    private UserManager mUm;
    private Intent mBatteryIntent;

    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                mBatteryIntent = intent;
            }
        }
    };

    /** Target we publish for clients to send messages to IncomingHandler. */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MSG_REFRESH_BATTERY_STATS: {
                    StrictMode.ThreadPolicy policy = CwStrictMode.allowDiskReads();
                    try {
                        refreshBatteryStats(msg);
                    } finally {
                        CwStrictMode.restoreStrictMode(policy);
                    }
                    break;
                }
                default:
                    super.handleMessage(msg);
            }
        }

        private void refreshBatteryStats(Message msg) {
            mStatsHelper.clearStats();
            BatteryStats stats = mStatsHelper.getStats();
            PowerProfile profile = mStatsHelper.getPowerProfile();
            final List<UserHandle> profiles = mUm.getUserProfiles();
            mStatsHelper.refreshStats(BatteryStats.STATS_SINCE_CHARGED, profiles);

            ArrayList<WearableBatterySipper> sippers = extractSippers(stats, profile);
            ArrayList<WearableHistoryItem> historyItems = extractHistory(stats);
            DataMap configMap = retrieveInfo(stats);

            Message reply = Message.obtain(null,
                    Constants.MSG_REFRESH_BATTERY_STATS_RESULTS, 0, 0);
            Bundle returnData = new Bundle();

            final boolean hasHistory = historyItems.size() > 0;

            returnData.putParcelableArrayList(Constants.EXTRA_BATTERY_SIPPERS, sippers);
            returnData.putParcelable(Constants.EXTRA_STATS_INFO, configMap.toBundle());
            returnData.putBoolean(Constants.EXTRA_HAS_HISTORY, hasHistory);
            reply.setData(returnData);

            try {
                msg.replyTo.send(reply);
            } catch (RemoteException e) {
                Log.e(TAG, e.getMessage(), e);
            }

            sendHistoryItems(historyItems, msg.replyTo);
        }
    }

    /**
     * Breaks apart a list of {@link com.google.android.clockwork.battery.WearableHistoryItem}
     * into {@link android.os.Message} friendly chunks and sends them to the supplied
     * {@link android.os.Messenger}.
     */
    private void sendHistoryItems(ArrayList<WearableHistoryItem> historyItems, Messenger target) {
        final int totalSize = historyItems.size();

        for (int i = 0; i < totalSize; i+= HISTORY_CHUNK_SIZE) {
            Bundle returnData = new Bundle();
            final int endPos = Math.min(i + HISTORY_CHUNK_SIZE, totalSize);
            ArrayList<WearableHistoryItem> subset = new ArrayList<WearableHistoryItem>(
                    historyItems.subList(i, endPos));
            returnData.putParcelableArrayList(Constants.EXTRA_HISTORY_ITEMS, subset);
            returnData.putBoolean(Constants.EXTRA_LAST, endPos >= totalSize);

            Message historyReply = Message.obtain(null,
                    Constants.MSG_REFRESH_BATTERY_STATS_HISTORY, 0, 0);
            historyReply.setData(returnData);
            try {
                target.send(historyReply);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mUm = (UserManager) getSystemService(Context.USER_SERVICE);
        mStatsHelper = new BatteryStatsHelper(this, true);
        mStatsHelper.create((Bundle) null);
        mBatteryIntent = registerReceiver(mBatteryInfoReceiver,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mBatteryInfoReceiver);
        super.onDestroy();
    }

    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    /**
     * Extracts relevant information from the {@link android.os.BatteryStats}. The result
     * is stored in a {@link com.google.android.gms.wearable.DataMap} as it is meant
     * to be consumed ultimately by other nodes.
     */
    private DataMap retrieveInfo(BatteryStats stats) {
        DataMap dataMap = new DataMap();
        final int batteryLevel = getApplicationContext().getResources().getInteger(
                R.integer.config_lowBatteryWarningLevel);
        dataMap.putInt(Constants.KEY_BATTERY_WARN_LEVEL, batteryLevel);
        dataMap.putInt(Constants.KEY_BATTERY_CRITICAL_LEVEL,
                getApplicationContext().getResources().getInteger(
                        R.integer.config_criticalBatteryWarningLevel));
        final long elapsedRealtimeUs = SystemClock.elapsedRealtime() * 1000;
        dataMap.putLong(Constants.KEY_TIME_SINCE_CHARGED,
                stats.computeBatteryRealtime(elapsedRealtimeUs, BatteryStats.STATS_SINCE_CHARGED));
        final long timeRemaining = stats.computeBatteryTimeRemaining(elapsedRealtimeUs);
        dataMap.putLong(Constants.KEY_TIME_REMAINING, timeRemaining);
        dataMap.putBoolean(Constants.KEY_DISCHARGING, true);

        if (mBatteryIntent != null) {
            final int plugType = mBatteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
            dataMap.putInt(Constants.KEY_PLUGGED, plugType);
            dataMap.putInt(Constants.KEY_BATTERY_LEVEL, getBatteryLevel(mBatteryIntent));
            final int batteryStatus = mBatteryIntent.getIntExtra(
                    BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
            dataMap.putInt(Constants.KEY_BATTERY_STATUS, batteryStatus);

            final long chargeTime = stats.computeChargeTimeRemaining(elapsedRealtimeUs);

            if (chargeTime > 0 && batteryStatus != BatteryManager.BATTERY_STATUS_FULL) {
                dataMap.putLong(Constants.KEY_TIME_REMAINING, chargeTime);
                dataMap.putBoolean(Constants.KEY_DISCHARGING, false);
            }
        }

        return dataMap;
    }

    private static int getBatteryLevel(Intent batteryChangedIntent) {
        int level = batteryChangedIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int scale = batteryChangedIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        return (level * 100) / scale;
    }

    /**
     * Iterates through the history items from the supplied {@link android.os.BatteryStats}
     * and converts them into the equivalent
     * {@link com.google.android.clockwork.battery.WearableHistoryItem}.
     */
    private ArrayList<WearableHistoryItem> extractHistory(BatteryStats stats) {
        ArrayList<WearableHistoryItem> entries = new ArrayList<WearableHistoryItem>();

        if (stats.startIteratingHistoryLocked()) {
            final BatteryStats.HistoryItem rec = new BatteryStats.HistoryItem();
            while (stats.getNextHistoryLocked(rec)) {
                final int length = entries.size();
                final boolean replace = length >= 2
                        && rec.isDeltaData()
                        && entries.get(length - 1).batteryLevel == rec.batteryLevel
                        && entries.get(length - 2).batteryLevel == rec.batteryLevel;
                final WearableHistoryItem entry = WearableHistoryItemUtils.convertHistoryItem(rec);
                if (replace) {
                    entries.set(length - 1, entry);
                } else {
                    entries.add(entry);
                }
            }
            stats.finishIteratingHistoryLocked();
        }

        return entries;
    }

    /**
     * Iterates over the {@link com.android.internal.os.BatterySipper} captured in the supplied
     * {@link android.os.BatteryStats} and converts them into the equivalent
     * {@link com.google.android.clockwork.battery.wear.WearableBatterySipper}.
     */
    private ArrayList<WearableBatterySipper> extractSippers(BatteryStats stats,
            PowerProfile powerProfile) {
        ArrayList<WearableBatterySipper> wearableSippers = new ArrayList<WearableBatterySipper>();
        final double averagePower = powerProfile.getAveragePower(PowerProfile.POWER_SCREEN_FULL);

        if (DEVELOPMENT) {
            final List<BatterySipper> usageList = mStatsHelper.getUsageList();
            final int dischargeAmount = stats != null ? stats.getDischargeAmount(
                    BatteryStats.STATS_SINCE_CHARGED) : 0;

            final int numSippers = usageList.size();
            for (int i = 0; i < numSippers; i++) {
                final BatterySipper sipper = usageList.get(i);
                double percentOfTotal =
                        ((sipper.totalPowerMah / mStatsHelper.getTotalPower()) * dischargeAmount);

                sipper.percent = percentOfTotal;
                WearableBatteryStats wearableStats = createWearableBatteryStats(percentOfTotal,
                        percentOfTotal, sipper);
                WearableBatterySipper wearableSipper =
                        createWearableSipper(sipper, wearableStats);

                if (wearableSipper != null) {
                    wearableSippers.add(wearableSipper);
                }
            }
        } else if (averagePower >= MIN_AVERAGE_POWER_THRESHOLD_MILLI_AMP) {
            final List<BatterySipper> usageList = getCoalescedUsageList(
                    mStatsHelper.getUsageList());
            final int dischargeAmount = stats != null ? stats.getDischargeAmount(
                    BatteryStats.STATS_SINCE_CHARGED) : 0;
            final int numSippers = usageList.size();
            for (int i = 0; i < numSippers; i++) {
                final BatterySipper sipper = usageList.get(i);
                if ((sipper.totalPowerMah * SECONDS_IN_HOUR) < MIN_POWER_THRESHOLD_MILLI_AMP) {
                    continue;
                }
                double percentOfTotal =
                        ((sipper.totalPowerMah / mStatsHelper.getTotalPower()) * dischargeAmount);

                if (((int) (percentOfTotal + .5)) < 1) {
                    continue;
                }
                if (sipper.drainType == BatterySipper.DrainType.OVERCOUNTED) {
                    // Don't show over-counted unless it is at least 2/3 the size of
                    // the largest real entry, and its percent of total is more significant
                    if (sipper.totalPowerMah < ((mStatsHelper.getMaxRealPower()*2)/3)) {
                        continue;
                    }
                    if (percentOfTotal < 10) {
                        continue;
                    }
                }
                if (sipper.drainType == BatterySipper.DrainType.UNACCOUNTED) {
                    // Don't show over-counted unless it is at least 1/2 the size of
                    // the largest real entry, and its percent of total is more significant
                    if (sipper.totalPowerMah < (mStatsHelper.getMaxRealPower()/2)) {
                        continue;
                    }
                    if (percentOfTotal < 5) {
                        continue;
                    }
                }

                final double percentOfMax =
                        (sipper.totalPowerMah * 100) / mStatsHelper.getMaxPower();
                sipper.percent = percentOfTotal;

                final WearableBatteryStats wearableStats = createWearableBatteryStats(percentOfMax,
                        percentOfTotal, sipper);
                WearableBatterySipper wearableSipper =
                        createWearableSipper(sipper, wearableStats);
                if (wearableSipper != null) {
                    wearableSippers.add(wearableSipper);
                }
            }
        }

        return wearableSippers;
    }

    private static boolean isSharedGid(int uid) {
        return UserHandle.getAppIdFromSharedAppGid(uid) > 0;
    }

    private static boolean isSystemUid(int uid) {
        return uid >= android.os.Process.SYSTEM_UID
                && uid < android.os.Process.FIRST_APPLICATION_UID;
    }

    /**
     * We want to coalesce some UIDs. For example, dex2oat runs under a shared gid that
     * exists for all users of the same app. We detect this case and merge the power use
     * for dex2oat to the device OWNER's use of the app.
     * @return A sorted list of apps using power.
     */
    private static List<BatterySipper> getCoalescedUsageList(final List<BatterySipper> sippers) {
        final SparseArray<BatterySipper> uidList = new SparseArray<>();

        final ArrayList<BatterySipper> results = new ArrayList<>();
        final int numSippers = sippers.size();
        for (int i = 0; i < numSippers; i++) {
            BatterySipper sipper = sippers.get(i);
            if (sipper.getUid() > 0) {
                int realUid = sipper.getUid();

                // Check if this UID is a shared GID. If so, we combine it with the OWNER's
                // actual app UID.
                if (isSharedGid(sipper.getUid())) {
                    realUid = android.os.UserHandle.getUid(android.os.UserHandle.USER_OWNER,
                            android.os.UserHandle.getAppIdFromSharedAppGid(sipper.getUid()));
                }

                // Check if this UID is a system UID (mediaserver, logd, nfc, drm, etc).
                if (isSystemUid(realUid)
                        && !"mediaserver".equals(sipper.packageWithHighestDrain)) {
                    // Use the system UID for all UIDs running in their own sandbox that
                    // are not apps. We exclude mediaserver because we already are expected to
                    // report that as a separate item.
                    realUid = android.os.Process.SYSTEM_UID;
                }

                if (realUid != sipper.getUid()) {
                    // Replace the BatterySipper with a new one with the real UID set.
                    BatterySipper newSipper = new BatterySipper(sipper.drainType,
                            new FakeUid(realUid), 0.0);
                    newSipper.add(sipper);
                    newSipper.packageWithHighestDrain = sipper.packageWithHighestDrain;
                    newSipper.mPackages = sipper.mPackages;
                    sipper = newSipper;
                }

                int index = uidList.indexOfKey(realUid);
                if (index < 0) {
                    // New entry.
                    uidList.put(realUid, sipper);
                } else {
                    // Combine BatterySippers if we already have one with this UID.
                    final BatterySipper existingSipper = uidList.valueAt(index);
                    existingSipper.add(sipper);
                    if (existingSipper.packageWithHighestDrain == null
                            && sipper.packageWithHighestDrain != null) {
                        existingSipper.packageWithHighestDrain = sipper.packageWithHighestDrain;
                    }

                    final int existingPackageLen = existingSipper.mPackages != null ?
                            existingSipper.mPackages.length : 0;
                    final int newPackageLen = sipper.mPackages != null ?
                            sipper.mPackages.length : 0;
                    if (newPackageLen > 0) {
                        String[] newPackages = new String[existingPackageLen + newPackageLen];
                        if (existingPackageLen > 0) {
                            System.arraycopy(existingSipper.mPackages, 0, newPackages, 0,
                                    existingPackageLen);
                        }
                        System.arraycopy(sipper.mPackages, 0, newPackages, existingPackageLen,
                                newPackageLen);
                        existingSipper.mPackages = newPackages;
                    }
                }
            } else {
                results.add(sipper);
            }
        }

        final int numUidSippers = uidList.size();
        for (int i = 0; i < numUidSippers; i++) {
            results.add(uidList.valueAt(i));
        }

        // The sort order must have changed, so re-sort based on total power use.
        Collections.sort(results, new Comparator<BatterySipper>() {
            @Override
            public int compare(BatterySipper a, BatterySipper b) {
                return Double.compare(b.totalPowerMah, a.totalPowerMah);
            }
        });
        return results;
    }

    private WearableBatteryStats createWearableBatteryStats(double percentOfMax,
            double percentOfTotal, BatterySipper sipper) {
        return new WearableBatteryStats(
                percentOfMax,
                percentOfTotal,
                sipper.usageTimeMs,
                sipper.cpuTimeMs,
                sipper.gpsTimeMs,
                sipper.wifiRunningTimeMs,
                sipper.cpuFgTimeMs,
                sipper.wakeLockTimeMs,
                sipper.mobileRxPackets,
                sipper.mobileTxPackets,
                sipper.mobileActive,
                sipper.mobileActiveCount,
                sipper.wifiRxPackets,
                sipper.wifiTxPackets,
                sipper.mobileRxBytes,
                sipper.mobileTxBytes,
                sipper.wifiRxBytes,
                sipper.wifiTxBytes);
    }

    private WearableBatterySipper createWearableSipper(BatterySipper sipper,
            WearableBatteryStats stats) {
        Integer uid = sipper.uidObj != null ? sipper.uidObj.getUid() : null;

        WearableUserInfo wearableInfo = null;
        UserInfo info = mUm.getUserInfo(sipper.userId);
        if (info != null) {
            Drawable userIcon = getUserIcon(getApplicationContext(), mUm, info);
            wearableInfo = new WearableUserInfo(sipper.userId,
                    !TextUtils.isEmpty(info.name) ? info.name : Integer.toString(info.id),
                    UserIcons.convertToBitmap(userIcon));
        }

        return new WearableBatterySipper(stats, uid,
                BatteryConvertUtil.getCategory(sipper.drainType), wearableInfo,
                sipper.packageWithHighestDrain);
    }

    private static Drawable getUserIcon(Context context, UserManager um, UserInfo user) {
        if (user.iconPath != null) {
            Bitmap icon = um.getUserIcon(user.id);
            if (icon != null) {
                return CircleFramedDrawable.getInstance(context, icon);
            }
        }
        return UserIcons.getDefaultUserIcon(context.getResources(), user.id, /* light= */ false);
    }

}
