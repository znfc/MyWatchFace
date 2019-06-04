package com.google.android.clockwork.settings.time;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;

import com.google.android.clockwork.settings.DateTimeConfig;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.annotation.Config;

import java.util.TimeZone;

@RunWith(ClockworkRobolectricTestRunner.class)
@Config(manifest="vendor/google_clockwork/packages/Settings/AndroidManifest.xml")
public class TimeZoneMediatorTest {
    @Mock private Context mMockContext;
    @Mock private DateTimeConfig mMockDateTimeConfig;
    @Mock private AlarmManager mMockAlarmManager;
    @Mock private TimeZone mMockTimeZone;

    private TimeZoneMediator mTimeZoneMediator;

    @Before
    public void setUp() {
        initMocks(this);
        mTimeZoneMediator = spy(
                new TimeZoneMediator(mMockContext, true, mMockAlarmManager, mMockDateTimeConfig));
    }

    @Test
    public void testCancelPendingTasks() {
        // WHEN the time zone mediator is stopped
        mTimeZoneMediator.cancelPendingTasks();

        // THEN the time zone check alarm is canceled
        verify(mMockAlarmManager).cancel(any(PendingIntent.class));
    }

    @Test
    public void testAddPhoneUpdate() {
        // GIVEN auto timezone syncing is enabled
        when(mMockDateTimeConfig.getClockworkAutoTimeZoneMode())
                .thenReturn(SettingsContract.SYNC_TIME_ZONE_FROM_PHONE);

        // WHEN a phone update is added
        mTimeZoneMediator.addPhoneUpdate(TimeZone.getDefault().getID());

        // THEN the timezone is set
        verify(mMockAlarmManager).setTimeZone(TimeZone.getDefault().getID());
    }

    @Test
    public void testAddNitzUpdate() {
        // GIVEN auto timezone syncing is enabled
        when(mMockDateTimeConfig.getClockworkAutoTimeZoneMode())
                .thenReturn(SettingsContract.SYNC_TIME_ZONE_FROM_PHONE);

        when(mMockTimeZone.getOffset(anyLong())).thenReturn(123);
        when(mMockTimeZone.getID()).thenReturn("foo");
        when(mTimeZoneMediator.getCurrentTimeZone()).thenReturn(mMockTimeZone);

        // WHEN a nitz update is added
        mTimeZoneMediator.addNitzUpdate("GMT");

        // THEN a time zone check alarm is set
        verify(mMockAlarmManager).set(anyInt(), anyLong(), any(PendingIntent.class));
    }

    @Test
    public void testAddNitzUpdateSameTimeZone() {
        // GIVEN auto timezone syncing is enabled
        when(mMockDateTimeConfig.getClockworkAutoTimeZoneMode())
                .thenReturn(SettingsContract.SYNC_TIME_ZONE_FROM_PHONE);

        // WHEN a nitz update is added with the existing timezone ID
        mTimeZoneMediator.addNitzUpdate(TimeZone.getDefault().getID());

        // THEN a time zone check alarm is not set
        verify(mMockAlarmManager, never()).set(anyInt(), anyLong(), any(PendingIntent.class));
    }

    @Test
    public void testAddNitzUpdatePhoneUpdateRecent() {
        // GIVEN auto timezone syncing is enabled
        when(mMockDateTimeConfig.getClockworkAutoTimeZoneMode())
                .thenReturn(SettingsContract.SYNC_TIME_ZONE_FROM_PHONE);

        // GIVEN a phone update is added
        mTimeZoneMediator.addPhoneUpdate(TimeZone.getDefault().getID());

        when(mMockTimeZone.getOffset(anyLong())).thenReturn(123);
        when(mMockTimeZone.getID()).thenReturn("foo");
        when(mTimeZoneMediator.getCurrentTimeZone()).thenReturn(mMockTimeZone);

        // WHEN a nitz update is added
        mTimeZoneMediator.addNitzUpdate("GMT");

        // THEN a time zone check alarm is not set
        verify(mMockAlarmManager, never()).set(anyInt(), anyLong(), any(PendingIntent.class));
    }
}