package com.google.android.clockwork.settings;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import com.google.android.apps.wearable.adboverbluetooth.AdbOverBluetooth;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.DefaultSettingsContentResolver;
import com.google.android.clockwork.settings.SettingsContract;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Iterator;

/**
 * Control ADB for wireless debugging.
 */
public class AdbUtil {
    private static final String TAG = "settings";

    public static final String WIRELESS_DEBUG_CONFIG_PATH = "wireless_debug_config";
    public static final String KEY_WIRELESS_DEBUG_MODE = "wireless_debug_mode";
    public static final String KEY_WIFI_DEBUG_PORT = "wireless_debug_wifi_port";
    public static final int ADB_DEFAULT_PORT = 5555;
    private static final int ADB_PORT_OFF = 0;

    public static final int WIRELESS_DEBUG_OFF = 0;
    public static final int WIRELESS_DEBUG_BLUETOOTH = 1 << 0;
    public static final int WIRELESS_DEBUG_WIFI = 1 << 1;

    private static final Object sModeLock = new Object();
    private static AdbUtilNetworkCallback sNetworkCallback;

    /**
     * Uninstantiatable.
     */
    private AdbUtil() {}

    /**
     * If {@link #WIRELESS_DEBUG_BLUETOOTH} is not enabled, enable it, otherwise disable wireless
     * debugging.
     */
    public static void toggleBluetoothDebugging(Context context, boolean enabled) {
        boolean alreadyEnabled = getWirelessDebugSetting(context) == WIRELESS_DEBUG_BLUETOOTH;

        if (alreadyEnabled == enabled) {
            return;
        }

        int newMode = enabled ? WIRELESS_DEBUG_BLUETOOTH : WIRELESS_DEBUG_OFF;
        setWirelessDebugSetting(context, newMode);
        activateMode(context, getAdbEnabledSetting(context), newMode);
    }

    /**
     * If {@link #WIRELESS_DEBUG_WIFI} is not enabled, enable it, otherwise disable wireless
     * debugging.
     */
    public static void toggleWifiDebugging(Context context, boolean enabled) {
        boolean alreadyEnabled = getWirelessDebugSetting(context) == WIRELESS_DEBUG_WIFI;

        if (alreadyEnabled == enabled) {
            return;
        }

        int newMode = enabled ? WIRELESS_DEBUG_WIFI : WIRELESS_DEBUG_OFF;
        setWirelessDebugSetting(context, newMode);
        activateMode(context, getAdbEnabledSetting(context), newMode);
    }

    /**
     * Sets or disables the adb TCP port and starts or stops the debugging-over-bluetooth service as
     * appropriate based on the wireless debugging setting and whether ADB is enabled.
     */
    static void onBoot(Context context) {
        boolean adbEnabled = getAdbEnabledSetting(context);
        int mode = getWirelessDebugSetting(context);
        if (!adbEnabled || mode == WIRELESS_DEBUG_OFF) {
            // The default state is off, so no need to force the issue
            return;
        }

        activateMode(context, adbEnabled, mode);
    }

