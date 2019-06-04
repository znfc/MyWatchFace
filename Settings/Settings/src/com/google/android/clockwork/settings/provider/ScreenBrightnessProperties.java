package com.google.android.clockwork.settings.provider;

import android.content.ContentValues;
import android.content.res.Resources;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.utils.SettingsCursor;

class ScreenBrightnessProperties extends SettingProperties {
    private int[] mScreenBrightnessLevels;

    public ScreenBrightnessProperties(Resources res) {
        super(SettingsContract.SCREEN_BRIGHTNESS_LEVELS_PATH);
        mScreenBrightnessLevels = res.getIntArray(R.array.brightness_levels);
    }

    @Override
    public SettingsCursor query() {
        SettingsCursor c = new SettingsCursor();
        for (int brightnessLevel : mScreenBrightnessLevels) {
            c.addRow(SettingsContract.KEY_SCREEN_BRIGHTNESS_LEVEL, brightnessLevel);
        }
        return c;
    }

    @Override
    public int update(ContentValues values) {
        // updates not supported as values are immutable
        throw new UnsupportedOperationException("SCREEN_BRIGHTNESS_LEVELS is not mutable");
    }
}
