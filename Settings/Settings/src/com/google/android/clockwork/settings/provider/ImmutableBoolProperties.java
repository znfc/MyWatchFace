package com.google.android.clockwork.settings.provider;

import android.content.ContentValues;
import android.text.TextUtils;
import com.google.android.clockwork.settings.utils.SettingsCursor;
import com.google.common.base.Preconditions;

class ImmutableBoolProperties extends SettingProperties {
    private final boolean mFlag;
    private final String mKey;

    public ImmutableBoolProperties(String path, String key, boolean flag) {
        super(path);
        Preconditions.checkArgument(!TextUtils.isEmpty(key),
                "key \"%s\" cannot be null or empty", key);
        mKey = key;
        mFlag = flag;
    }

    @Override
    public SettingsCursor query() {
        return new SettingsCursor(mKey, mFlag ? 1 : 0);
    }

    @Override
    public int update(ContentValues values) {
        // updates not supported as value is immutable
        throw new UnsupportedOperationException("path \"" + getPath() + "\" is not mutable" );
    }
}
