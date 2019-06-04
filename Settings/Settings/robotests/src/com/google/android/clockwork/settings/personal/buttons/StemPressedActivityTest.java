package com.google.android.clockwork.settings.personal.buttons;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.os.Build;
import android.view.KeyEvent;

import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.util.ReflectionHelpers;

/** Unit tests for {@link StemPressedActivity}. */
@RunWith(ClockworkRobolectricTestRunner.class)
public class StemPressedActivityTest {
    private StemPressedActivity mActivity;

    @Test
    public void testOnCreate_isFinishing() {
        Intent intent = new Intent();
        intent.putExtra("stem_id", 1);
        StemPressedActivity activity =
            Robolectric.buildActivity(StemPressedActivity.class, intent).create().get();
        Intent nextIntent = ShadowApplication.getInstance().peekNextStartedActivity();
        // Since we are not setting up ButtonManager properly, the default Activity is going to be
        // started.
        assertEquals("com.google.android.clockwork.settings.MainSettingsActivity",
            nextIntent.getComponent().getClassName());
        assertTrue(activity.isFinishing());
    }

    @Test
    public void testGetKeycodeFromIntent_Stem1() {
        Intent intent = new Intent();
        intent.putExtra("stem_id", 1);
        StemPressedActivity activity =
            Robolectric.buildActivity(StemPressedActivity.class, intent).get();
        assertEquals(KeyEvent.KEYCODE_STEM_1, activity.getKeycodeFromIntent(intent));
    }

    @Test
    public void testGetKeycodeFromIntent_Stem2() {
        Intent intent = new Intent();
        intent.putExtra("stem_id", 2);
        StemPressedActivity activity =
            Robolectric.buildActivity(StemPressedActivity.class, intent).get();
        assertEquals(KeyEvent.KEYCODE_STEM_2, activity.getKeycodeFromIntent(intent));
    }

    @Test
    public void testGetKeycodeFromIntent_Stem3() {
        Intent intent = new Intent();
        intent.putExtra("stem_id", 3);
        StemPressedActivity activity =
            Robolectric.buildActivity(StemPressedActivity.class, intent).get();
        assertEquals(KeyEvent.KEYCODE_STEM_3, activity.getKeycodeFromIntent(intent));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetKeycodeFromIntent_StemUnknown_UserdebugBuild() {
        setBuildType("userdebug");

        Intent intent = new Intent();
        intent.putExtra("stem_id", 4);
        StemPressedActivity activity =
            Robolectric.buildActivity(StemPressedActivity.class, intent).get();
        activity.getKeycodeFromIntent(intent);
    }

    @Test
    public void testGetKeycodeFromIntent_StemUnknown_UserBuild() {
        setBuildType("user");

        Intent intent = new Intent();
        intent.putExtra("stem_id", 4);
        StemPressedActivity activity =
            Robolectric.buildActivity(StemPressedActivity.class, intent).get();
        assertEquals(-1, activity.getKeycodeFromIntent(intent));
    }

    private void setBuildType(String type) {
        ReflectionHelpers.setStaticField(Build.class, "TYPE", type);
    }
}
