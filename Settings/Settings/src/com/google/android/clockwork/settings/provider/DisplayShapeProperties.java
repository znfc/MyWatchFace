package com.google.android.clockwork.settings.provider;

import android.content.SharedPreferences;
import com.google.android.clockwork.settings.SettingsContract;

class DisplayShapeProperties extends PreferencesProperties {
    public DisplayShapeProperties(SharedPreferences prefs) {
        super(prefs, SettingsContract.DISPLAY_SHAPE_PATH);

        addInt(SettingsContract.KEY_BOTTOM_OFFSET, 0);
        addInt(SettingsContract.KEY_DISPLAY_SHAPE, SettingsContract.DISPLAY_SHAPE_SQUARE,
                //valid values:
                SettingsContract.DISPLAY_SHAPE_SQUARE,
                SettingsContract.DISPLAY_SHAPE_ROUND);
    }
}
