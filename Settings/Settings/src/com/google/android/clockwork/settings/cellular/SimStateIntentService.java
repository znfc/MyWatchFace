package com.google.android.clockwork.settings.cellular;

import android.app.IntentService;
import android.content.Intent;

/**
 * Receives dismiss Unlock Sim Notication from stream card and dismiss it.
 */
public class SimStateIntentService extends IntentService {
    public static final String DISMISS_NOTIFICATION = "dismiss_notification";

    public SimStateIntentService() {
        super("SimStateIntentService");
    }

    @Override
    public void onHandleIntent(Intent intent) {
        switch (intent.getAction()) {
            case DISMISS_NOTIFICATION:
                SimStateNotification.cancelNotification(this);
                break;
        }
    }
}