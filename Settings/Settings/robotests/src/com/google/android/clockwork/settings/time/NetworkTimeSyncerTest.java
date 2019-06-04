package com.google.android.clockwork.settings.time;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.PersistableBundle;

import com.google.android.clockwork.settings.DateTimeConfig;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.shadows.ShadowApplication;

@RunWith(ClockworkRobolectricTestRunner.class)
public class NetworkTimeSyncerTest {
    @Mock private NetworkTimeSyncer.JobInfoBuilderFactory mMockJobInfoBuilderFactory;
    @Mock private ComponentName mMockComponentName;
    @Mock private DateTimeConfig mDateTimeConfig;
    @Mock private JobScheduler mMockJobScheduler;
    @Mock private TimeSyncerCallback mCallback;

    private Context mContext;

    @Before
    public void setUp() {
        initMocks(this);

        mContext = spy(ShadowApplication.getInstance().getApplicationContext());

        when(mContext.getSystemService(Context.JOB_SCHEDULER_SERVICE))
                .thenReturn(mMockJobScheduler);
        when(mMockJobInfoBuilderFactory.invoke())
                .thenReturn(new JobInfo.Builder(0, mMockComponentName));
    }

    @Test
    public void testScheduleJob() {
        // GIVEN a polling interval
        int pollingInterval = 50;
        int driftThreshold = 60;

        // AND NetworkTimeSyncer is started
        setupNetworkTimeSyncer(pollingInterval, driftThreshold);

        // THEN a refresh time job is scheduled immediately with the correct configs
        ArgumentCaptor<JobInfo> requestCaptor = ArgumentCaptor.forClass(JobInfo.class);
        verify(mMockJobScheduler).schedule(requestCaptor.capture());

        JobInfo jobInfo = requestCaptor.getValue();
        PersistableBundle extras = jobInfo.getExtras();

        assertEquals(0, jobInfo.getMinLatencyMillis());
        assertEquals(pollingInterval, extras.getLong(NetworkTimeSyncer.FRESHNESS_THRESHOLD));
        assertEquals(driftThreshold, extras.getInt(NetworkTimeSyncer.DRIFT_THRESHOLD));
    }

    @Test
    public void testSuccessOnRefreshIntent() {
        // GIVEN a polling interval
        int pollingInterval = 50;

        // AND handleRefresh is called
        setupNetworkTimeSyncer(pollingInterval, 0).handleRefresh();

        // THEN a success callback is called
        verify(mCallback).onSuccess();
    }

    private NetworkTimeSyncer setupNetworkTimeSyncer(int pollingInterval, int driftThreshold) {
        when(mDateTimeConfig.getClockworkAutoTimeMode()).thenReturn(
                SettingsContract.SYNC_TIME_FROM_PHONE);
        NetworkTimeSyncer networkTimeSyncer = new NetworkTimeSyncer(
                mContext,
                pollingInterval,
                driftThreshold,
                mDateTimeConfig,
                mMockJobInfoBuilderFactory,
                mCallback);
        networkTimeSyncer.scheduleRefreshTimeJob();

        ArgumentCaptor<JobInfo> requestCaptor = ArgumentCaptor.forClass(JobInfo.class);
        verify(mMockJobScheduler).schedule(requestCaptor.capture());

        JobInfo jobInfo = requestCaptor.getValue();

        assertEquals(0, jobInfo.getMinLatencyMillis());

        return networkTimeSyncer;
    }
}