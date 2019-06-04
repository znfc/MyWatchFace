package com.google.android.clockwork.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.google.android.clockwork.settings.utils.FeatureManager;
import java.util.Locale;

/** Handle intent to enable all available locales for Local Edition build. */
public class EnableAllLanguagesReceiver extends BroadcastReceiver {

    static final String ENABLE_LANGUAGES_INTENT =
            "com.google.android.clockwork.ENABLE_ALL_LANGUAGES";
    static final String VALUE = "value";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ENABLE_LANGUAGES_INTENT.equals(intent.getAction())) {
            boolean enableAll = "TRUE".equals(intent.getStringExtra(VALUE));
            FeatureManager featureManager = FeatureManager.INSTANCE.get(context);
            Locale locale =
                    SupportedLocales.getDefaultLocale(
                            featureManager.isLocalEditionDevice() && !enableAll);
            SettingsContentResolver resolver =
                    new DefaultSettingsContentResolver(context.getContentResolver());
            resolver.putIntegerValueForKey(
                    SettingsContract.SETUP_LOCALE_URI,
                    SettingsContract.KEY_ENABLE_ALL_LANGUAGES,
                    enableAll ? 1 : 0);
            resolver.putStringValueForKey(
                    SettingsContract.SETUP_LOCALE_URI,
                    SettingsContract.KEY_SETUP_LOCALE,
                    locale.toLanguageTag());
            context.startService(SettingsIntents.getSetLocaleIntent(locale));
        }
    }
}
