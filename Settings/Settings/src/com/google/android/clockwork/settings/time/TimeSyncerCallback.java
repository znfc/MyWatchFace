package com.google.android.clockwork.settings.time;

/**
 * A callback interface for time syncing successes and failures
 */
public interface TimeSyncerCallback {
    /** Called when time is successfully synced from a time source */
    void onSuccess();

    /** Called after time syncing fails and all retries have been exhausted */
    void onFailure();
}
