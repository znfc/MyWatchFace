package com.google.android.clockwork.settings.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.res.Resources;
import android.content.SharedPreferences;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.common.base.Preconditions;
import java.util.function.Supplier;

class WristGesturesProperties extends PreferencesProperties {
    private final Supplier<ContentResolver> mResolver;

    public WristGesturesProperties(SharedPreferences prefs, Resources res,
            Supplier<ContentResolver> resolver) {
        super(prefs, SettingsContract.WRIST_GESTURES_ENABLED_PATH);
        mResolver = Preconditions.checkNotNull(resolver);

        add(new WristGesturesProperty(res));
    }

    private class WristGesturesProperty extends BooleanProperty {
        WristGesturesProperty(Resources res) {
            super(SettingsContract.KEY_WRIST_GESTURES_ENABLED,
                    res.getBoolean(R.bool.config_wristGesturesDefaultEnabled));
        }

        @Override
        public int updateProperty(ContentValues values, SharedPreferences.Editor editor) {
            if (!mPrefs.contains(mKey)) {
                mResolver.get().notifyChange(
                        SettingsContract.WRIST_GESTURES_ENABLED_PREF_EXISTS_URI, null);
            }
            return super.updateProperty(values, editor);
        }
    }
}
