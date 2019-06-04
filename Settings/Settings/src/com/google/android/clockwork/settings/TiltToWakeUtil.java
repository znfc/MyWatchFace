package com.google.android.clockwork.settings;

import android.content.Context;
import android.content.Intent;

public class TiltToWakeUtil {
    public static void syncTiltToWakeEnabled(Context context, boolean enabled) {
        context.sendBroadcast(
            new Intent(SettingsIntents.ACTION_SYNC_TILT_TO_WAKE_ENABLED)
                .putExtra(SettingsIntents.EXTRA_TILT_TO_WAKE_ENABLED, enabled));
    }
}
