package android.support.wearable.input;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.wearable.input.WearableButtons.ButtonInfo;
import android.view.KeyEvent;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(WearableButtons.class)
public class ShadowWearableButtons {

    public static final float STEM_1_X = 5f;
    public static final float STEM_1_Y = 1f;
    public static final float STEM_2_X = 0f;
    public static final float STEM_2_Y = 2f;

    public static final int NUM_BUTTONS = 3;

    public static final String STEM_1_LABEL = "STEMCODE_1";
    public static final String STEM_2_LABEL = "STEMCODE_2";
    public static final String STEM_3_LABEL = "STEMCODE_3";

    /**
     * Stub for getButtonInfo
     *
     * @param context Context of the app
     * @param keycode Requested keycode
     * @return Returns hardcoded values for KEYCODE_STEM_1 and KEYCODE_STEM_2. KEYCODE_STEM_3 return
     *     null. All other keycodes throw an exception as a convenience to bring attention to a
     *     misused API.
     */
    @Implementation
    public static ButtonInfo getButtonInfo(Context context, int keycode) {
        float screenLocationX;
        float screenLocationY;
        int locationZone;
        switch (keycode) {
            case KeyEvent.KEYCODE_STEM_1:
                screenLocationX = STEM_1_X;
                screenLocationY = STEM_1_Y;
                locationZone = WearableButtons.LOC_LEFT_TOP;
                break;
            case KeyEvent.KEYCODE_STEM_2:
                screenLocationX = STEM_2_X;
                screenLocationY = STEM_2_Y;
                locationZone = WearableButtons.LOC_LEFT_BOTTOM;
                break;
            case KeyEvent.KEYCODE_STEM_3:
                return null;
            default:
                throw new IllegalArgumentException(
                        "Test: Unexpected keycode. "
                                + "Did you mean to call this API with a non-button keycode? "
                                + "keycode = " + keycode);
        }

        return new ButtonInfo(keycode, screenLocationX, screenLocationY, locationZone);
    }

    @Implementation
    public static final CharSequence getButtonLabel(Context context, int keycode) {
        switch (keycode) {
            case KeyEvent.KEYCODE_STEM_1:
                return STEM_1_LABEL;
            case KeyEvent.KEYCODE_STEM_2:
                return STEM_2_LABEL;
            case KeyEvent.KEYCODE_STEM_3:
                return STEM_3_LABEL;
            default:
                throw new IllegalArgumentException(
                        "Test: Unexpected keycode. "
                                + "Did you mean to call this API with a non-button keycode? "
                                + "keycode = " + keycode);
        }
    }

    @Implementation
    public static final Drawable getButtonIcon(Context context, int keycode) {
        return null;
    }

    @Implementation
    public static int getButtonCount(Context context) {
        return NUM_BUTTONS;
    }
}
