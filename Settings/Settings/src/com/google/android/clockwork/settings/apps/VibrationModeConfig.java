package com.google.android.clockwork.settings.apps;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import com.google.android.apps.wearable.settings.R;

import com.google.android.clockwork.settings.CardPreviewModeConfig;
import com.google.android.clockwork.settings.DefaultSettingsContentResolver;
import com.google.android.clockwork.settings.SettingsContract;

/**
 * Holds config for the default vibration strength/length.
 */
public class VibrationModeConfig {

    private static final String TAG = "VibrationModeConfig";
    private static final boolean DEBUG = false;

    // keep these the same as the XML
    public static final int NORMAL = 1;
    public static final int LONG = 2;
    public static final int DOUBLE = 3;

    private final DefaultSettingsContentResolver mSettings;
    private final Resources mResources;

    private final Object mLock = new Object();

    public VibrationModeConfig(Context ctx) {
        this(ctx, ctx.getResources());
    }

    public VibrationModeConfig(Context ctx, Resources res) {
        mSettings = new DefaultSettingsContentResolver(ctx.getContentResolver());
        mResources = res;
    }

    public int getVibrationMode() {

        String longPattern = mResources.getString((R.string.config_default_vibration_long));
        String doublePattern = mResources.getString((R.string.config_default_vibration_double));

        final String currentPattern;
        synchronized (mLock) {
            currentPattern = mSettings.getStringValueForKey(
                    SettingsContract.DEFAULT_VIBRATION_URI,
                    SettingsContract.KEY_DEFAULT_VIBRATION,
                    null);
        }

        if (longPattern.equals(currentPattern)) {
            return LONG;
        } else if (doublePattern.equals(currentPattern)) {
            return DOUBLE;
        }  else {
            return NORMAL;
        }
    }


    public void setVibrationMode(int mode) {

        if (DEBUG) {
            Log.d(TAG, "setVibrationMode to " + mode);
        }

        final String pattern = getVibrationPatternForMode(mode);

        synchronized (mLock) {
            if (!mSettings.putStringValueForKey(
                    SettingsContract.DEFAULT_VIBRATION_URI,
                    SettingsContract.KEY_DEFAULT_VIBRATION,
                    pattern)) {

                Log.e(TAG, "Failed to save vibration mode to settings");
            }
        }
    }

    public String getVibrationPatternForMode(int mode) {
        String pattern;
        if (mode == LONG) {
            pattern = mResources.getString((R.string.config_default_vibration_long));
        } else if (mode == DOUBLE) {
            pattern = mResources.getString((R.string.config_default_vibration_double));
        } else {
            pattern = mResources.getString((R.string.config_default_vibration));
        }
        return pattern;
    }
}
