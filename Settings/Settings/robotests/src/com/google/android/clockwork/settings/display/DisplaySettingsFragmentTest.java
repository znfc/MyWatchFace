package com.google.android.clockwork.settings.display;

import android.content.Context;
import android.preference.Preference;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.util.FragmentTestUtil;

@RunWith(ClockworkRobolectricTestRunner.class)
public class DisplaySettingsFragmentTest {
    private static final String KEY_PREF_SCREEN_ORIENTATION = "pref_screenOrientation";

    private Context mContext;
    private DisplaySettingsFragment mFragment;

    @Before
    public void setup() {
        mFragment = new DisplaySettingsFragment();
        mContext = ShadowApplication.getInstance().getApplicationContext();
    }

    @Test
    public void testWatchfacePref() {
        mFragment.initWatchface(new Preference(mContext));
    }

    @Test
    public void initScreenOrientationPref_unsupported() {
        mFragment.setLeftyModeSupportedForTest(false);
        FragmentTestUtil.startFragment(mFragment);
        Assert.assertNull(mFragment.findPreference(KEY_PREF_SCREEN_ORIENTATION));
    }

    @Test
    public void initScreenOrientationPref_supported() {
        mFragment.setLeftyModeSupportedForTest(true);
        FragmentTestUtil.startFragment(mFragment);
        Assert.assertNotNull(mFragment.findPreference(KEY_PREF_SCREEN_ORIENTATION));
    }
}
