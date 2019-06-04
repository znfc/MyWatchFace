package com.google.android.clockwork.settings.system;

import static org.mockito.Mockito.when;
import static org.mockito.Matchers.anyInt;

import android.os.BatteryManager;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.util.FragmentTestUtil;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(ClockworkRobolectricTestRunner.class)
public class SystemSettingsFragmentTest {
    private SystemSettingsFragment mFragment;

    @Mock
    private BatteryManager mBatteryManager;

    @Before
    public void setup() {
        mFragment = new SystemSettingsFragment();
        MockitoAnnotations.initMocks(this);
        when(mBatteryManager.getIntProperty(anyInt())).thenReturn(0);
        mFragment.mBatteryManager = mBatteryManager;
    }

    @Test
    public void testStartFragment() {
        // WHEN fragment is started
        FragmentTestUtil.startFragment(mFragment);

        // THEN no exceptions thrown
    }
}
