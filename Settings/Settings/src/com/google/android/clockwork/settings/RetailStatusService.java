package com.google.android.clockwork.settings;

import android.app.IntentService;
import android.content.Intent;

/**
 * Dummy service for external clients to check if the device is currently in retail mode.
 */
public class RetailStatusService extends IntentService {
    public RetailStatusService() {
        super(RetailStatusService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Do nothing.
    }
}
