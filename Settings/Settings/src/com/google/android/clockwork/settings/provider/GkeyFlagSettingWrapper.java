package com.google.android.clockwork.settings.provider;

import android.content.ContentValues;
import android.content.SharedPreferences;
import com.google.android.clockwork.settings.utils.SettingsCursor;
import com.google.android.gsf.GservicesValue;
import com.google.common.base.Preconditions;

/**
 * Wraps a boolean GservicesValue as a single key property. This wrapper is used as a simple way to
 * access Gkey flags via the settings provider and does not notify any listeners when its value has
 * changed and does not allow updates to the value through this interface. Changes and listening to
 * changes should be done through the normal Gkeys interface.
 */
class GkeyFlagSettingWrapper extends SettingProperties {
    private final GservicesValue<Boolean> mFlag;
    private final String mKey;

    /**
     * @param path path of the property.
     * @param key the key to access the flag in the property.
     * @param flag the GservicesValue to wrap as the property.
     */
    public GkeyFlagSettingWrapper(String path, String key, GservicesValue<Boolean> flag) {
        super(path);
        mKey = Preconditions.checkNotNull(key);
        mFlag = Preconditions.checkNotNull(flag);
    }

    /** @return cursor with the flag as either 1 or 0 for true or false. */
    @Override
    public SettingsCursor query() {
        return new SettingsCursor(mKey, mFlag.get() ? 1 : 0);
    }

    @Override
    public int update(ContentValues values) {
        // updates not supported as value will only be updated via Gkeys
        throw new UnsupportedOperationException(
                "path \"" + getPath() + "\" is not mutable; updates are done via Gkeys");
    }
}
