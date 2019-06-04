package com.google.android.clockwork.settings.provider;

import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.common.system.WearSystemConstants;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.utils.SettingsCursor;
import com.google.android.clockwork.settings.utils.FeatureManager;
import java.util.function.Supplier;

/**
 * System related properties such as system version and capabilities.
 */
class SystemInfoProperties extends SettingProperties {
    private final long mAndroidWearVersion;
    private final long mCapabilities;
    private final int mEdition;

    public SystemInfoProperties(Resources res, Supplier<PackageManager> pm, FeatureManager fm) {
        super(SettingsContract.SYSTEM_INFO_PATH);
        mAndroidWearVersion = getLongResource(res, R.string.system_android_wear_version);

        mEdition = fm.isLocalEditionDevice()
                ? WearSystemConstants.EDITION_LOCAL : WearSystemConstants.EDITION_GLOBAL;

        long capabilities = fm.isLocalEditionDevice()
                ? getLongResource(res, R.string.default_le_system_capabilities)
                : getLongResource(res, R.string.default_system_capabilities);

        if (!pm.get().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            capabilities |= getBitMask(WearSystemConstants.CAPABILITY_COMPANION_LEGACY_CALLING);
        }
        if (pm.get().hasSystemFeature(PackageManager.FEATURE_AUDIO_OUTPUT)) {
            capabilities |= getBitMask(WearSystemConstants.CAPABILITY_SPEAKER);
        }
        capabilities |= getBitMask(WearSystemConstants.CAPABILITY_SETUP_PROTOCOMM_CHANNEL);
        mCapabilities = capabilities;

    }

    @Override
    public SettingsCursor query() {
        return new SettingsCursor()
                .addRow(SettingsContract.KEY_ANDROID_WEAR_VERSION, mAndroidWearVersion)
                .addRow(SettingsContract.KEY_SYSTEM_CAPABILITIES, mCapabilities)
                .addRow(SettingsContract.KEY_SYSTEM_EDITION, mEdition);
    }

    @Override
    public int update(ContentValues values) {
        throw new UnsupportedOperationException(); // updates not supported as values are immutable
    }

    private static long getLongResource(Resources res, int id) {
        final String maskVal = res.getString(id);
        return Long.parseLong(maskVal);
    }

    public static long getBitMask(int capability) {
        return 1 << (capability - 1);
    }
}
