package com.google.android.clockwork.settings;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.UpdateLock;
import android.util.Log;

public class UpdateLockHolderService extends Service {
    private static final String TAG = "UpdateLockHolder";

    private UpdateLock mLock;

    @Override
    public void onCreate() {
        mLock = new UpdateLock(TAG);
        mLock.setReferenceCounted(false);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String action = intent.getAction();
        if (action.equals(SettingsIntents.ACTION_LOCK_UPDATES)) {
            Log.w(TAG, "acquiring update lock");
            mLock.acquire();
        } else if (action.equals(SettingsIntents.ACTION_UNLOCK_UPDATES)) {
            Log.i(TAG, "releasing update lock");
            mLock.release();
            stopSelf();
        }
        return START_REDELIVER_INTENT;
    }
}
