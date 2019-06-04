package com.google.android.clockwork.settings.time;

import android.net.Network;
import android.util.Log;

import java.util.concurrent.Callable;

public class RefreshTimeCallable implements Callable<Boolean> {
    private static final String TAG = NetworkTimeSyncer.TAG;

    private final Clock mClock;
    private final TrustedTimeProxy mTime;
    private final Network mNetwork;
    private final long mFreshnessThreshold;
    // If the time difference is greater than this threshold, then update the time.
    private final int mDriftThreshold;

    RefreshTimeCallable(
            Clock clock,
            TrustedTimeProxy ntpTrustedTime,
            Network network,
            long freshnessThreshold,
            int drift) {
        mClock = clock;
        mTime = ntpTrustedTime;
        mNetwork = network;
        mFreshnessThreshold = freshnessThreshold;
        mDriftThreshold = drift;
    }

    public Boolean call() {
        if (mTime.getCacheAge() < mFreshnessThreshold || mTime.forceRefresh(mNetwork)) {
            final long systemTime = mClock.currentTimeMillis();
            final long ntpTime = mTime.currentTimeMillis();

            long drift = Math.abs(systemTime - ntpTime);

            if (drift > mDriftThreshold) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Ntp time to be set = " + ntpTime);
                }
                mClock.setCurrentTimeMillis(ntpTime);
            } else if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Ntp time is close enough = " + ntpTime);
            }
            return true;
        } else {
            return false;
        }
    }
}
