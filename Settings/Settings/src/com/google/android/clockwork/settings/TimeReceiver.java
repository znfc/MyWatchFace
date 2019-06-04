package com.google.android.clockwork.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.android.clockwork.settings.time.TimeIntents;

public class TimeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        switch (action) {
            case Intent.ACTION_AIRPLANE_MODE_CHANGED:
                boolean state = intent.getBooleanExtra("state", false);
                if (!state) {
                    context.startService(TimeIntents.getSetNtpTimeIntent(context));
                }
                break;
        }
    }
}
