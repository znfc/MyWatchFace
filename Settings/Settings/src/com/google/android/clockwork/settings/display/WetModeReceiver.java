/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.google.android.clockwork.settings.display;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.input.InputManager;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.view.InputDevice;

import com.google.android.clockwork.battery.wear.PowerIntents;
import com.google.android.clockwork.common.emulator.EmulatorUtil;
import com.google.android.gsf.GservicesValue;

/**
 * Starts wet mode if it is enabled.
 */
public class WetModeReceiver extends BroadcastReceiver {
    @VisibleForTesting
    static final String ACTION_ENABLE_WET_MODE =
            "com.google.android.wearable.action.ENABLE_WET_MODE";

    private static final String WEAR_PACKAGE_NAME = "com.google.android.wearable.app";

    private static final String GSERVICES_VALUE_WET_MODE_HOME_VERSION = "cw:wet_mode_home_version";

    // This is actually the version for Eagle DF 2, but it doesn't matter since DF2 will not ship
    private static final long HOME_EAGLE_VERSION = 780571168;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Log.isLoggable(WetModeService.TAG, Log.DEBUG)) {
            Log.d(WetModeService.TAG, "[WetModeReceiver] received intent: " + intent.getAction());
        }
        switch (intent.getAction()) {
            case ACTION_ENABLE_WET_MODE:
                if (isWetModeEnabled(context)) {
                    startWetMode(context);
                }
                break;
            default:
                if (Log.isLoggable(WetModeService.TAG, Log.DEBUG)) {
                    Log.d(WetModeService.TAG, String.format(
                                    "[WetModeReceiver] Ignoring unknown action '%s'.", intent.getAction()));
                }
        }
    }

    static boolean isWetModeEnabled(Context c) {
        try {
            if (EmulatorUtil.inEmulator()) {
                // Touch lock not supported yet due to b/72399634.
                return false;
            }
            PackageInfo info = c.getPackageManager().getPackageInfo(WEAR_PACKAGE_NAME, 0);
            GservicesValue<Long> wetModeHomeVersion =
                    GservicesValue.value(GSERVICES_VALUE_WET_MODE_HOME_VERSION, HOME_EAGLE_VERSION);
            Long homeVersionRequired = wetModeHomeVersion.get();
            if (Log.isLoggable(WetModeService.TAG, Log.DEBUG)) {
                Log.d(WetModeService.TAG,
                        "[WetModeReceiver] Checking Wet Mode availability, home version is "
                                + info.getLongVersionCode() + ", "
                                + homeVersionRequired + " required");
            }
            return info.getLongVersionCode() > homeVersionRequired;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(WetModeService.TAG, "[WetModeReceiver] Couldn't find Home App version");
            return false;
        }
    }

    static void startWetMode(Context context) {
        context.startService(new Intent(context, WetModeService.class));
    }
}
