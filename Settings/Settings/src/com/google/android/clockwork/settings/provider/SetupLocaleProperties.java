package com.google.android.clockwork.settings.provider;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.android.clockwork.settings.utils.FeatureManager;
import com.google.android.clockwork.settings.utils.SettingsCursor;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.SupportedLocales;
import java.util.Arrays;
import java.util.Locale;

class SetupLocaleProperties extends PreferencesProperties {
    private static final String TAG = "SetupLocaleProp";

    private final FeatureManager mFeatureManager;
    private final BooleanProperty mEnableAll;

    public SetupLocaleProperties(SharedPreferences prefs, FeatureManager fm) {
        super(prefs, SettingsContract.SETUP_LOCALE_PATH);
        mFeatureManager = fm;

        final boolean isLocalEdition = fm.isLocalEditionDevice();
        final boolean isUnifiedBuild = fm.isUnifiedBuild();
        add(new SetupLocaleProperty(isLocalEdition));
        add(mEnableAll = new BooleanProperty(
                SettingsContract.KEY_ENABLE_ALL_LANGUAGES, isUnifiedBuild || !isLocalEdition));
    }

    private class SetupLocaleProperty extends Property {
        private String mSetupLocale;

        public SetupLocaleProperty(boolean isLocalEdition) {
            super(SettingsContract.KEY_SETUP_LOCALE);
            mSetupLocale = mPrefs.getString(SettingsContract.KEY_SETUP_LOCALE,
                    SupportedLocales.getDefaultLocale(isLocalEdition).toLanguageTag());
        }

        @Override
        public void populateQuery(SettingsCursor c) {
            c.addRow(SettingsContract.KEY_SETUP_LOCALE, mSetupLocale);
        }

        @Override
        public int updateProperty(ContentValues values, SharedPreferences.Editor editor) {
            String languageTag = values.getAsString(SettingsContract.KEY_SETUP_LOCALE);
            if (languageTag != null && !languageTag.equals(mSetupLocale)) {
                // Need to make sure the Locale is supported.
                Locale newSetupLocale = Locale.forLanguageTag(languageTag);

                Locale[] supportedLocales = SupportedLocales.getLocales(
                        mFeatureManager, () -> mEnableAll.mVal);
                if (!Arrays.asList(supportedLocales).contains(newSetupLocale)) {
                    Log.w(TAG, "attempt to set an unsupported setup locale: " + newSetupLocale);
                } else {
                    mSetupLocale = languageTag;
                    editor.putString(SettingsContract.KEY_SETUP_LOCALE, languageTag);
                    return 1;
                }
            }
            return 0;
        }
    }
}
