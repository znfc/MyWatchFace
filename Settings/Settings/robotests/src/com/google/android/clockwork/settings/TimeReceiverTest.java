package com.google.android.clockwork.settings;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import android.content.Context;
import android.content.Intent;

import com.google.android.clockwork.settings.time.TimeIntents;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(ClockworkRobolectricTestRunner.class)
public class TimeReceiverTest {
    @Mock private Context mMockContext;

    private final TimeReceiver receiver = new TimeReceiver();

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void testAirplaneModeOff() {
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", false);

        receiver.onReceive(mMockContext, intent);

        verify(mMockContext).startService(TimeIntents.getSetNtpTimeIntent(mMockContext));
    }

    @Test
    public void testAirplaneModeOn() {
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", true);

        receiver.onReceive(mMockContext, intent);

        verify(mMockContext, never()).startService(TimeIntents.getSetNtpTimeIntent(mMockContext));
    }
}
