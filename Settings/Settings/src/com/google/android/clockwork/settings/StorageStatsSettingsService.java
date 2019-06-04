package com.google.android.clockwork.settings;

import android.annotation.Nullable;
import android.app.Service;
import android.app.usage.StorageStats;
import android.app.usage.StorageStatsManager;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.StatFs;
import android.os.UserHandle;
import android.util.Log;

import com.google.android.clockwork.common.concurrent.Executors;
import com.google.android.clockwork.common.concurrent.WrappedCwRunnable;
import com.google.android.clockwork.storage.AppStorageInfo;
import com.google.android.clockwork.storage.Constants;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class StorageStatsSettingsService extends Service {

    private static final String TAG = "StorageStatsService";

    @Nullable private Messenger mReplyTo;

    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MSG_REFRESH_STORAGE_STATS: {
                    if (msg.replyTo != null) {
                        mReplyTo = msg.replyTo;
                    }

                    refreshStats();
                    break;
                }
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private final Messenger mMessenger = new Messenger(new IncomingHandler());

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    private void refreshStats() {
        Executors.INSTANCE.get(this)
                .getBackgroundExecutor()
                .submit(new WrappedCwRunnable("StorageStatsUpdate", this::calcStats));
    }

    private void calcStats() {
        ArrayList<AppStorageInfo> results = new ArrayList<>();

        // get total and available storage data
        StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
        long blockSize = statFs.getBlockSizeLong();
        long totalStorageSize = statFs.getBlockCountLong() * blockSize;
        long availableStorageSize = statFs.getAvailableBlocksLong() * blockSize;
        results.add(new AppStorageInfo(Constants.STORAGE_TOTAL_KEY,
                Constants.STORAGE_TOTAL_KEY, totalStorageSize, /* appSize= */ 0,
                /* dataSize= */ 0));
        results.add(new AppStorageInfo(Constants.STORAGE_AVAILABLE_KEY,
                Constants.STORAGE_AVAILABLE_KEY, availableStorageSize, /* appSize= */ 0,
                /* dataSize= */ 0));

        // get list of applications
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        // get storage data for each application
        StorageStatsManager storageStatsManager = getSystemService(StorageStatsManager.class);
        UserHandle userHandle = UserHandle.of(UserHandle.myUserId());
        for (ApplicationInfo packageInfo : packages) {
            try {
                StorageStats stats = storageStatsManager.queryStatsForPackage(
                        packageInfo.storageUuid, packageInfo.packageName, userHandle);
                long appSize = stats.getAppBytes();
                long dataSize = stats.getDataBytes();
                long totalSize = appSize + dataSize + stats.getCacheBytes();
                results.add(new AppStorageInfo(
                        pm.getApplicationLabel(packageInfo).toString(),
                        packageInfo.packageName, totalSize, appSize, dataSize));
            } catch (NameNotFoundException | IOException e) {
                Log.w(TAG, "Failed to query stats: " + e);
            }
        }

        // send reply
        if (mReplyTo == null) {
            Log.w(TAG, "No replyTo");
            return;
        }
        sendReply(results);
    }

    private void sendReply(ArrayList<AppStorageInfo> appStorageInfos) {
        Message reply = Message.obtain(
                null, Constants.MSG_REFRESH_STORAGE_STATS_RESULTS, 0, 0);
        Bundle returnData = new Bundle();
        returnData.putParcelableArrayList(Constants.EXTRA_STORAGE_ENTRIES, appStorageInfos);
        reply.setData(returnData);
        try {
            mReplyTo.send(reply);
        } catch (RemoteException e) {
            Log.w(TAG, "reply could not be sent", e);
        }
    }
}
