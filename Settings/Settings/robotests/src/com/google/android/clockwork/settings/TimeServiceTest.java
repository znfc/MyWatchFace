package com.google.android.clockwork.settings;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.RuntimeEnvironment.application;

import android.app.AlarmManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import com.google.android.clockwork.settings.time.TimeIntents;
import com.google.android.clockwork.settings.time.TimeSyncManager;
import com.google.android.clockwork.settings.time.TimeZoneMediator;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

@RunWith(ClockworkRobolectricTestRunner.class)
public class TimeServiceTest {

  @Mock private DateTimeConfig mMockDateTimeConfig;
  @Mock private TimeSyncManager mMockTimeSyncManager;
  @Mock private TimeZoneMediator mMockTimeZoneMediator;
  @Mock private AlarmManager mMockAlarmManager;
  @Mock private Context mMockContext;

  private ContentResolver mContentResolver;
  private TimeServiceImpl mTimeService;

  @Before
  public void setUp() {
    initMocks(this);
    mContentResolver = application.getContentResolver();
    mTimeService = new TimeServiceImpl(
        mMockDateTimeConfig,
        mMockTimeSyncManager,
        mMockTimeZoneMediator,
        mMockAlarmManager,
        mContentResolver);
  }

  @Test
  public void testHandleCheckTimeZone() {
    Intent intent = TimeIntents.getCheckTimeZoneIntent(mMockContext);

    mTimeService.onHandleIntent(mMockContext, intent);

    verify(mMockTimeZoneMediator).handleCheckTimeZone(anyLong());
  }

  @Test
  public void testSetNtpTime() {
    Intent intent = TimeIntents.getSetNtpTimeIntent(mMockContext);

    mTimeService.onHandleIntent(mMockContext, intent);

    verify(mMockTimeSyncManager).setNtpTime();
  }

  @Test
  public void testHandleNetworkTimeRefresh() {
    Intent intent = TimeIntents.getNetworkTimeRefreshIntent(mMockContext);

    mTimeService.onHandleIntent(mMockContext, intent);

    verify(mMockTimeSyncManager).handleNetworkTimeRefresh();
  }

  @Test
  public void testHandlePhoneTimeRequestTimeout() {
    Intent intent = TimeIntents.getPhoneTimeRequestTimeoutIntent(mMockContext);

    mTimeService.onHandleIntent(mMockContext, intent);

    verify(mMockTimeSyncManager).handlePhoneTimeRequestTimeout();
  }

  @Test
  public void testSetTime() {
    Intent intent = new Intent(SettingsIntents.ACTION_SET_TIME);
    intent.putExtra(SettingsIntents.EXTRA_CURRENT_TIME_MILLIS, 100L);
    intent.putExtra(SettingsIntents.EXTRA_SENT_AT_TIME, 50L);

    mTimeService.onHandleIntent(mMockContext, intent);

    verify(mMockAlarmManager).setTime(anyLong());
  }

  @Test
  public void testSetTimeZone() {
    final String fakeTimeZone = "fake_time_zone";
    Intent intent = new Intent(SettingsIntents.ACTION_SET_TIMEZONE);
    intent.putExtra(SettingsIntents.EXTRA_TIMEZONE, fakeTimeZone);

    mTimeService.onHandleIntent(mMockContext, intent);
    verify(mMockTimeZoneMediator).addPhoneUpdate("fake_time_zone");
  }

  @Test
  public void testSetIs24Hour() {
    Intent intent = new Intent(SettingsIntents.ACTION_SET_24HOUR);
    intent.putExtra(SettingsIntents.EXTRA_IS_24_HOUR, true);

    mTimeService.onHandleIntent(mMockContext, intent);

    assertEquals(
        TimeServiceImpl.HOURS_24,
        Settings.System.getString(mContentResolver, Settings.System.TIME_12_24));

    verify(mMockDateTimeConfig).set24HourMode(true);

    ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
    verify(mMockContext).sendBroadcast(intentCaptor.capture());
    assertEquals(Intent.ACTION_TIME_CHANGED, intentCaptor.getValue().getAction());
    assertEquals(
        TimeServiceImpl.EXTRA_TIME_PREF_VALUE_USE_24_HOUR,
        intentCaptor.getValue().getIntExtra(Intent.EXTRA_TIME_PREF_24_HOUR_FORMAT, -1));
  }

  @Test
  public void testUnsetIs24Hour() {
    Intent intent = new Intent(SettingsIntents.ACTION_SET_24HOUR);

    mTimeService.onHandleIntent(mMockContext, intent);

    assertEquals(
        null,
        Settings.System.getString(mContentResolver, Settings.System.TIME_12_24));

    verify(mMockDateTimeConfig).set24HourMode(false);

    ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
    verify(mMockContext).sendBroadcast(intentCaptor.capture());
    assertEquals(Intent.ACTION_TIME_CHANGED, intentCaptor.getValue().getAction());
    assertEquals(
        -1,
        intentCaptor.getValue().getIntExtra(Intent.EXTRA_TIME_PREF_24_HOUR_FORMAT, -1));
  }

