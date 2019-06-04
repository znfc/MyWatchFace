package com.google.android.clockwork.settings.system;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.RuntimeEnvironment.application;

import android.app.AlarmManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import com.google.android.clockwork.settings.FakeSettingsContentResolver;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.SettingsIntents;
import com.google.android.clockwork.settings.TimeService;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

@RunWith(ClockworkRobolectricTestRunner.class)
public class DateTimeSettingsHelperTest {

  @Mock Context mMockContext;
  @Mock AlarmManager mMockAlarmManager;
  @Mock TimeZone mMockTimeZone;
  @Mock Calendar mMockCalendar;

  FakeSettingsContentResolver mFakeSettings;

  @Before
  public void setup() {
    initMocks(this);
    mFakeSettings = new FakeSettingsContentResolver();
  }

  @Test
  public void testSendTimeServiceIntent() {
    DateTimeSettingsHelper.sendTimeServiceIntent(
        mMockContext, SettingsIntents.ACTION_EVALUATE_TIME_SYNCING);

    ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
    verify(mMockContext).startService(intentCaptor.capture());
    assertEquals(
        "com.google.android.clockwork.settings.TimeService",
        intentCaptor.getValue().getComponent().getClassName());
    assertEquals(SettingsIntents.ACTION_EVALUATE_TIME_SYNCING, intentCaptor.getValue().getAction());
  }

  @Test
  public void testSetTime() {
    Calendar expectedCalendar = Calendar.getInstance();
    expectedCalendar.set(Calendar.HOUR_OF_DAY, 10);
    expectedCalendar.set(Calendar.MINUTE, 0);
    expectedCalendar.set(Calendar.SECOND, 0);
    expectedCalendar.set(Calendar.MILLISECOND, 0);

    DateTimeSettingsHelper.setTime(mMockAlarmManager, 10, 0);

    verify(mMockAlarmManager).setTime(expectedCalendar.getTimeInMillis());
  }

  @Test
  public void testSetDate() {
    Calendar expectedCalendar = Calendar.getInstance();
    expectedCalendar.set(Calendar.YEAR, 2017);
    expectedCalendar.set(Calendar.MONTH, 2);
    expectedCalendar.set(Calendar.DAY_OF_MONTH, 7);
    expectedCalendar.set(Calendar.HOUR_OF_DAY, 0);
    expectedCalendar.set(Calendar.MINUTE, 0);
    expectedCalendar.set(Calendar.SECOND, 0);
    expectedCalendar.set(Calendar.MILLISECOND, 0);

    Calendar mockCalendar = mock(Calendar.class);
    when(mockCalendar.getTimeInMillis()).thenReturn(expectedCalendar.getTimeInMillis());

    DateTimeSettingsHelper.setDate(mockCalendar, mMockAlarmManager, 2017, 2, 7);

    verify(mMockAlarmManager).setTime(expectedCalendar.getTimeInMillis());
  }

  @Test
  public void testSetTimeZone() {
    String fakeTimeZone = "fakeTimeZone";

    DateTimeSettingsHelper.setTimeZone(mMockAlarmManager, fakeTimeZone);

    verify(mMockAlarmManager).setTimeZone(fakeTimeZone);
  }

  @Test
  public void testGetTimeZoneOffsetAndName() {
    when(mMockCalendar.getTimeInMillis()).thenReturn(60000L);
    when(mMockTimeZone.getOffset(anyLong())).thenReturn(60000);
    when(mMockContext.getString(com.google.android.apps.wearable.settings.R.string.gmt))
        .thenReturn("GMT");
    when(mMockTimeZone.getDisplayName(anyBoolean(), eq(TimeZone.SHORT), eq(Locale.getDefault())))
        .thenReturn("fake/tz");

    assertEquals(
        "GMT+00:01 fake/tz",
        DateTimeSettingsHelper.getTimeZoneOffsetAndName(
            mMockTimeZone, mMockCalendar, mMockContext));
  }

  @Test
  public void testFormatOffset() {
    when(mMockContext.getString(com.google.android.apps.wearable.settings.R.string.gmt))
        .thenReturn("GMT");

    assertEquals("GMT+00:01", DateTimeSettingsHelper.formatOffset(60000L, mMockContext));
  }

  @Test
  public void testIsAltMode() {
    mFakeSettings.putIntValueForKey(
        SettingsContract.BLUETOOTH_MODE_URI,
        SettingsContract.KEY_BLUETOOTH_MODE,
        SettingsContract.BLUETOOTH_MODE_ALT);

    assertTrue(DateTimeSettingsHelper.isAltMode(mFakeSettings));
  }
}