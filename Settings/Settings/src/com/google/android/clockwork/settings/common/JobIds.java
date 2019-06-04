package com.google.android.clockwork.settings.common;

/**
 * Central class for defining all {@link android.app.job.JobInfo#getId() job IDs} used with {@link
 * android.app.job.JobScheduler}. Job IDs are not namespaced, so need to be globally unique across
 * the app. By putting them in one class, we avoid collisions.
 *
 * <p>Persistent job IDS should NOT be changed/reused, since this can lead to collisions or
 * duplicates of a persisted job.
 */
public final class JobIds {
    private JobIds() {}

    public static final int JOB_ID_NETWORK_TIME_SYNC = 0;
}
