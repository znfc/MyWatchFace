package com.google.android.clockwork.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.google.android.clockwork.settings.cellular.SimStateNotification;

/**
 * Receiver for locale changes, which can be used to re-post notifications when the language of
 * the device has changed.
 */
public class LocaleChangedReceiver extends BroadcastReceiver {
    private static final String TAG = "settings";

    @Override
    public void onReceive(Context context, Intent intent) {
        SimStateNotification.maybeCreateNotification(context.getApplicationContext());
    }
}
