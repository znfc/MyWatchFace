package com.google.android.clockwork.settings.provider;

/**
 * Callback interface for IPC related to backup and restore of settings
 */
interface IBackupService {

    /** Get the backup data for the package */
    byte[] getBackupData(in String path) = 0;

    /** Restore the data to the given path */
    void restoreBackupData(in String path, in byte[] data) = 1;
}
