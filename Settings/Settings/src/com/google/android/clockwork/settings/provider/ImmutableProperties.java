package com.google.android.clockwork.settings.provider;

import android.content.ContentValues;
import android.text.TextUtils;
import com.google.android.clockwork.settings.utils.SettingsCursor;
import com.google.common.base.Preconditions;

class ImmutableProperties extends SettingProperties {
    private final Object mVal;
    private final String mKey;

    public ImmutableProperties(String path, String key, Object val) {
        super(path);
        Preconditions.checkArgument(!TextUtils.isEmpty(key),
                "key \"%s\" cannot be null or empty", key);
        mKey = key;
        mVal = val;
    }

    @Override
    public SettingsCursor query() {
        return new SettingsCursor(mKey, mVal);
    }

    @Override
    public int update(ContentValues values) {
        // updates not supported as value is immutable
        throw new UnsupportedOperationException("path \"" + getPath() + "\" is not mutable" );
    }
}
