package com.google.android.clockwork.settings;

import com.google.android.clockwork.common.concurrent.AbstractCwRunnable;
import com.google.android.clockwork.common.concurrent.Executors;
import com.google.android.gsf.GoogleSettingsContract.Partner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

/**
 * Sets LGAAYL (Let Google Apps Access Your Location) to 1 and clear the
 * locationPackagePrefixBlacklist. See b/17754967 for context.
 */
public class SetLGAAYLReceiver extends BroadcastReceiver {

    // Defined in
    // frameworks/base/services/core/java/com/android/server/location/LocationBlacklist.java
    private static final String BLACKLIST_CONFIG_NAME = "locationPackagePrefixBlacklist";

    @Override
    public void onReceive(final Context context, Intent intent) {
        Executors.Supplier.getInstance().get().getUserExecutor()
                .submit(new AbstractCwRunnable("SetLGAAYL") {
                    @Override
                    public void run() {
                        Partner.putInt(context.getContentResolver(),
                                Partner.USE_LOCATION_FOR_SERVICES, 1);
                        Settings.Secure.putString(context.getContentResolver(),
                                BLACKLIST_CONFIG_NAME, "");
                    }
                });
    }
}
