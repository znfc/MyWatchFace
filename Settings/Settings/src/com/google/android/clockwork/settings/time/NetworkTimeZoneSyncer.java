package com.google.android.clockwork.settings.time;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Syncs the system time zone from the cellular network. Listens for NITZ time zone updates from the
 * carrier, and updates the system time zone if it has changed.
 */
public class NetworkTimeZoneSyncer {
    private static final String TAG = "NetworkTimeZoneSyncer";

    private Context mContext;

    private Intent mNitzTimeZoneUpdaterIntent;

    NetworkTimeZoneSyncer(Context context) {
        mContext = context;
	    mNitzTimeZoneUpdaterIntent = new Intent(context, NitzTimeUpdateService.class);
    }

    public void startNitzService() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Starting NetworkTimeZoneSyncer");
        }
        mContext.startService(mNitzTimeZoneUpdaterIntent);
    }

    public void stopNitzService() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Stopping NetworkTimeZoneSyncer");
        }
        // Call startService() instead of stopService() because NitzTimeUpdateService will stop
        // itself in onStartCommand() if it is no longer needed. It may still need to be running
        // to sync NITZ time.
        mContext.startService(mNitzTimeZoneUpdaterIntent);
    }
}
