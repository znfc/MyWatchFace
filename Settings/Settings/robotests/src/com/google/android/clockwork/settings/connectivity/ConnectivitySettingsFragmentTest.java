package com.google.android.clockwork.settings.connectivity;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.preference.SwitchPreference;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLooper;

/**
 * Tests for {@link ConnectivitySettingsFragment}.
 */
@RunWith(ClockworkRobolectricTestRunner.class)
public class ConnectivitySettingsFragmentTest {
    private ConnectivitySettingsFragment mFragment;

    @Before
    public void setUp() {
      MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testAirplaneMode_DisabledAfterSelected() {
        Context context = ShadowApplication.getInstance().getApplicationContext();
        SwitchPreference sp = new MySwitchPreference(context);

        mFragment = new ConnectivitySettingsFragment();
        mFragment.initAirplaneMode(sp, null);
        assertTrue(sp.isEnabled());
        ((MySwitchPreference) sp).doClick();
        assertTrue(!sp.isEnabled());
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        assertTrue(sp.isEnabled());
    }

    /**
     * Derived class of Switch Preference to allow click to be delivered.
     */
    public static class MySwitchPreference extends SwitchPreference {
        public MySwitchPreference(Context context) {
            super(context);
        }

        public void doClick() {
            super.onClick();
        }
    }
}
