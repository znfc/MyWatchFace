package com.google.android.clockwork.settings;

import android.app.IntentService;
import android.content.Intent;
import android.os.UserHandle;
import android.provider.Settings;

/** Intent service to serialize and offload clockwork APM requests */
public class AirplaneModeService extends IntentService {

    public AirplaneModeService() {
        super("AirplaneModeService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (SettingsIntents.ACTION_CHANGE_AIRPLANE_MODE.equals(intent.getAction())) {
            final boolean airplaneModeEnabled = intent.getBooleanExtra(
                    SettingsIntents.EXTRA_IS_AIRPLANE_MODE_ENABLED, false);
            final boolean writeSuccess = Settings.Global.putInt(
                    getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON,
                    airplaneModeEnabled ? 1 : 0);
            if (writeSuccess) {
                final Intent newIntent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED)
                        .putExtra("state", airplaneModeEnabled);
                sendBroadcastAsUser(newIntent, UserHandle.CURRENT_OR_SELF);
            }
        }
    }
}
