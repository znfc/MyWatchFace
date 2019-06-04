package com.google.android.clockwork.settings;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.Shadows.shadowOf;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import com.google.android.clockwork.settings.utils.FeatureManager;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import java.util.Arrays;
import java.util.Locale;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowContentResolver;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(ClockworkRobolectricTestRunner.class)
@Config(
    manifest = Config.NONE,
    sdk = 21,
    shadows = {ShadowContentResolver.class}
)
public class EnableAllLanguagesReceiverTest {

    private final EnableAllLanguagesReceiver receiver = new EnableAllLanguagesReceiver();

    @Mock private FeatureManager featureManager;
    private Context context;
    private TestContentProvider provider;

    @Before
    public void setUp() {
        initMocks(this);
        context = ShadowApplication.getInstance().getApplicationContext();
        FeatureManager.INSTANCE.setTestInstance(featureManager);
        provider =
                Robolectric.setupContentProvider(
                        TestContentProvider.class, SettingsContract.SETTINGS_AUTHORITY);
    }

    @After
    public void tearDown() {
        FeatureManager.INSTANCE.clearTestInstance();
    }

    private void onReceive(boolean value) {
        receiver.onReceive(
                context,
                new Intent(EnableAllLanguagesReceiver.ENABLE_LANGUAGES_INTENT)
                        .putExtra(EnableAllLanguagesReceiver.VALUE, value ? "TRUE" : "FALSE"));
    }

    @Test
    public void testOnReceiveLocalEdition() {
        when(featureManager.isLocalEditionDevice()).thenReturn(true);
        provider.init(featureManager);

        // Only the default locales should be returned
        assertThat(SupportedLocales.getLocales(context))
            .asList()
            .containsExactly(Locale.SIMPLIFIED_CHINESE, Locale.UK, Locale.US);
        // US English locale is supported by default
        assertEquals(
            Locale.US,
            SupportedLocales.getSupportedLocaleByLanguageOrCountry(context, "en", "US").locale);
        // Since fr-FR is not supported by default, we should get the default locale, zh-CN
        assertEquals(
            Locale.SIMPLIFIED_CHINESE,
            SupportedLocales.getSupportedLocaleByLanguageOrCountry(context, "fr", "FR").locale);

        // Enable all locales
        onReceive(true);

        // All locales are available now
        assertThat(SupportedLocales.getLocales(context)).isEqualTo(SystemLocales.LOCALES);
        // fr-FR should be supported now
        assertEquals(
            Locale.FRANCE,
            SupportedLocales.getSupportedLocaleByLanguageOrCountry(context, "fr", "FR").locale);

        // Switch back to default
        onReceive(false);

        // Back to restricted mode
        assertThat(SupportedLocales.getLocales(context))
            .asList()
            .containsExactly(Locale.SIMPLIFIED_CHINESE, Locale.UK, Locale.US);
        assertEquals(
            Locale.US,
            SupportedLocales.getSupportedLocaleByLanguageOrCountry(context, "en", "US").locale);
        assertEquals(
            Locale.SIMPLIFIED_CHINESE,
            SupportedLocales.getSupportedLocaleByLanguageOrCountry(context, "fr", "FR").locale);
    }

    @Test
    public void testOnReceiveRestOfWorld() {
        when(featureManager.isLocalEditionDevice()).thenReturn(false);
        provider.init(featureManager);

        // All locales are available by default
        assertThat(SupportedLocales.getLocales(context)).isEqualTo(SystemLocales.LOCALES);
        // en-US is supported
        assertEquals(
                Locale.US,
                SupportedLocales.getSupportedLocaleByLanguageOrCountry(context, "en", "US").locale);

        // Enable all locales
        onReceive(true);

        // Nothing should have changed
        assertThat(SupportedLocales.getLocales(context)).isEqualTo(SystemLocales.LOCALES);
        assertEquals(
                Locale.US,
                SupportedLocales.getSupportedLocaleByLanguageOrCountry(context, "en", "US").locale);

        // Switch back to default
        onReceive(false);

        // Nothing should have changed
        assertThat(SupportedLocales.getLocales(context)).isEqualTo(SystemLocales.LOCALES);
        assertEquals(
                Locale.US,
                SupportedLocales.getSupportedLocaleByLanguageOrCountry(context, "en", "US").locale);
    }

    public static class TestContentProvider extends ContentProvider {

        private String setupLocale;
        private boolean enableAllLanguages;

        void init(FeatureManager featureManager) {
            final boolean isLocalEdition = featureManager.isLocalEditionDevice();
            setupLocale = SupportedLocales.getDefaultLocale(isLocalEdition).toLanguageTag();
            enableAllLanguages = !isLocalEdition;
        }

        @Override
        public String getType(Uri uri) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean onCreate() {
            return true;
        }

        @Override
        public Cursor query(
                Uri uri,
                String[] projection,
                String selection,
                String[] selectionArgs,
                String sortOrder) {
            if (SettingsContract.SETUP_LOCALE_URI.equals(uri)) {
                final MatrixCursor matrixCursor =
                        new MatrixCursor(
                                new String[] {
                                    SettingsContract.COLUMN_KEY, SettingsContract.COLUMN_VALUE
                                });
                matrixCursor.addRow(new Object[] {SettingsContract.KEY_SETUP_LOCALE, setupLocale});
                matrixCursor.addRow(
                        new Object[] {
                            SettingsContract.KEY_ENABLE_ALL_LANGUAGES, enableAllLanguages ? 1 : 0
                        });
                return matrixCursor;
            }
            throw new UnsupportedOperationException();
        }

        @Override
        public Uri insert(Uri uri, ContentValues contentValues) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int delete(Uri uri, String selection, String[] selectionArgs) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
            if (SettingsContract.SETUP_LOCALE_URI.equals(uri)) {
                Integer enableAll = values.getAsInteger(SettingsContract.KEY_ENABLE_ALL_LANGUAGES);
                if (enableAll != null) {
                    enableAllLanguages = (enableAll != 0);
                }
                String languageTag = values.getAsString(SettingsContract.KEY_SETUP_LOCALE);
                if (languageTag != null) {
                    Locale newSetupLocale = Locale.forLanguageTag(languageTag);
                    if (Arrays.asList(SupportedLocales.getLocales(getContext()))
                            .contains(newSetupLocale)) {
                        setupLocale = languageTag;
                    }
                }
                return 1;
            }
            throw new UnsupportedOperationException();
        }
    }
}
