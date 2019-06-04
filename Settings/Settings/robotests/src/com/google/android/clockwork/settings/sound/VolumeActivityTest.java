package com.google.android.clockwork.settings.sound;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.view.WindowManager.LayoutParams;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtils;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.Robolectric;

@RunWith(ClockworkRobolectricTestRunner.class)
@Config(shadows={VolumeActivityTest.ShadowRestrictedLockUtils.class})
public class VolumeActivityTest {

    @Test
    public void testFlagSetOnActivityLaunch() {
        Intent intent = new Intent();
        intent.putExtra(VolumeActivity.EXTRA_STREAM, AudioManager.STREAM_VOICE_CALL);

        VolumeActivity activity =
                Robolectric.buildActivity(VolumeActivity.class, intent).create().get();
        int flags = activity.getWindow().getAttributes().flags;

        assertTrue((flags & LayoutParams.FLAG_SHOW_WHEN_LOCKED) != 0);
    }

    @Implements(RestrictedLockUtils.class)
    protected static class ShadowRestrictedLockUtils {
        @Implementation
        public static EnforcedAdmin checkIfRestrictionEnforced(Context context,
                String userRestriction, int userId) {
            return null;
        }
    }
}
