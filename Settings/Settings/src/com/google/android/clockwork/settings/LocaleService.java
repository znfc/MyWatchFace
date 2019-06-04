package com.google.android.clockwork.settings;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.IntentService;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.clockwork.settings.SettingsIntents;

import java.util.Locale;

public class LocaleService extends IntentService {
    private static final String TAG = "Clockwork.LocaleService";

    public LocaleService() {
        super("LocaleService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final String action = intent.getAction();
        if (action.equals(SettingsIntents.ACTION_SET_LOCALE)) {
            Locale locale = new Locale(
                    intent.getStringExtra(SettingsIntents.EXTRA_LOCALE_LANGUAGE),
                    intent.getStringExtra(SettingsIntents.EXTRA_LOCALE_COUNTRY),
                    intent.getStringExtra(SettingsIntents.EXTRA_LOCALE_VARIANT));

            try {
                IActivityManager am = ActivityManager.getService();
                Configuration config = am.getConfiguration();

                if (config.locale != null
                        && TextUtils.equals(locale.getLanguage(), config.locale.getLanguage())
                        && TextUtils.equals(locale.getCountry(), config.locale.getCountry())
                        && TextUtils.equals(locale.getVariant(), config.locale.getVariant())) {
                    Log.i(TAG, "system locale already set to " + locale);
                    return;
                }

                config.setLocale(locale);
                config.userSetLocale = true;

                am.updateConfiguration(config);
                Log.i(TAG, "changed system locale to " + locale);
            } catch (RemoteException e) {
                Log.e(TAG, "could not change system locale to " + locale, e);
            }
        }
    }
}
