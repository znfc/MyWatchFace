package com.google.android.clockwork.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;

/**
 * Receives the power key.
 */
public class GlobalKeyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Receives the Settings key, launches the settings activity.
        KeyEvent key = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        if (KeyEvent.KEYCODE_SETTINGS == key.getKeyCode()
                && KeyEvent.ACTION_DOWN == key.getAction()) {
            PowerManager manager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (manager.isScreenOn()) {
                Intent newIntent = new Intent(context, MainSettingsActivity.class);
                newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(newIntent);
            }
            // If not screen on, don't do anything.
        }
    }
}
