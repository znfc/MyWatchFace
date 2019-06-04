package com.google.android.clockwork.settings.time;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import android.content.Context;
import android.content.Intent;

import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.annotation.Config;

@RunWith(ClockworkRobolectricTestRunner.class)
@Config(manifest="vendor/google_clockwork/packages/Settings/AndroidManifest.xml")
public class NetworkTimeZoneSyncerTest {
    @Mock private Context mMockContext;

    private NetworkTimeZoneSyncer mNetworkTimeZoneSyncer;

    @Before
    public void setUp() {
        initMocks(this);
        mNetworkTimeZoneSyncer = new NetworkTimeZoneSyncer(mMockContext);
    }

    @Test
    public void testStart() {
        // WHEN network time zone syncing is started
        mNetworkTimeZoneSyncer.startNitzService();

        // THEN a NITZ updater intent is fired
        Intent expectedIntent = new Intent(mMockContext, NitzTimeUpdateService.class);
        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mMockContext).startService(intentCaptor.capture());
        assertEquals(expectedIntent.getAction(), intentCaptor.getValue().getAction());
    }

    @Test
    public void testStop() {
        // GIVEN network time syncing is started
        mNetworkTimeZoneSyncer.startNitzService();

        // WHEN network time syncing is stopped
        mNetworkTimeZoneSyncer.stopNitzService();

        // THEN a NITZ updater intent is fired
        Intent expectedIntent = new Intent(mMockContext, NitzTimeUpdateService.class);
        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mMockContext, times(2)).startService(intentCaptor.capture());
        assertEquals(expectedIntent.getAction(), intentCaptor.getValue().getAction());
    }
}
