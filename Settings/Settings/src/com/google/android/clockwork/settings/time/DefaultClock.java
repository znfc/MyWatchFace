package com.google.android.clockwork.settings.time;

import android.os.SystemClock;

public class DefaultClock implements Clock {
    public static final DefaultClock INSTANCE = new DefaultClock();

    private DefaultClock() {}

    @Override
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    @Override
    public boolean setCurrentTimeMillis(long millis) {
        return SystemClock.setCurrentTimeMillis(millis);
    }

    @Override
    public long elapsedRealtime() {
        return SystemClock.elapsedRealtime();
    }
}
