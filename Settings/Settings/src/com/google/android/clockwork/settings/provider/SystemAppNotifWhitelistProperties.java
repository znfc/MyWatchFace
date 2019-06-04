package com.google.android.clockwork.settings.provider;

import android.content.ContentValues;
import android.content.res.Resources;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.utils.SettingsCursor;

class SystemAppNotifWhitelistProperties extends SettingProperties {
    private final String[] mSystemAppNotifWhitelist;

    public SystemAppNotifWhitelistProperties(Resources res) {
        super(SettingsContract.SYSTEM_APPS_NOTIF_WHITELIST_PATH);
        mSystemAppNotifWhitelist = res.getStringArray(R.array.system_app_notif_whitelist);
    }

    @Override
    public SettingsCursor query() {
        SettingsCursor c = new SettingsCursor();
        if (mSystemAppNotifWhitelist != null) {
            for (int i = 0; i < mSystemAppNotifWhitelist.length; ++i) {
                c.addRow(SettingsContract.KEY_SYSTEM_APPS_NOTIF_WHITELIST + i,
                        mSystemAppNotifWhitelist[i]);
            }
        }
        return c;
    }

    @Override
    public int update(ContentValues values) {
        // updates not supported as value is immutable
        throw new UnsupportedOperationException("SYSTEM_APPS_NOTIF_WHITELIST is not mutable");
    }
}
