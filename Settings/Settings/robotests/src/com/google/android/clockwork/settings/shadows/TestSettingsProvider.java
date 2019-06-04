package com.google.android.clockwork.settings.shadows;

import android.content.pm.ProviderInfo;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.provider.SettingsProvider;
import org.robolectric.Robolectric;

/** Shadow class to test settings provider */
public class TestSettingsProvider extends SettingsProvider {

    public static void initForTesting() {
          ProviderInfo info = new ProviderInfo();
          info.authority = SettingsContract.SETTINGS_AUTHORITY;
          Robolectric.buildContentProvider(TestSettingsProvider.class).create(info);
    }
}
