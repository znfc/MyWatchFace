package com.google.android.clockwork.settings.connectivity.wifi;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.UserManager;
import android.support.wearable.preference.AcceptDenyDialogPreference;
import android.support.wearable.view.AcceptDenyDialog;
import android.util.Log;
import android.view.View;

import com.android.settingslib.RestrictedLockUtils;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.common.SettingsPreferenceLogConstants;

/**
 * A custom implementation of Preference for a saved Wi-Fi access point.
 */
public class SavedNetworkPreference extends AcceptDenyDialogPreference {
    private static final String TAG = "SavedNetwork";

    private WifiConfiguration mWifiConfig;

    private ForgetNetworkListener mListener;

    /**
     * Callback triggered when a saved access point is successfully forgotten. Used to notify
     * WifiSettingsFragment to update "Current Network" and "Saved Networks".
     */
    public interface ForgetNetworkListener {
        /**
         * Called when a saved access point is successfully forgotten
         */
        public void onNetworkForgotten();
    }

    public SavedNetworkPreference(Context context,
                                  WifiConfiguration wifiConfig,
                                  ForgetNetworkListener listener) {
        super(context);
        mWifiConfig = wifiConfig;
        setTitle(mWifiConfig.SSID);
        setIcon(getContext().getDrawable(R.drawable.ic_cc_settings_clear));
        setKey(SettingsPreferenceLogConstants.IGNORE_SUBSTRING + mWifiConfig.SSID);
        mListener = listener;
    }

    protected WifiConfiguration getWifiConfig() {
        return mWifiConfig;
    }

    private WifiManager.ActionListener mForgetListener = new WifiManager.ActionListener() {
        @Override
        public void onSuccess() {
            mListener.onNetworkForgotten();
        }

        @Override
        public void onFailure(int reason) {
            Log.e(TAG, "Failed to forget network. Reason: " + reason);
        }
    };

    @Override
    protected void onPrepareDialog(AcceptDenyDialog dialog) {
        dialog.setMessage(
            getContext().getString(R.string.wifi_forget_network_title, mWifiConfig.SSID));
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);
        if (which == DialogInterface.BUTTON_POSITIVE) {
            if (!WifiSettingsUtil.canModifyNetwork(getContext(), mWifiConfig)
                || UserManager.get(getContext())
                    .hasUserRestriction(UserManager.DISALLOW_CONFIG_WIFI)) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getContext(),
                    RestrictedLockUtils.getDeviceOwner(getContext()));
            } else {
                WifiManager wifiManager =
                    (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);
                wifiManager.forget(mWifiConfig.networkId, mForgetListener);
            }
        }
    }
}