package com.google.android.clockwork.settings.wifi;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.wifi.AccessPoint;

import android.content.Context;
import android.content.res.Resources;
import android.net.wifi.WifiConfiguration;

import java.util.concurrent.TimeUnit;

public final class WifiHelper {

    private static final String WEP_PASSWORD_PATTERN = "[0-9A-Fa-f]*";
    private static final String WPA_PASSWORD_PATTERN = "[0-9A-Fa-f]{64}";
    private static final int MINUTES_PER_HOUR = (int) TimeUnit.HOURS.toMinutes(1);

    /**
     * Generate a WifiConfiguration for a secured AP (WEP or PSK). Implementation is similar to
     * google3/java/com/google/android/apps/chromecast/app/discovery/WifiConnectionManager
     * #updateWifiConfigurationSecurity().
     */
    public static WifiConfiguration generateConfigForSecuredAp(AccessPoint ap, String key) {
        return generateConfigForSecuredAp(ap.ssid, ap.security, key);
    }

    public static WifiConfiguration generateConfigForSecuredAp(String ssid, int security,
            String key) {
        WifiConfiguration conf = new WifiConfiguration();
        conf.hiddenSSID = true;
        conf.SSID = "\"" + ssid + "\"";
        conf.priority = 1;
        conf.status = WifiConfiguration.Status.ENABLED;
        int keyLength = (key == null) ? 0 : key.length();
        if (security == AccessPoint.SECURITY_WEP) {
            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            conf.wepKeys = new String[4];
            if (keyLength > 0) {
                if (((keyLength == 10) || (keyLength == 26) || (keyLength == 58))
                    && key.matches(WEP_PASSWORD_PATTERN)) {
                    conf.wepKeys[0] = key;
                } else {
                    conf.wepKeys[0] = "\"" + key + "\"";
                }
                conf.wepTxKeyIndex = 0;
            }
        } else if (security == AccessPoint.SECURITY_PSK) {
            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            if (keyLength > 0) {
                if (key.matches(WPA_PASSWORD_PATTERN)) {
                    conf.preSharedKey = key;
                } else {
                    conf.preSharedKey = "\"" + key + "\"";
                }
            }
        } else {
            throw new UnsupportedOperationException();
        }

        return conf;
    }

    public static WifiConfiguration generateOpenNetworkConfig(String ssid) {
        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = WifiHelper.convertToQuotedString(ssid);
        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

        return conf;
    }

    public static String convertToQuotedString(String string) {
        return "\"" + string + "\"";
    }

    public static String getDisplayTextForPowerSave(Context context, int minutes) {
        Resources res = context.getResources();
        if (minutes == Integer.MAX_VALUE) {
            return context.getString(R.string.wifi_power_save_never);
        } else if (minutes < MINUTES_PER_HOUR) {
            // Displayed in minutes.
            return res.getQuantityString(R.plurals.wifi_power_save_minutes, minutes, minutes);
        } else {
            // Displayed in hours.
            int hours = (int) TimeUnit.MINUTES.toHours(minutes);
            return res.getQuantityString(R.plurals.wifi_power_save_hours, hours, hours);
        }
    }

    private WifiHelper() {}
}
