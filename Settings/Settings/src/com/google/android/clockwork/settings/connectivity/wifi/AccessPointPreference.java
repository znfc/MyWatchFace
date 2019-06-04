package com.google.android.clockwork.settings.connectivity.wifi;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.NetworkSelectionStatus;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import com.android.settingslib.wifi.AccessPoint;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.common.logging.CwEventLogger;
import com.google.common.logging.Cw.CwEvent;
import com.google.common.logging.Cw.CwSetWifiPasswordEvent;
import com.google.common.logging.Cw.CwSetWifiPasswordEvent.PasswordInputMechanism;

/**
 * A custom implementation of Preference for a Wi-Fi access point.
 *
 * Used by AvailableNetworksFragment.
 */
public class AccessPointPreference extends Preference implements AccessPoint.AccessPointListener {

    private static final String TAG = "AccessPointPreference";

    private static final int[] PUBLIC_AP_ICONS = {
            R.drawable.ic_cc_settings_wifi_0,
            R.drawable.ic_cc_settings_wifi_1,
            R.drawable.ic_cc_settings_wifi_2,
            R.drawable.ic_cc_settings_wifi_3,
            R.drawable.ic_cc_settings_wifi_4
    };

    private static final int[] SECURE_AP_ICONS = {
            R.drawable.ic_cc_settings_wifi_secure_0,
            R.drawable.ic_cc_settings_wifi_secure_1,
            R.drawable.ic_cc_settings_wifi_secure_2,
            R.drawable.ic_cc_settings_wifi_secure_3,
            R.drawable.ic_cc_settings_wifi_secure_4
    };

    private Context mContext;

    private AccessPoint mAccessPoint;

    private WifiPasswordSource mWifiPasswordSource;

    private CwEventLogger mCwEventLogger;

    public AccessPointPreference(
            Context context, AccessPoint accessPoint, CwEventLogger cwEventLogger) {
        super(context);
        mContext = context;
        mAccessPoint = accessPoint;
        mAccessPoint.setListener(this);
        mCwEventLogger = cwEventLogger;
        mWifiPasswordSource = WifiPasswordSource.NONE;
    }

    @Override
    public void onAccessPointChanged(AccessPoint accessPoint) {
        String curKey = getKey();
        String key = getAccessPointKey(accessPoint);
        if ((key != null && !key.equals(curKey)) || (curKey != null && !curKey.equals(key))) {
            setKey(key);
            // Notify WifiAddNetworkFragment that the key for this access point has changed so that
            // WifiAddNetworkFragment can update its cache
            Intent intent = new Intent(WifiAddNetworkFragment.ACTION_ACCESS_POINT_CHANGED);
            intent.putExtra(WifiAddNetworkFragment.EXTRA_ACCESS_POINT_KEY, curKey);
            intent.putExtra(WifiAddNetworkFragment.EXTRA_NEW_ACCESS_POINT_KEY, key);
            mContext.sendBroadcast(intent);
        }
    }

    @Override
    public void onLevelChanged(AccessPoint accessPoint) {
    }

    public AccessPoint getAccessPoint() {
        return mAccessPoint;
    }

    public void setWifiPasswordSource(WifiPasswordSource wifiPasswordSource) {
        mWifiPasswordSource = wifiPasswordSource;
    }

    @Override
    protected void onBindView(View view) {
        if (mAccessPoint != null) {
            setTitle(mAccessPoint.getSsid());
            setIcon(getIcon(mAccessPoint.getLevel(),
                    (mAccessPoint.getSecurity() != AccessPoint.SECURITY_NONE)));
            updateSummary();
        }
        super.onBindView(view);
    }

    /*package*/ static String getAccessPointKey(AccessPoint accessPoint) {
        String key = accessPoint.getBssid();
        if (TextUtils.isEmpty(key)) {
            key = accessPoint.getSsidStr().replace("\"", "");
        }
        return key;
    }

    private Drawable getIcon(int level, boolean secured) {
        int iconLevel = (level >= 0 && level <= 4) ? level : 0;
        int iconRes = secured
                ? SECURE_AP_ICONS[iconLevel]
                : PUBLIC_AP_ICONS[iconLevel];
        return getContext().getDrawable(iconRes);
    }

    private void updateSummary() {
        final DetailedState detailedState = mAccessPoint.getDetailedState();
        if (detailedState == DetailedState.IDLE) {
            return;
        }

        String summary = null;

        WifiConfiguration config = mAccessPoint.getConfig();
        if (mAccessPoint.isActive() && detailedState != null) {
            switch (detailedState) {
                case SCANNING:
                case CONNECTING:
                case AUTHENTICATING:
                    summary = mContext.getString(R.string.wifi_current_connection_connecting);
                    break;
                case OBTAINING_IPADDR:
                    summary = mContext.getString(R.string.wifi_current_connection_obtaining_ip);
                    break;
                case CONNECTED:
                    summary = mContext.getString(R.string.wifi_current_connection_connected);
                    maybeLogPasswordEntrySuccess();
                    break;
            }
        } else if (config != null) {
            if (config.hasNoInternetAccess()) {
                summary = mContext.getString(R.string.wifi_connection_no_internet);
            } else if (!config.getNetworkSelectionStatus().isNetworkEnabled()) {
                summary = mContext.getString(R.string.wifi_connection_failure);
                if (config.getNetworkSelectionStatus().getNetworkSelectionDisableReason()
                        == NetworkSelectionStatus.DISABLED_BY_WRONG_PASSWORD) {
                  maybeLogPasswordEntryFailure();
                }
            } else {
                summary = mContext.getString(R.string.wifi_remembered);
            }
        }
        setSummary(summary);
    }

    private void maybeLogPasswordEntrySuccess() {
        if (mWifiPasswordSource == WifiPasswordSource.WATCH) {
            logPasswordEvent(/* success = */ true, PasswordInputMechanism.WATCH_KEYBOARD);
        } else if (mWifiPasswordSource == WifiPasswordSource.PHONE) {
            logPasswordEvent(/* success = */ true, PasswordInputMechanism.PHONE_KEYBOARD);
        }
        mWifiPasswordSource = WifiPasswordSource.NONE;
    }

    private void maybeLogPasswordEntryFailure() {
        if (mWifiPasswordSource == WifiPasswordSource.WATCH) {
            logPasswordEvent(/* success = */ false, PasswordInputMechanism.WATCH_KEYBOARD);
        } else if (mWifiPasswordSource == WifiPasswordSource.PHONE) {
            logPasswordEvent(/* success = */ false, PasswordInputMechanism.PHONE_KEYBOARD);
        }
        mWifiPasswordSource = WifiPasswordSource.NONE;
    }

    private void logPasswordEvent(boolean success, PasswordInputMechanism inputMechanism) {
        if (mCwEventLogger == null) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Logger is unavailable, must be phone process");
            }
            return;
        }
        CwSetWifiPasswordEvent.Builder passwordEvent =
                CwSetWifiPasswordEvent.newBuilder()
                        .setSuccess(success)
                        .setPasswordInputMechanism(inputMechanism);
        mCwEventLogger.logEvent(CwEvent.newBuilder().setSetWifiPasswordEvent(passwordEvent));
    }

    public static enum WifiPasswordSource {
        NONE,
        WATCH,
        PHONE
    };
}
