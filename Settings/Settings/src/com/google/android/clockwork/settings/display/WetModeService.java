package com.google.android.clockwork.settings.display;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.annotation.VisibleForTesting;
import android.support.wearable.view.WearableDialogHelper;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.ImageView;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.common.content.CwPrefs;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service that runs while wet mode is enabled.  Ensures that wet mode exits properly and displays
 * the wet mode UI overlay.
 */
public class WetModeService extends Service {
    static final String TAG = "WetMode";

    private static final String ACTION_END_WET_MODE =
            "com.google.android.clockwork.actions.END_WET_MODE";
    private static final String ACTION_WET_MODE_ENDED =
        "com.google.android.clockwork.actions.WET_MODE_ENDED";
    private static final String ACTION_WET_MODE_STARTED =
        "com.google.android.clockwork.actions.WET_MODE_STARTED";

    private static final String PERMISSION_END_WET_MODE =
            "com.google.android.clockwork.settings.END_WET_MODE";
    private static final String PERMISSION_TOUCH =
            "com.google.android.clockwork.settings.WATCH_TOUCH";
    private static final int ONGOING_NOTIFICATION_ID = 0x1028;

    private static final String WETMODE_PREFERENCES =
            "com.google.android.clockwork.settings.display.wetmode";

    private static final String KEY_SHOW_DIALOG = "cw.wetmode.show_dialog";

    /**
     * The screen timeout in seconds while wetmode is in effect, so that we do not fallback to the
     * default 30s timeout.
     */
    private static final long SCREEN_OFF_TIMEOUT = 10L;

    @VisibleForTesting
    WindowManager mWindowManager;
    @VisibleForTesting
    PowerManager mPowerManager;
    @VisibleForTesting
    ImageView mOverlayView;
    @VisibleForTesting
    AlertDialog mDialog;
    @VisibleForTesting
    SharedPreferences mPrefs;

    private boolean mWetModeEnabled = false;
    private AtomicInteger mExpectedScreenOns = new AtomicInteger(0);

    private ScreenReceiver mScreenReceiver;
    private EndReceiver mEndReceiver;
    private TiltCallback mTiltCallback;

