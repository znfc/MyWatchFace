package com.google.android.clockwork.settings.time;

import static junit.framework.Assert.assertEquals;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;

import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlarmManager;
import org.robolectric.shadows.ShadowApplication;

@RunWith(ClockworkRobolectricTestRunner.class)
@Config(manifest="vendor/google_clockwork/packages/Settings/AndroidManifest.xml")
public class TimeSyncManagerTest {
    private Context mContext;

    @Mock private PhoneTimeSyncer mMockPhoneTimeSyncer;
    @Mock private NetworkTimeSyncer mMockNetworkTimeSyncer;
    @Mock private NitzTimeSyncer mMockNitzTimeSyncer;

    private AlarmManager mAlarmManager;
    private ShadowAlarmManager mShadowAlarmManager;
    private TimeSyncManager mTimeSyncManager;

    @Before
    public void setUp() {
        initMocks(this);

        mContext = spy(ShadowApplication.getInstance().getApplicationContext());

        mAlarmManager = mContext.getSystemService(AlarmManager.class);
        mShadowAlarmManager = shadowOf(mAlarmManager);

        mTimeSyncManager = new TimeSyncManager(mContext);
        mTimeSyncManager.setPhoneTimeSyncer(mMockPhoneTimeSyncer);
        mTimeSyncManager.setNetworkTimeSyncer(mMockNetworkTimeSyncer);
        mTimeSyncManager.setNitzTimeSyncer(mMockNitzTimeSyncer);
    }

    @Test
    public void testHandleNetworkTimeRefresh() {
        // WHEN handleNetworkTimeRefresh is called
        mTimeSyncManager.handleNetworkTimeRefresh();

        // THEN NetworkTimeSyncer#handleRefresh is called
        verify(mMockNetworkTimeSyncer).handleRefresh();
    }

    @Test
    public void testHandlePhoneTimeRequestTimeout() {
        // WHEN handlePhoneTimeRequestTimeout is called
        mTimeSyncManager.handlePhoneTimeRequestTimeout();

        // THEN PhoneTimeSyncer#handleNitzTimeSet is called
        verify(mMockPhoneTimeSyncer).handleRequestTimeout();
    }

    @Test
    public void testsetNtpTime() {
        // WHEN setNtpTime is called
        mTimeSyncManager.setNtpTime();

        // THEN NetworkTimeSyncer#setNtpTime is called
        verify(mMockNetworkTimeSyncer).setNtpTime();
    }

    @Test
    public void testStartPhoneTimeSyncer() {
        // WHEN PhoneTimeSyncer is started
        mTimeSyncManager.startPhoneTimeUpdate();

        // THEN phone time syncing is started
        verify(mMockPhoneTimeSyncer).startUpdate();
    }

    @Test
    public void testCancelPendingTasks() {
        // WHEN time sync is stopped
        mTimeSyncManager.cancelPendingTasks();

        // THEN phone time syncing is stopped
        verify(mMockPhoneTimeSyncer).resetTimeLatencyTracking();
        // AND network time syncing is stopped
        verify(mMockNetworkTimeSyncer).cancelRefreshTimeJob();
        // AND nitz time syncing is stopped
        verify(mMockNitzTimeSyncer).stopNitzService();
    }

    @Test
    public void testStartPhoneTimeUpdate() {
        // WHEN a phone time update is started
        mTimeSyncManager.startPhoneTimeUpdate();

        // THEN phone time syncer is called
        verify(mMockPhoneTimeSyncer).startUpdate();
    }

    @Test
    public void testHandlePhoneTimeSyncResponse() {
        // WHEN a phone time sync response is received
        mTimeSyncManager.handlePhoneTimeSyncResponse(123, 456);

        // THEN phone time syncer is called
        verify(mMockPhoneTimeSyncer).handleTimeSyncResponse(123, 456);
    }

    @Test
    public void testTimeSyncerCallbackSuccess() {
        // WHEN time is successfully synced
        mTimeSyncManager.mTimeSyncerCallback.onSuccess();

        // THEN time sync state is reset
        verify(mMockPhoneTimeSyncer).resetTimeLatencyTracking();
        assertEquals(mShadowAlarmManager.getNextScheduledAlarm().operation,
                PendingIntent.getService(
                        mContext,
                        TimeIntents.REQ_POLL_PHONE_TIME,
                        TimeIntents.getPollPhoneTimeIntent(mContext),
                        0));
        // AND fallbacks are stopped
        verify(mMockNetworkTimeSyncer).cancelRefreshTimeJob();
        verify(mMockNitzTimeSyncer).stopNitzService();
    }

    @Test
    public void testTimeSyncerCallbackFail() {
        // WHEN time sync fails
        mTimeSyncManager.mTimeSyncerCallback.onFailure();

        // THEN a retry is scheduled
        assertEquals(mShadowAlarmManager.getNextScheduledAlarm().operation,
                PendingIntent.getService(
                        mContext,
                        TimeIntents.REQ_POLL_PHONE_TIME,
                        TimeIntents.getPollPhoneTimeIntent(mContext),
                        0));
        // AND time sync state is maintained
        verify(mMockPhoneTimeSyncer, never()).resetTimeLatencyTracking();
    }

    @Test
    public void testTimeSyncerCallbackTotalFail() {
        // WHEN time sync exhausts all failure retries
        for (int i=0; i<=TimeSyncManager.MAX_FAILURE_RETRY_COUNT; i++) {
            mTimeSyncManager.mTimeSyncerCallback.onFailure();
        }

        // THEN time sync state is reset
        verify(mMockPhoneTimeSyncer).resetTimeLatencyTracking();
        assertEquals(mShadowAlarmManager.getNextScheduledAlarm().operation,
                PendingIntent.getService(
                        mContext,
                        TimeIntents.REQ_POLL_PHONE_TIME,
                        TimeIntents.getPollPhoneTimeIntent(mContext),
                        0));

        // AND fallbacks are started
        verify(mMockNetworkTimeSyncer).scheduleRefreshTimeJob();
        verify(mMockNitzTimeSyncer).startNitzService();
    }
}
