package com.google.android.clockwork.settings;

import android.app.ActivityManager;
import android.app.IntentService;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

/** Configures the doze/ambient state. Kept around for legacy reasons only. */
public class ConfigureDozeService extends IntentService {
    private static final String TAG = "ConfDoze";

    private static DozeChangeListener mDozeChangeListener;

    public interface DozeChangeListener {
        public void onDozeChanged();
    }

    public static void setDozeChangeListener(DozeChangeListener listener) {
        mDozeChangeListener = listener;
    }

    public ConfigureDozeService() {
        super("ConfigureDozeService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final String action = intent.getAction();
        if (action.equals(SettingsIntents.ACTION_CONFIGURE_DOZE)) {
            setDozeDisabled(intent.getBooleanExtra(SettingsIntents.EXTRA_DOZE_DISABLED, false));
        }
    }

    private void setDozeDisabled(boolean dozeDisabled) {
        // Power testing needs to take place with doze disabled. We require
        // that tests that run in the test harness set the disable_ambient
        // property manually (via setprop) and make this function a no-op, so
        // that the OEM setup broadcast doesn't end up turning doze back on.
        if (!ActivityManager.isRunningInTestHarness()) {
            Log.i(TAG, "setting doze property to disabled=" + dozeDisabled);

            // Just let the ambient config update ambient and doze at the same time, since they go
            // hand-in-hand regardless.
            DefaultAmbientConfig.getInstance(this).setAmbientEnabled(!dozeDisabled);
            if (mDozeChangeListener != null) {
                mDozeChangeListener.onDozeChanged();
            }
        }
    }
}
