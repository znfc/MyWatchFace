package com.google.android.clockwork.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;
import android.text.TextUtils;

/**
 * This receiver captures when home is ready and notifies the system via a system
 * property of the change.  Home also tells settings whether it is in retail mode.
 */
public class HomeReadyReceiver extends BroadcastReceiver {

    private static final String SYS_PROPERTY_CW_HOME_READY = "sys.cw_home_ready";
    private static final String SYS_PROPERTY_CW_RETAIL_MODE = "sys.cw_retail_mode";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null
                && TextUtils.equals(intent.getAction(),
                        SettingsIntents.ACTION_SET_HOME_READY)) {
            SystemProperties.set(SYS_PROPERTY_CW_HOME_READY, "1");
            boolean retail = intent.getBooleanExtra(SettingsIntents.EXTRA_RETAIL_MODE, false);
            SystemProperties.set(SYS_PROPERTY_CW_RETAIL_MODE,
                    retail ? "1" : "0");
        }
    }
}
