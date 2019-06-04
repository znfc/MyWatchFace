package com.google.android.clockwork.settings.apps;

import android.os.Bundle;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.util.FragmentTestUtil;

/** Test advance app settings fragment */
@RunWith(ClockworkRobolectricTestRunner.class)
public class AdvancedAppSettingsFragmentTest {

    @Test
    public void testPackageInfoEmpty() {
        AdvancedAppSettingsFragment fragment = new AdvancedAppSettingsFragment();
        Bundle args = new Bundle();

        args.putString(AppInfoBase.ARG_PACKAGE_NAME, "");
        fragment.setArguments(args);

        ShadowLooper.pauseMainLooper();
        FragmentTestUtil.startVisibleFragment(fragment);
    }

    @Test
    public void testPackageInfoNull() {
        AdvancedAppSettingsFragment fragment = new AdvancedAppSettingsFragment();
        Bundle args = new Bundle();

        // Provide a package that will yield no package info.
        args.putString(AppInfoBase.ARG_PACKAGE_NAME, "package.1");
        fragment.setArguments(args);

        ShadowLooper.pauseMainLooper();
        FragmentTestUtil.startVisibleFragment(fragment);
    }
}
