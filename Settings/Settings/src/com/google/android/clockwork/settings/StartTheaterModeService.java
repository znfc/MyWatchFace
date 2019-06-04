/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.TextUtils;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.common.content.CwPrefs;

public class StartTheaterModeService extends Service {
    public static final String ACTION_EXIT = "com.google.android.clockwork.settings.hide";

    private static final String THEATER_MODE_PREFERENCES
        = "com.google.android.clockwork.settings.theatermode";

    private static final String KEY_TOAST_COUNT = "cw.theatermode.toast_count";

    private static final int MAX_TOASTS = 3;

    private static final long EXIT_DELAY_MS = 1000;

    private PowerManager mPowerManager;

    private ScreenOffReceiver mScreenOffReceiver;

    private SettingsToastController mToastController;

    private PendingIntent mExitIntent;
    private boolean mToastShown;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mPowerManager.wakeUp(SystemClock.uptimeMillis(), "startTheaterMode");

        // Listen to screen changes
        mScreenOffReceiver = new ScreenOffReceiver();
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(ACTION_EXIT);
        getApplicationContext().registerReceiver(mScreenOffReceiver, filter);

        Intent intent = new Intent(ACTION_EXIT);
        intent.setPackage(getPackageName());
        mExitIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        maybeShowToastAndIncrementCount();
    }

    @Override
    public void onDestroy() {

        if (mToastController != null) {
            mToastController.remove();
        }

        getApplicationContext().unregisterReceiver(mScreenOffReceiver);

        AlarmManager alarmMgr =
                (AlarmManager) getApplicationContext().getSystemService(
                        Context.ALARM_SERVICE);

        alarmMgr.cancel(mExitIntent);

        super.onDestroy();
    }

    private void maybeShowToastAndIncrementCount() {
        SharedPreferences prefs = CwPrefs.wrap(this, THEATER_MODE_PREFERENCES);
        int toastCount = prefs.getInt(KEY_TOAST_COUNT, 0);
        if (toastCount < MAX_TOASTS) {
            mToastController = new SettingsToastController(this,
                getString(R.string.theater_mode_toast_button));
            mToastController.show(() -> finish());
            prefs.edit().putInt(KEY_TOAST_COUNT, toastCount + 1).apply();
        } else {
            finish();
        }
    }

    private void scheduleExit() {
        AlarmManager alarmMgr = (AlarmManager) getApplicationContext().getSystemService(
                        Context.ALARM_SERVICE);
        alarmMgr.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + EXIT_DELAY_MS,
                mExitIntent);
    }

    private void finish() {
        mToastShown = true;
        mPowerManager.goToSleep(SystemClock.uptimeMillis());
    }

    /**
     * Receiver to capture when the screen state changes and exit timer has fired.
     */
    private class ScreenOffReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TextUtils.equals(ACTION_EXIT, intent.getAction())) {
                stopSelf();
            } else if (TextUtils.equals(Intent.ACTION_SCREEN_OFF, intent.getAction())) {
                scheduleExit();
            } else if (TextUtils.equals(Intent.ACTION_SCREEN_ON, intent.getAction())) {
                // This is the worst case scenario where we didn't remove ourselves when the
                // screen turned off. Remove on wakeup.
                if (mToastShown) {
                    stopSelf();
                }
            }
        }
    }
}
