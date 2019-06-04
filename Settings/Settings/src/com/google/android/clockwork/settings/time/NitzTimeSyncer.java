package com.google.android.clockwork.settings.time;

import static com.google.android.clockwork.settings.time.NitzTimeUpdateService.NITZ_TIME_OFF;
import static com.google.android.clockwork.settings.time.NitzTimeUpdateService.NITZ_TIME_ON;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.internal.util.IndentingPrintWriter;

/**
 * Manages the lifecycle of {@link NitzTimeUpdateService} and listens for NITZ time updates.
 */
public class NitzTimeSyncer {
    private static final String TAG = "NitzTimeSyncer";

    private final boolean mCellularCapable;
    private Context mContext;
    private Intent mNitzTimeUpdaterIntent;

    NitzTimeSyncer(Context context, boolean isCellularCapable) {
        mContext = context;
        mNitzTimeUpdaterIntent = new Intent(mContext, NitzTimeUpdateService.class);
        mCellularCapable = isCellularCapable;
    }

    /** Initialize the service in the phone process to handle NITZ updates from the carrier */
    public void startNitzService() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Starting NitzTimeSyncer");
        }
        if (mCellularCapable) {
            mNitzTimeUpdaterIntent.putExtra(
                    NitzTimeUpdateService.EXTRA_SYNC_TIME, NITZ_TIME_ON);
            mContext.startService(mNitzTimeUpdaterIntent);
        }
    }

    /** Stop the NITZ service */
    public void stopNitzService() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Stopping NitzTimeSyncer");
        }
        if (mCellularCapable) {
            // Call startService() instead of stopService() because NitzTimeUpdateService will
            // stop itself in onStartCommand() if it is no longer needed. It may still need to
            // be running to sync NITZ time zone.
            mNitzTimeUpdaterIntent.putExtra(
                    NitzTimeUpdateService.EXTRA_SYNC_TIME, NITZ_TIME_OFF);
            mContext.startService(mNitzTimeUpdaterIntent);
        }
    }

    void dump(IndentingPrintWriter pw) {
        pw.println("NitzTimeSyncer");
        pw.increaseIndent();
        pw.print("mCellularCapable="); pw.println(mCellularCapable);
        pw.decreaseIndent();
        pw.println();
    }
}