  @Test
  public void testSetPhoneTimeSyncerHomeTime() {
    Intent intent = new Intent(SettingsIntents.ACTION_SET_HOME_TIME);
    intent.putExtra(SettingsIntents.EXTRA_HOME_TIME, 100L);
    intent.putExtra(SettingsIntents.EXTRA_COMPANION_TIME, 1000L);

    mTimeService.onHandleIntent(mMockContext, intent);

    verify(mMockTimeSyncManager).handlePhoneTimeSyncResponse(100L, 1000L);
  }

  @Test
  public void testRequestPhoneTimeSyncerUpdate() {
    Intent intent = new Intent(SettingsIntents.ACTION_REQUEST_TIME_SYNCER_UPDATE);

    mTimeService.onHandleIntent(mMockContext, intent);

    verify(mMockTimeSyncManager).startPhoneTimeUpdate();
  }

  @Test
  public void testEvaluateTimeSyncingSyncFromPhone() {
    Intent intent = new Intent(SettingsIntents.ACTION_EVALUATE_TIME_SYNCING);

    when(mMockDateTimeConfig.getClockworkAutoTimeMode())
        .thenReturn(SettingsContract.SYNC_TIME_FROM_PHONE);

    mTimeService.onHandleIntent(mMockContext, intent);

    verify(mMockTimeSyncManager).startPhoneTimeUpdate();

    Intent expectedIntent = SettingsIntents.getReevaluatePhone24HrFormatIntent();
    ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
    verify(mMockContext).startService(intentCaptor.capture());
    assertEquals(expectedIntent.getAction(), intentCaptor.getValue().getAction());
    assertEquals(expectedIntent.getComponent(), intentCaptor.getValue().getComponent());
  }

  @Test
  public void testEvaluateTimeSyncingSyncFromNetwork() {
    Intent intent = new Intent(SettingsIntents.ACTION_EVALUATE_TIME_SYNCING);

    when(mMockDateTimeConfig.getClockworkAutoTimeMode())
            .thenReturn(SettingsContract.SYNC_TIME_FROM_NETWORK);

    mTimeService.onHandleIntent(mMockContext, intent);

    // sync from phone and network were combined in b/35318277
    // and standardized on SYNC_TIME_FROM_PHONE
    verify(mMockDateTimeConfig).setAutoTime(SettingsContract.SYNC_TIME_FROM_PHONE);

    verify(mMockTimeSyncManager).startPhoneTimeUpdate();

    Intent expectedIntent = SettingsIntents.getReevaluatePhone24HrFormatIntent();
    ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
    verify(mMockContext).startService(intentCaptor.capture());
    assertEquals(expectedIntent.getAction(), intentCaptor.getValue().getAction());
    assertEquals(expectedIntent.getComponent(), intentCaptor.getValue().getComponent());
  }

  @Test
  public void testEvaluateTimeSyncingAutoTimeOff() {
    Intent intent = new Intent(SettingsIntents.ACTION_EVALUATE_TIME_SYNCING);

    when(mMockDateTimeConfig.getClockworkAutoTimeMode())
        .thenReturn(SettingsContract.AUTO_TIME_OFF);

    mTimeService.onHandleIntent(mMockContext, intent);

    verify(mMockTimeSyncManager).cancelPendingTasks();
  }

  @Test
  public void testEvaluateTimeZoneSyncingSyncFromPhone() {
    Intent intent = new Intent(SettingsIntents.ACTION_EVALUATE_TIME_ZONE_SYNCING);

    when(mMockDateTimeConfig.getClockworkAutoTimeZoneMode())
        .thenReturn(SettingsContract.SYNC_TIME_ZONE_FROM_PHONE);

    mTimeService.onHandleIntent(mMockContext, intent);

    verify(mMockTimeZoneMediator).startNitzService();
  }

  @Test
  public void testEvaluateTimeZoneSyncingSyncFromNetwork() {
    Intent intent = new Intent(SettingsIntents.ACTION_EVALUATE_TIME_ZONE_SYNCING);

    when(mMockDateTimeConfig.getClockworkAutoTimeZoneMode())
            .thenReturn(SettingsContract.SYNC_TIME_ZONE_FROM_NETWORK);

    mTimeService.onHandleIntent(mMockContext, intent);

    // sync from phone and network were combined in b/35318281
    // and standardized on SYNC_TIME_ZONE_FROM_PHONE
    verify(mMockDateTimeConfig).setAutoTimeZone(SettingsContract.SYNC_TIME_ZONE_FROM_PHONE);

    verify(mMockTimeZoneMediator).startNitzService();
  }

  @Test
  public void testEvaluateTimeZoneSyncingAutoTimeZoneOff() {
    Intent intent = new Intent(SettingsIntents.ACTION_EVALUATE_TIME_ZONE_SYNCING);

    when(mMockDateTimeConfig.getClockworkAutoTimeZoneMode())
        .thenReturn(SettingsContract.AUTO_TIME_ZONE_OFF);

    mTimeService.onHandleIntent(mMockContext, intent);

    verify(mMockTimeZoneMediator).cancelPendingTasks();
  }
}