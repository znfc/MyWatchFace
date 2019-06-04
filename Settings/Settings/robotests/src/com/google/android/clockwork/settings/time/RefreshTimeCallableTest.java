package com.google.android.clockwork.settings.time;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.net.Network;

import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(ClockworkRobolectricTestRunner.class)
public class RefreshTimeCallableTest {

  private static final long POLLING_INTERVAL= 500;
  private static final int NTP_THRESHOLD_MS = 10;
  private static final long FAKE_CURRENT_TIME_MILLIS = 100L;

  @Mock private Clock mMockClock;
  @Mock private TrustedTimeProxy mMockTime;
  @Mock private Network mMockNetwork;

  private RefreshTimeCallable mRefreshTimeCallable;

  @Before
  public void setUp() {
    initMocks(this);
    mRefreshTimeCallable = new RefreshTimeCallable(
        mMockClock, mMockTime, mMockNetwork, POLLING_INTERVAL, NTP_THRESHOLD_MS);
  }

  @Test
  public void testRefreshTimeFreshCache() {
    // GIVEN the ntp cache is fresh
    when(mMockTime.getCacheAge()).thenReturn(POLLING_INTERVAL - 1);

    // WHEN time is synced
    mRefreshTimeCallable.call();

    // THEN the ntp cache is not refreshed
    verify(mMockTime, never()).forceRefresh(any(Network.class));
  }

  @Test
  public void testRefreshTimeOldCache() {
    // GIVEN the ntp cache is stale
    when(mMockTime.getCacheAge()).thenReturn(POLLING_INTERVAL);

    // WHEN time is synced
    mRefreshTimeCallable.call();

    // THEN the ntp cache is refreshed
    verify(mMockTime).forceRefresh(any(Network.class));
  }

  @Test
  public void testRefreshTimeNoDrift() {
    // GIVEN the ntp cache is fresh
    when(mMockTime.getCacheAge()).thenReturn(POLLING_INTERVAL - 1);
    // AND there is no clock drift
    when(mMockClock.currentTimeMillis()).thenReturn(FAKE_CURRENT_TIME_MILLIS);
    when(mMockTime.currentTimeMillis()).thenReturn(FAKE_CURRENT_TIME_MILLIS);

    // WHEN time is synced
    assertTrue(mRefreshTimeCallable.call());

    // THEN the clock is not set
    verify(mMockClock, never()).setCurrentTimeMillis(anyLong());
  }

  @Test
  public void testRefreshTimeHighDrift() {
    // GIVEN the ntp cache is fresh
    when(mMockTime.getCacheAge()).thenReturn(POLLING_INTERVAL - 1);
    // AND there is clock drift
    when(mMockClock.currentTimeMillis()).thenReturn(
            FAKE_CURRENT_TIME_MILLIS + NTP_THRESHOLD_MS + 1);
    when(mMockTime.currentTimeMillis()).thenReturn(FAKE_CURRENT_TIME_MILLIS);

    // WHEN time is updated
    assertTrue(mRefreshTimeCallable.call());

    // THEN the clock is set
    verify(mMockClock).setCurrentTimeMillis(FAKE_CURRENT_TIME_MILLIS);
  }

  @Test
  public void testRefreshTimeSucceeds() {
    // GIVEN the ntp cache is stale
    when(mMockTime.getCacheAge()).thenReturn(POLLING_INTERVAL);
    // AND time refresh succeeds
    when(mMockTime.forceRefresh(any(Network.class))).thenReturn(true);
    // AND there is clock drift
    when(mMockClock.currentTimeMillis()).thenReturn(
            FAKE_CURRENT_TIME_MILLIS + NTP_THRESHOLD_MS + 1);
    when(mMockTime.currentTimeMillis()).thenReturn(FAKE_CURRENT_TIME_MILLIS);

    // WHEN time is updated
    assertTrue(mRefreshTimeCallable.call());

    // THEN the clock is set
    verify(mMockClock).setCurrentTimeMillis(FAKE_CURRENT_TIME_MILLIS);
  }

  @Test
  public void testRefreshTimeFails() {
    // GIVEN the ntp cache is stale
    when(mMockTime.getCacheAge()).thenReturn(POLLING_INTERVAL);
    // AND time refresh fails
    when(mMockTime.forceRefresh(any(Network.class))).thenReturn(false);

    // WHEN time is updated
    assertFalse(mRefreshTimeCallable.call());

    // THEN the clock is not set
    verify(mMockClock, never()).setCurrentTimeMillis(anyLong());
  }
}