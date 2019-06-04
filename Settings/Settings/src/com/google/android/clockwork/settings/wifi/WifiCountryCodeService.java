package com.google.android.clockwork.settings.wifi;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.wifi.WifiManager;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;

import com.google.android.clockwork.settings.SettingsIntents;

import com.google.common.base.Preconditions;

/**
 * An IntentService that sets the WiFi country code.
 * The String extra EXTRA_COUNTRY_ISO of the intent cannot be null.
 */
public class WifiCountryCodeService extends IntentService {
    private static final String TAG = "WifiCountryCodeService";

    public WifiCountryCodeService() {
        super("WifiCountryCodeService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final String action = intent.getAction();
        if (action.equals(SettingsIntents.ACTION_SET_WIFI_COUNTRY_CODE)) {
            String countryIso = Preconditions.checkNotNull(intent.getStringExtra(
                    SettingsIntents.EXTRA_COUNTRY_ISO));
            WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            if (wm == null) { // No WiFi, quit.
                return;
            }
            String wifiCountryCode = Settings.Global.getString(getContentResolver(),
                    Settings.Global.WIFI_COUNTRY_CODE);
            if (!countryIso.equals(wifiCountryCode)) {
                wm.setCountryCode(countryIso);
                Log.i(TAG, "Set WiFi country code to " + countryIso);
            }
        }
    }
}
