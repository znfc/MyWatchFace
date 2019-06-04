package com.google.android.clockwork.settings.provider;

import android.content.ContentValues;
import android.content.res.Resources;
import android.os.SystemProperties;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.utils.SettingsCursor;
import com.android.internal.R;

class BurnInProtectionProperties extends SettingProperties {
    private static final String DEBUG_FORCE_BURN_IN = "persist.debug.force_burn_in";

    private boolean mEnableBurnInProtection;
    private boolean mForceBurnInProtection;

    BurnInProtectionProperties(Resources res) {
        super(SettingsContract.BURN_IN_CONFIG_PATH);
        mEnableBurnInProtection = res.getBoolean(R.bool.config_enableBurnInProtection);
        mForceBurnInProtection = SystemProperties.getBoolean(DEBUG_FORCE_BURN_IN, false);
    }

    @Override
    public SettingsCursor query() {
        return new SettingsCursor()
                .addRow(SettingsContract.KEY_BURN_IN_PROTECTION,
                        mForceBurnInProtection || mEnableBurnInProtection ? 1 : 0)
                .addRow(SettingsContract.KEY_BURN_IN_PROTECTION_DEV,
                        mForceBurnInProtection ? 1 : 0);
    }

    @Override
    public int update(ContentValues values) {
        mForceBurnInProtection = values.getAsBoolean(SettingsContract.KEY_BURN_IN_PROTECTION_DEV);
        SystemProperties.set(DEBUG_FORCE_BURN_IN, mForceBurnInProtection ? "1" : "0");
        return 1;
    }
}
