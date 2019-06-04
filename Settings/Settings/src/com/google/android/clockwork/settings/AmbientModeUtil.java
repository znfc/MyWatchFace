package com.google.android.clockwork.settings;

import android.content.Context;
import android.content.Intent;


public final class AmbientModeUtil {
    public static void syncAmbientEnabled(Context context, boolean enabled) {
        context.sendBroadcast(
                new Intent(SettingsIntents.ACTION_SYNC_AMBIENT_DISABLED)
                .putExtra(SettingsIntents.EXTRA_AMBIENT_DISABLED, !enabled));
    }
}
