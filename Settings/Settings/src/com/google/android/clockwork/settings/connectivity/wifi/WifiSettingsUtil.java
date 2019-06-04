package com.google.android.clockwork.settings.connectivity.wifi;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiConfiguration;
import android.provider.Settings;
import android.text.TextUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.android.clockwork.settings.common.PackageManagerProxy;
import com.google.android.clockwork.settings.enterprise.DevicePolicyManagerWrapper;
import com.google.android.clockwork.settings.enterprise.DevicePolicyManagerWrapperImpl;

public class WifiSettingsUtil {

    /** valid values for this key: "on", "off", "off_airplane" */
    private static final String WIFI_SETTING_KEY = "clockwork_wifi_setting";
    private static final String ON = "on";
    private static final String OFF = "off";
    private static final String OFF_AIRPLANE = "off_airplane";
    private static final String IN_WIFI_SETTINGS_KEY = "clockwork_in_wifi_settings";

    private WifiSettingsUtil() {}

    /**
     * Like any other app, use a NetworkRequest to bring up WiFi and keep it up as long as
     * the user is in Wifi Settings.
     *
     * Returns the NetworkCallback used to register the NetworkRequest. Note that the caller of this
     * method must retain this callback and input it as a parameter to any subsequent calls to this
     * method.
     */
    /*package*/ static NetworkCallback setWifiHoldUp(Context context, NetworkCallback networkCallback,
                                      boolean holdUp) {
        boolean isHeldUp = (networkCallback != null);
        if (holdUp == isHeldUp) {
            return networkCallback;
        }

        ConnectivityManager cm = context.getSystemService(ConnectivityManager.class);
        if (holdUp) {
            NetworkRequest request = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build();

            networkCallback = new NetworkCallback();
            cm.requestNetwork(request, networkCallback);
        } else {
            cm.unregisterNetworkCallback(networkCallback);
            networkCallback = null;
        }
        return networkCallback;
    }

    /**
     * Signal to WifiMediator that the user is in Wifi Settings, so that certain rules
     * (i.e. Wifi Backoff) will not be applied.
     */
    /*package*/ static void setInWifiSettings(Context context, boolean inWifiSettings) {
        int forceWifiSetting = Settings.System.getInt(
                context.getContentResolver(), IN_WIFI_SETTINGS_KEY, 0);
        if (forceWifiSetting <= 1) {
            Settings.System.putInt(context.getContentResolver(),
                    IN_WIFI_SETTINGS_KEY,
                    inWifiSettings ? 1 : 0);
        }
    }

    /*package*/ static boolean getWearWifiEnabled(Context context) {
        String setting =
                Settings.System.getString(context.getContentResolver(), WIFI_SETTING_KEY);
        // if the Setting is unconfigured, default to On
        if (TextUtils.isEmpty(setting)) {
            setting = ON;
        }
        return ON.equals(setting);
    }

    /*package*/ static void setWearWifiEnabled(Context context, boolean enabled) {
        String setting = enabled
                ? ON
                : (isAirplaneModeOn(context) ? OFF_AIRPLANE : OFF);
        Settings.System.putString(context.getContentResolver(),
                WIFI_SETTING_KEY, setting);
    }

    /*package*/ static boolean isAirplaneModeOn(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    /**
     * Returns true if the given WifiConfiguration can be edited, and false if Wifi Configuration
     * lockdown is in effect (meaning the WifiConfiguration cannot be deleted).
     */
    /*package*/ static boolean canModifyNetwork(Context context, WifiConfiguration config) {
        return canModifyNetwork(
            new DevicePolicyManagerWrapperImpl(
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE)),
            new PackageManagerProxy(context.getPackageManager()),
            context.getContentResolver(),
            config);
    }

    @VisibleForTesting static boolean canModifyNetwork(DevicePolicyManagerWrapper dpm,
            PackageManagerProxy pm, ContentResolver contentResolver,
            WifiConfiguration config) {
        if (config == null) {
            return true;
        }

        // Check if device has DPM capability. If it has and dpm is still null, then we
        // treat this case with suspicion and bail out.
        if (pm.hasSystemFeature(PackageManager.FEATURE_DEVICE_ADMIN) && dpm == null) {
            return false;
        }

        boolean isConfigEligibleForLockdown = false;
        if (dpm != null) {
            final ComponentName deviceOwner = dpm.getDeviceOwnerComponentOnAnyUser();
            if (deviceOwner != null) {
                final int deviceOwnerUserId = dpm.getDeviceOwnerUserId();
                try {
                    final int deviceOwnerUid = pm.getPackageUidAsUser(deviceOwner.getPackageName(),
                        deviceOwnerUserId);
                    isConfigEligibleForLockdown = deviceOwnerUid == config.creatorUid;
                } catch (NameNotFoundException e) {
                    // don't care
                }
            }
        }
        if (!isConfigEligibleForLockdown) {
            return true;
        }

        final boolean isLockdownFeatureEnabled = Settings.Global.getInt(contentResolver,
            Settings.Global.WIFI_DEVICE_OWNER_CONFIGS_LOCKDOWN, 0) != 0;
        return !isLockdownFeatureEnabled;
    }
}