    @Override
    public IBinder onBind(Intent intent) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Returning tilt binder");
        }
        if (mTiltCallback == null) {
            mTiltCallback = new TiltCallback();
        }
        return mTiltCallback;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, String.format("Starting service, mWetModeEnabled: %b", mWetModeEnabled));
        }
        if (!mWetModeEnabled) {
            startForeground();
            setupWetMode();
            return START_STICKY;
        } else {
            return START_NOT_STICKY;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Creating service");
        }
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mOverlayView = new OverlayView(this);
        mPrefs = CwPrefs.wrap(this, WETMODE_PREFERENCES);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Destroying service");
        }
        stopWetMode();
    }

    private void startForeground() {

        Notification notification = new Notification.Builder(this)
                .setLocalOnly(true)
                .setSmallIcon(getApplicationInfo().icon)
                .setTicker("WetModeService")
                .setWhen(System.currentTimeMillis())
                .setContentTitle("WetModeService")
                .setContentText("WetModeService")
                .setPriority(Notification.PRIORITY_LOW)
                .build();

        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }

    @VisibleForTesting
    void setupWetMode() {
        if (mPrefs.getBoolean(KEY_SHOW_DIALOG, true)) {
            if(mDialog == null) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Showing Dialog");
                }
                WearableDialogHelper.DialogBuilder dialogBuilder =
                        new WearableDialogHelper.DialogBuilder(
                                this);
                dialogBuilder.setTitle(R.string.wet_mode_dialog_title);
                dialogBuilder.setMessage(R.string.wet_mode_dialog_message);
                dialogBuilder.setCancelable(false);
                dialogBuilder.setPositiveIcon(R.drawable.action_accept);
                dialogBuilder.setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                mDialog = null;
                                startWetMode();
                            }
                        });
                dialogBuilder.setNegativeIcon(R.drawable.action_block);
                dialogBuilder.setNegativeButton(R.string.wet_mode_dialog_never_show_dialog,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if (Log.isLoggable(TAG, Log.DEBUG)) {
                                    Log.d(TAG, "Disabling Dialog");
                                }
                                mDialog = null;
                                mPrefs.edit().putBoolean(KEY_SHOW_DIALOG, false).apply();
                                startWetMode();
                            }
                        });
                dialogBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {

                        if (!mWetModeEnabled) {

                            // If we dismiss without enabling wet mode, send broadcast that wet mode
                            // has ended so Home app doesnt get into weird state.  Home considers
                            // wet mode started as soon as the enable broadcast is sent, which has
                            // already happened at this point, so notify that it ended.
                            if (Log.isLoggable(TAG, Log.DEBUG)) {
                                Log.d(TAG, "Dismissing Dialog without starting wet mode");
                            }
                            sendBroadcast(new Intent(ACTION_WET_MODE_ENDED), PERMISSION_TOUCH);
                            stopSelf();
                        }
                        mDialog = null;
                    }
                });
                mDialog = dialogBuilder.create();
                mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);
                mDialog.show();
            } else {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Dialog already displayed");
                }
            }
        } else {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Dialog not necessary");
            }
            startWetMode();
        }
    }

    private void startWetMode() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Starting wet mode");
        }

        // Register for the screen turning back on
        mExpectedScreenOns.set(0);
        if (mScreenReceiver == null) {
            mScreenReceiver = new ScreenReceiver();
        }
        IntentFilter screenFilter = new IntentFilter();
        screenFilter.addAction(Intent.ACTION_SCREEN_ON);
        screenFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenReceiver, screenFilter);

        if (mEndReceiver == null) {
            mEndReceiver = new EndReceiver();
        }
        IntentFilter endFilter = new IntentFilter();
        endFilter.addAction(ACTION_END_WET_MODE);
        registerReceiver(mEndReceiver, endFilter, PERMISSION_END_WET_MODE, null);

        // Add overlay view
        mWindowManager.addView(mOverlayView, getWindowParams());

        // Send device into ambient mode
        mPowerManager.goToSleep(SystemClock.uptimeMillis());
        mWetModeEnabled = true;

        sendBroadcast(new Intent(ACTION_WET_MODE_STARTED), PERMISSION_TOUCH);
    }

    private void stopWetMode() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Ending wet mode: " + mWetModeEnabled);
        }

        if (mWetModeEnabled) {
            // Remove overlay view
            if (mOverlayView.isAttachedToWindow()) {
                mWindowManager.removeView(mOverlayView);
            }

            // Unregister receivers
            if (mScreenReceiver != null) {
                unregisterReceiver(mScreenReceiver);
                mScreenReceiver = null;
            }

            if (mEndReceiver != null) {
                unregisterReceiver(mEndReceiver);
                mEndReceiver = null;
            }

            mWetModeEnabled = false;

            // Send broadcast that wetmode has ended
            sendBroadcast(new Intent(ACTION_WET_MODE_ENDED), PERMISSION_TOUCH);

            // End service
            stopSelf();
        }

    }

    @VisibleForTesting
    WindowManager.LayoutParams getWindowParams() {
        // We create a window of type {@link TYPE_SYSTEM_ALERT} to prevent the window from being
        // placed in the background. (see b/17539797)
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                (int) getResources().getDimension(R.dimen.wet_mode_indicator_size),
                (int) getResources().getDimension(R.dimen.wet_mode_indicator_size),
                WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP;
        params.y = (int) getResources().getDimension(R.dimen.wet_mode_indicator_margin);
        params.setUserActivityTimeout(SCREEN_OFF_TIMEOUT);
        return params;
    }

    @VisibleForTesting
    WindowManager getWindowManager() {
        return mWindowManager;
    }

    @VisibleForTesting
    class OverlayView extends ImageView {
        OverlayView(Context c) {
            super(c);
            Drawable icon = WetModeService.this.getDrawable(
                    R.drawable.ic_settings_wet_mode_indicator_ambient);
            setImageDrawable(icon);
        }
    }

    @VisibleForTesting
    class TiltCallback extends IWetModeTilt.Stub {
        @Override
        public void incomingTiltToWake() throws RemoteException {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Tilt to wake incoming");
            }
            mExpectedScreenOns.getAndIncrement();
        }

        @Override
        public boolean isWetModeEnabled() {
            return mWetModeEnabled;
        }
    }

    class ScreenReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                if (mExpectedScreenOns.decrementAndGet() >= 0) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Screen on expected; ignoring");
                    }
                } else {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Unexpect screen on; exiting wet mode");
                    }

                    stopWetMode();
                }
                mOverlayView.setImageDrawable(
                        getDrawable(R.drawable.ic_settings_wet_mode_indicator));
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Screen turning off");
                }
                mOverlayView.setImageDrawable(
                        getDrawable(R.drawable.ic_settings_wet_mode_indicator_ambient));
            }
        }
    }

    class EndReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Ending by command");
            }
            if (intent.getAction().equals(ACTION_END_WET_MODE)) {
                stopWetMode();
            }
        }
    }
}
