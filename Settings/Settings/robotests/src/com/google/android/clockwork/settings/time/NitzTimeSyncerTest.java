package com.google.android.clockwork.settings.time;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.MockitoAnnotations.initMocks;

import android.content.Context;
import android.content.Intent;

import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

@RunWith(ClockworkRobolectricTestRunner.class)
@Config(manifest="vendor/google_clockwork/packages/Settings/AndroidManifest.xml")
public class NitzTimeSyncerTest {
    private Context mContext;

    @Before
    public void setUp() {
        initMocks(this);
        mContext = spy(ShadowApplication.getInstance().getApplicationContext());
    }

    @Test
    public void testNitzServiceStartsWhenCellular() {
        // GIVEN cellular enabled
        NitzTimeSyncer nitzTimeSyncer = new NitzTimeSyncer(mContext, true);
        // WHEN NitzTimeSyncer is started
        nitzTimeSyncer.startNitzService();

        // THEN NitzTimeUpdateService is started
        Intent serviceIntent = ShadowApplication.getInstance().peekNextStartedService();
        assertThat(serviceIntent.getComponent().getClassName(),
                is(NitzTimeUpdateService.class.getCanonicalName()));
    }

    @Test
    public void testNitzServiceNotStartsWhenNotCellular() {
        // GIVEN cellular disabled
        NitzTimeSyncer nitzTimeSyncer = new NitzTimeSyncer(mContext, false);
        // WHEN NitzTimeSyncer is started
        nitzTimeSyncer.startNitzService();

        // THEN NitzTimeUpdateService is not started
        assertThat(ShadowApplication.getInstance().peekNextStartedService(), nullValue());
    }
}