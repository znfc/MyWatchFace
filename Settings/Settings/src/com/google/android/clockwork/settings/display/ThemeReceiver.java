package com.google.android.clockwork.settings.display;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.om.IOverlayManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.ArraySet;
import android.util.Log;

import java.util.Arrays;

import com.google.android.clockwork.common.content.CwPrefs;

/**
 * Handles requests for runtime theme changes.
 *
 * A new theme is set by firing the SET_THEME intent to this receiver with an extra
 * of "packageName" containing the name of the target package to be overlaid. Theme
 * switching occurs by disabling the current theme and then enabling the new theme.
 */
public class ThemeReceiver extends BroadcastReceiver {
    static final String TAG = "ThemeReceiver";

    private static final String ACTION_SET_THEME =
            "com.google.android.clockwork.settings.SET_THEME";
    private static final String EXTRA_PACKAGE_NAME = "packageName";

    private static final String THEME_PREFERENCES =
            "com.google.android.clockwork.settings.display.theme";
    private static final String KEY_OVERLAY_PREFIX = "cw.theme.overlay_";

    // This whitelist is intentionally limited to just the android target for now;
    // we expect to add clockwork-specific packages later.
    private static final ArraySet<String> ALLOWED_PACKAGE_NAMES =
            new ArraySet<>(Arrays.asList("android"));

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_SET_THEME.equals(intent.getAction())) {
            return;
        }

        String packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
        if (packageName == null) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "No packageName extra specified for SET_THEME intent");
            }
            return;
        } else if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "SET_THEME intent received for overlay package: " + packageName);
        }

        IOverlayManager overlayService = IOverlayManager.Stub.asInterface(
                ServiceManager.getService(Context.OVERLAY_SERVICE));
        PackageManager packageManager = context.getPackageManager();
        PackageInfo themeInfo;

        try {
            themeInfo = packageManager.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Overlay package not found: " + packageName);
            }
            return;
        }

        if (!ALLOWED_PACKAGE_NAMES.contains(themeInfo.overlayTarget)) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Not an allowed overlay target: " + themeInfo.overlayTarget);
            }
            return;
        }

        String preferenceKey = KEY_OVERLAY_PREFIX + themeInfo.overlayTarget;

        try {
            SharedPreferences preferences = CwPrefs.wrap(context, THEME_PREFERENCES);
            String currentTheme = preferences.getString(preferenceKey, null);
            if (packageName.equals(currentTheme)) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "New overlay package is the same as the previous one. Ignoring.");
                }
                return;
            }
            if (currentTheme != null) {
                    boolean themeDisabled =
                            overlayService.setEnabled(currentTheme, false, UserHandle.USER_CURRENT);
                    if (themeDisabled) {
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "Disabling current overlay: " + currentTheme);
                        }
                        preferences.edit().putString(preferenceKey, null).apply();
                    } else if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Failed to disable current overlay: " + currentTheme);
                    }
            }
            boolean themeSet =
                    overlayService.setEnabled(packageName, true, UserHandle.USER_CURRENT);
            if (themeSet) {
                preferences.edit().putString(preferenceKey, packageName).apply();
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Set overlay to " + packageName
                            + " for target package " + themeInfo.overlayTarget);
                }
            } else if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Failed to set overlay to " + packageName
                        + " for target package " + themeInfo.overlayTarget);
            }
        } catch (RemoteException e) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Caught RemoteException when attempting overlay for " + packageName);
            }
        }
    }

}