    /**
     * Sets or disables the adb TCP port and starts or stops the debugging-over-bluetooth service as
     * appropriate based on supplied mode and adb flag.
     */
    private static void activateMode(Context context, boolean adbEnabled, int mode) {
        if (!adbEnabled) {
            mode = WIRELESS_DEBUG_OFF;
        }

        synchronized (sModeLock) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, String.format("Wireless debugging switching to mode %s (%d)",
                        modeToString(mode), mode));
            }

            if (mode == WIRELESS_DEBUG_BLUETOOTH) {
                setAdbTcpListen(AdbOverBluetooth.ADBD_TCP_PORT);
            } else if (mode == WIRELESS_DEBUG_WIFI) {
                setAdbTcpListen(getWifiDebugPort(context));
            } else {
                if (mode != WIRELESS_DEBUG_OFF) {
                    Log.e(TAG, String.format(
                            "Unknown wireless debugging mode (%d). Defaulting to off.", mode));
                }
                setAdbTcpListen(ADB_PORT_OFF);
            }
            setAdbBluetoothTargetServiceRunning(context, mode == WIRELESS_DEBUG_BLUETOOTH);
            setWifiHoldUp(context, mode == WIRELESS_DEBUG_WIFI);
        }
    }

    /**
     * Returns a '\n' separated list of active IP addresses.
     */
    public static String getWirelessDebuggingAddresses(Context context) {
        String port = Integer.toString(getWifiDebugPort(context));

        LinkProperties prop = getWifiLinkProperties(context);
        if (prop == null) {
            // We aren't properly connected (yet).
            return context.getString(R.string.wifi_status_unavailable);
        }

        int numValidAddresses = 0;
        Iterator<InetAddress> iter = prop.getAllAddresses().iterator();
        StringBuilder ipAddressBuilder = new StringBuilder();
        while (iter.hasNext()) {
            InetAddress address = iter.next();
            if (address.isLoopbackAddress()) {
                continue;
            }

            if (address instanceof Inet4Address) {
                ipAddressBuilder.append(address.getHostAddress());
            } else {
                ipAddressBuilder.append("[").append(address.getHostAddress()).append("]");
            }
            ipAddressBuilder.append(":").append(port);

            if (iter.hasNext()) {
                ipAddressBuilder.append("\n");
            }

            numValidAddresses++;
        }

        if (numValidAddresses == 0) {
            // WiFi is still connecting.
            return context.getString(R.string.network_state_connecting);
        }

        return ipAddressBuilder.toString();
    }

    /**
     * Sets the global ADB setting, configures and restarts adbd, and starts/stops
     * the debugging-over-bluetooth service appropriately.
     */
    public static void setAdbDebugging(Context context, boolean adbEnabled) {
        if (getAdbEnabledSetting(context) == adbEnabled) {
            // Nothing changed.
            return;
        }
        setAdbEnabledSetting(context, adbEnabled);

        activateMode(context, adbEnabled, getWirelessDebugSetting(context));
    }

    /**
     * This just flips the settings bit. It does not touch the adbd service.
     */
    private static void setAdbEnabledSetting(Context context, boolean enabled) {
        Settings.Global.putInt(context.getContentResolver(),
                Settings.Global.ADB_ENABLED, enabled ? 1 : 0);
    }

    /**
     * Returns true if ADB is enabled.
     */
    static boolean getAdbEnabledSetting(Context context) {
        return Settings.Global.getInt(
                context.getContentResolver(), Settings.Global.ADB_ENABLED, 0) == 1;
    }

    private static Uri getSettingsPath() {
        return new Uri.Builder().scheme("content").authority(SettingsContract.SETTINGS_AUTHORITY)
                .path(WIRELESS_DEBUG_CONFIG_PATH).build();
    }

    /**
     * Returns the curent wireless debug setting, one of {@link WIRELESS_DEBUG_OFF},
     * {@link WIRELESS_DEBUG_BLUETOOTH}, or {@link WIRELESS_DEBUG_WIFI}
     */
    public static int getWirelessDebugSetting(Context context) {
        return new DefaultSettingsContentResolver(context.getContentResolver()).getIntValueForKey(
                getSettingsPath(), KEY_WIRELESS_DEBUG_MODE, WIRELESS_DEBUG_OFF);
    }

    /**
     * Sets the wireless debug setting, but does not enable or disable any system services.
     * @param mode one of {@link WIRELESS_DEBUG_OFF},{@link WIRELESS_DEBUG_BLUETOOTH},
     *   or {@link WIRELESS_DEBUG_WIFI}
     */
    static void setWirelessDebugSetting(Context context, int mode) {
        new DefaultSettingsContentResolver(context.getContentResolver()).putIntValueForKey(
                getSettingsPath(), KEY_WIRELESS_DEBUG_MODE, mode);
    }

    /**
     * Returns the curent wifi debug port setting. Does not reflect the current adb listening port.
     */
    static int getWifiDebugPort(Context context) {
        return new DefaultSettingsContentResolver(context.getContentResolver()).getIntValueForKey(
                getSettingsPath(), KEY_WIFI_DEBUG_PORT, ADB_DEFAULT_PORT);
    }

    /**
     * Sets the wifi debug port setting. Does not change which port adb is listening on.
     */
    static void setWifiDebugPort(Context context, int port) {
        new DefaultSettingsContentResolver(context.getContentResolver()).putIntValueForKey(
                getSettingsPath(), KEY_WIFI_DEBUG_PORT, port);
    }

    /**
     * Sets ADB to listen on the specified TCP port, or disables ADB TCP listening.
     * @param port the port to listen to, or 0 for disabled.
     */
    static void setAdbTcpListen(int port) {
        SystemProperties.set("service.adb.tcp.port", Integer.toString(port));
        SystemProperties.set("ctl.restart", "adbd");
    }

    /**
     * Enables or disables holding up WiFi.
     * Caller must hold sModeLock.
     */
    private static void setWifiHoldUp(Context context, boolean holdUp) {
        boolean isHeldUp = (sNetworkCallback != null);
        if (holdUp == isHeldUp) {
            return;
        }

        ConnectivityManager cm = context.getSystemService(ConnectivityManager.class);
        if (holdUp) {
            WifiManager wm = context.getSystemService(WifiManager.class);
            wm.setWifiEnabled(true);
            sNetworkCallback = new AdbUtilNetworkCallback();
            cm.requestNetwork(buildWifiNetworkRequest(), sNetworkCallback);
        } else {
            cm.unregisterNetworkCallback(sNetworkCallback);
            sNetworkCallback = null;
        }
    }

    /**
     * Starts or stops the debugging-over-bluetooth services.
     */
    static void setAdbBluetoothTargetServiceRunning(Context context, boolean runService) {
        Intent intent = new Intent(context, AdbBluetoothTargetService.class);
        if (runService) {
            context.startService(intent);
        } else {
            context.stopService(intent);
        }
    }

    /**
     * Build a {@link NetworkRequest} for the WiFi network.
     */
    public static NetworkRequest buildWifiNetworkRequest() {
        return new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();
    }

    /**
     * Get the link properties of the WiFi connection in order to get the list of IP addresses for
     * the link.
     */
    private static LinkProperties getWifiLinkProperties(Context context) {
        AdbUtilNetworkCallback currentCallback = sNetworkCallback;
        if (currentCallback == null) {
            return null;
        }

        if (currentCallback.linkProperties == null && currentCallback.network != null) {
            ConnectivityManager cm = context.getSystemService(ConnectivityManager.class);
            currentCallback.linkProperties = cm.getLinkProperties(currentCallback.network);
        }

        return currentCallback.linkProperties;
    }

    /**
     * Class to keep track of WiFi status
     */
    private static class AdbUtilNetworkCallback extends NetworkCallback {
        Network network;
        LinkProperties linkProperties;

        @Override
        public void onAvailable (Network network) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "WiFi debugging got WiFi network");
            }
            this.network = network;
        }

        @Override
        public void onLost (Network network) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "WiFi debugging lost WiFi network");
            }
            this.network = network;
        }

        @Override
        public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "WiFi debugging link properties changed");
            }
            this.network = network;
            this.linkProperties = linkProperties;
        }
    }

    /**
     * For pretty printing the various modes.
     */
    private static String modeToString(int mode) {
        switch (mode) {
            case WIRELESS_DEBUG_OFF:
                return "WIRELESS_DEBUG_OFF";
            case WIRELESS_DEBUG_BLUETOOTH:
                return "WIRELESS_DEBUG_BLUETOOTH";
            case WIRELESS_DEBUG_WIFI:
                return "WIRELESS_DEBUG_WIFI";
            default:
                return "WIRELESS_DEBUG_UNKNOWN";
        }
    }
}
