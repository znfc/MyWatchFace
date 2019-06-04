/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.google.android.clockwork.settings;

import android.os.Process;
import android.os.SystemClock;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

/**
 * A Runnable which runs a {@link #loop()} repeatedly until it exits, waiting between iterations.
 * The wait is an exponential backoff.
 */
public abstract class BackoffLoopingRunnable implements Runnable {
    private final long mInitialTimeout;
    private final double mMultiplier;
    private final long mMaxTimeout;
    private final String mTag;
    private final String mName;

    private long mCurrentTimeout;
    private long mSleepExpire;
    private int mMode;

    private Timer mTimer = new Timer();

    private static final int INITIAL = -1;
    public static final int CONTINUE = 0;
    public static final int FINISH = 1;

    /**
     * @param tag The log tag
     * @param name The name of this Runnable
     * @param initialTimeout The amount of time to wait after the first iteration, in milliseconds
     * @param multiplier Multiplied with the current timeout to determine the next iteration's
     *                   timeout
     * @param maxTimeout The maximium amount of time to wait between iterations, in milliseconds
     */
    public BackoffLoopingRunnable(String tag, String name, long initialTimeout, double multiplier,
                                  long maxTimeout) {
        mInitialTimeout = initialTimeout;
        mMultiplier = multiplier;
        mMaxTimeout = maxTimeout;
        mTag = tag;
        mName = tag + "-" + name;

        mMode = INITIAL;
        mCurrentTimeout = mInitialTimeout;
    }

    public String getName() {
        return mName;
    }

    public void reset() {
        synchronized (mTimer) {
            if (Log.isLoggable(mTag, Log.VERBOSE)) {
                if (mMode != FINISH) {
                    Log.v(mTag, getName() + " resetting timeout to " + mInitialTimeout +
                          " and waking thread");
                } else {
                    Log.v(mTag, getName() + " reset called on finished state");
                }
            }

            mCurrentTimeout = mInitialTimeout;
            mSleepExpire = 0;
            mTimer.notifyAll();
        }
    }

    public boolean shouldRun() {
        return true;
    }

    public abstract int loop();

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        if (Log.isLoggable(mTag, Log.VERBOSE)) {
            Log.v(mTag, getName() + " running");
        }

        while (shouldRun() && mMode != FINISH) {
            if (mMode == CONTINUE) {
                synchronized (mTimer) {
                    mSleepExpire = mTimer.getTime() + mCurrentTimeout;
                }
                if (Log.isLoggable(mTag, Log.VERBOSE)) {
                    Log.v(mTag, getName() + " sleeping for " + mCurrentTimeout);
                }

                mCurrentTimeout = (long) Math.ceil(mCurrentTimeout * mMultiplier);
                if (mCurrentTimeout > mMaxTimeout) {
                    mCurrentTimeout = mMaxTimeout;
                }
            }
            while (shouldRun() && mTimer.getTime() < mSleepExpire) {
                synchronized (mTimer) {
                    long timeToSleep = mSleepExpire - mTimer.getTime();
                    if (timeToSleep <= 0) {
                        break;
                    }

                    try {
                        mTimer.waitFor(timeToSleep);
                    } catch (InterruptedException e) {}
                }
            }

            if (!shouldRun()) {
                break;
            }

            if (Log.isLoggable(mTag, Log.VERBOSE)) {
                Log.v(mTag, getName() + " executing loop");
            }
            mMode = loop();

            if (mMode != CONTINUE) {
                mMode = FINISH;
            }
        }

        if (Log.isLoggable(mTag, Log.VERBOSE)) {
            Log.v(mTag, getName() + " finishing");
        }
    }

    @VisibleForTesting
    void setTimer(Timer timer) {
        mTimer = timer;
    }

    @VisibleForTesting
    static class Timer {
        public long getTime() {
            return SystemClock.elapsedRealtime();
        }

        public void waitFor(long time) throws InterruptedException {
            this.wait(time);
        }
    }
}
