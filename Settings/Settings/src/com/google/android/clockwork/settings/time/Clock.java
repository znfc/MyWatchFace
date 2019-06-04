package com.google.android.clockwork.settings.time;

/**
 * An interface for getting the current time that ensures testability of clock-related methods that
 * would otherwise be difficult to test (e.g. System#currentTimeMillis).
 */
public interface Clock {

  /**
   * Returns the standard "wall" clock (time and date) expressing milliseconds since the epoch.
   *
   * Synonymous to System#currentTimeMillis
   */
  public long currentTimeMillis();

  /**
   * Sets the current wall time, in milliseconds. Requires the calling process to have appropriate
   * permissions.
   *
   * Synonymous to SystemClock#setCurrentTimeMillis
   */
  public boolean setCurrentTimeMillis(long millis);

  /**
   * Returns milliseconds since boot, including time spent in sleep.
   *
   * Synonymous to SystemClock#elapsedRealtime
   */
  public long elapsedRealtime();
}