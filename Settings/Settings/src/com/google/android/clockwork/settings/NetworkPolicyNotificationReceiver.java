package com.google.android.clockwork.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Tranmpolines network policy notifcation intents to our intent service.
 */
public class NetworkPolicyNotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        intent.setClass(context, NetworkPolicyNotificationIntentService.class);
        context.startService(intent);
    }
}
