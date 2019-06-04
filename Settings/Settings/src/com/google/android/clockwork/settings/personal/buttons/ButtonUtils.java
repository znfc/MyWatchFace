package com.google.android.clockwork.settings.personal.buttons;

import android.content.res.Resources;
import android.view.KeyEvent;

import com.google.android.apps.wearable.settings.R;

/**
 * Stem button Utils
 */
public final class ButtonUtils {
    private static final String TAG = "ButtonUtils";

    public static final int[] CONFIGURABLE_BUTTON_KEYCODES =
        { KeyEvent.KEYCODE_STEM_1, KeyEvent.KEYCODE_STEM_2, KeyEvent.KEYCODE_STEM_3 };

    private ButtonUtils() {
        throw new RuntimeException("ButtonUtils should not be instantiated");
    }

    public static String getStemTypeKey(int keycode) {
        switch (keycode) {
            case KeyEvent.KEYCODE_STEM_1:
                return Constants.PREF_STEM_1_TYPE;
            case KeyEvent.KEYCODE_STEM_2:
                return Constants.PREF_STEM_2_TYPE;
            case KeyEvent.KEYCODE_STEM_3:
                return Constants.PREF_STEM_3_TYPE;
            default:
                throw new IllegalArgumentException("Unexpected keycode");
        }
    }

    public static String getStemDataKey(int keycode) {
        switch (keycode) {
            case KeyEvent.KEYCODE_STEM_1:
                return Constants.PREF_STEM_1_DATA;
            case KeyEvent.KEYCODE_STEM_2:
                return Constants.PREF_STEM_2_DATA;
            case KeyEvent.KEYCODE_STEM_3:
                return Constants.PREF_STEM_3_DATA;
            default:
                throw new IllegalArgumentException("Unexpected keycode");
        }
    }

    public static String getStemDefaultDataKey(int keycode) {
        switch (keycode) {
            case KeyEvent.KEYCODE_STEM_1:
                return Constants.PREF_STEM_1_DEFAULT_DATA;
            case KeyEvent.KEYCODE_STEM_2:
                return Constants.PREF_STEM_2_DEFAULT_DATA;
            case KeyEvent.KEYCODE_STEM_3:
                return Constants.PREF_STEM_3_DEFAULT_DATA;
            default:
                throw new IllegalArgumentException("Unexpected keycode");
        }
    }

    public static String getStemDefaultDataValue(Resources resources, int keycode) {
        String componentName;
        switch (keycode) {
            case KeyEvent.KEYCODE_STEM_1:
                componentName = resources.getString(R.string.config_defaultStem1ComponentName);
                break;
            case KeyEvent.KEYCODE_STEM_2:
                componentName = resources.getString(R.string.config_defaultStem2ComponentName);
                break;
            case KeyEvent.KEYCODE_STEM_3:
                componentName = resources.getString(R.string.config_defaultStem3ComponentName);
                break;
            default:
                throw new IllegalArgumentException("Invalid stem id: " + keycode);
        }
        return componentName;
    }
}
