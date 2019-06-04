package com.google.android.clockwork.settings.provider;

import android.app.backup.BackupAgent;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;
import android.util.Slog;

import com.google.android.clockwork.settings.SettingsContract;
import com.google.common.annotations.VisibleForTesting;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SettingsBackupAgent extends BackupAgent {

    static final String BACKUP_DEBUG_TAG = "ClockworkPhoenix";
    private static final long CONNECTION_TIMEOUT = TimeUnit.SECONDS.toMillis(10);

    static final String BACKUP_UPDATES_PATH = "backup_needs_update";
    private static final Uri BACKUP_UPDATES_URI = buildUriForSettingsPath(BACKUP_UPDATES_PATH);
    static final String BACKUP_UPDATES_KEY = "update";

    private boolean mConnected = false;
    private IBackupService mBackupService;
    private BackupConnection mBackupConnection;

    private Context mContext;

    public SettingsBackupAgent() {
        super();
    }

    @VisibleForTesting
    SettingsBackupAgent(Context context) {
        super();
        mContext = context;
    }

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
            ParcelFileDescriptor newState) throws IOException {

        if (mContext == null) {
            mContext = this;
        }

        if (Log.isLoggable(BACKUP_DEBUG_TAG, Log.DEBUG)) {
            Log.d(BACKUP_DEBUG_TAG, "onBackup called");
        }

        boolean shouldBackup = false;
        Cursor cursor = mContext.getContentResolver().query(BACKUP_UPDATES_URI, null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                shouldBackup = cursor
                        .getInt(cursor.getColumnIndex(SettingsContract.COLUMN_VALUE)) == 1;
            }
            cursor.close();
        }
        if (Log.isLoggable(BACKUP_DEBUG_TAG, Log.DEBUG)) {
            Log.d(BACKUP_DEBUG_TAG, "Provider has updates? " + shouldBackup);
        }
        if (shouldBackup) {
            if (Log.isLoggable(BACKUP_DEBUG_TAG, Log.DEBUG)) {
                Log.d(BACKUP_DEBUG_TAG, "Last backup is stale");
            }
            try {
                connectToBackupService();
            } catch (TimeoutException e) {
                Log.e(BACKUP_DEBUG_TAG, "Couldn't connect to backup service", e);
                return;
            }
            for (SettingProperties prop : new PropertiesMap(
                    mContext::getApplicationContext).values()) {
                if (Log.isLoggable(BACKUP_DEBUG_TAG, Log.DEBUG)) {
                    Log.d(BACKUP_DEBUG_TAG, "Backing up property " + prop.getPath());
                }
                byte[] backupData;
                try {
                    backupData = mBackupService.getBackupData(prop.getPath());
                } catch (RemoteException e) {
                    Log.w(BACKUP_DEBUG_TAG, "IPC error", e);
                    continue;
                }
                try {
                    data.writeEntityHeader(prop.getPath(), backupData.length);
                    data.writeEntityData(backupData, backupData.length);
                } catch (IOException e) {
                    Log.e(SettingsBackupAgent.BACKUP_DEBUG_TAG,
                            "Error writing backup data for " + prop.getPath(), e);
                }
            }

        } else {
            if (Log.isLoggable(BACKUP_DEBUG_TAG, Log.DEBUG)) {
                Log.d(BACKUP_DEBUG_TAG, "Don't need to backup");
            }
        }

        detachFromBackupService();
        mContext.sendBroadcast(new Intent(SettingsProvider.ACTION_BACKUP),
                SettingsProvider.PERMISSION_PROVIDER_BACKUP);
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState)
            throws IOException {

        if (mContext == null) {
            mContext = this;
        }

        if (Log.isLoggable(BACKUP_DEBUG_TAG, Log.DEBUG)) {
            Log.d(BACKUP_DEBUG_TAG, "onRestore called");
        }

        try {
            connectToBackupService();
        } catch (TimeoutException e) {
            Log.e(BACKUP_DEBUG_TAG, "Couldn't connect to backup service", e);
            return;
        }

        PropertiesMap propertiesMap = new PropertiesMap(mContext::getApplicationContext);

        while (data.readNextHeader()) {
            String key = data.getKey();
            int dataSize = data.getDataSize();

            SettingProperties property = propertiesMap.get(key);
            if (property != null) {
                if (Log.isLoggable(BACKUP_DEBUG_TAG, Log.DEBUG)) {
                    Log.d(BACKUP_DEBUG_TAG, "Found key: " + key + " with size " + dataSize);
                }

                byte[] dataBuf = new byte[dataSize];
                data.readEntityData(dataBuf, 0, dataSize);
                try {
                    mBackupService.restoreBackupData(property.getPath(), dataBuf);
                } catch (RemoteException e) {
                    Log.w(BACKUP_DEBUG_TAG, "IPC error", e);
                }

            } else {
                Log.w(BACKUP_DEBUG_TAG, "Unknown key: " + key);
            }
        }

        detachFromBackupService();
        mContext.sendBroadcast(new Intent(SettingsProvider.ACTION_RESTORE),
                SettingsProvider.PERMISSION_PROVIDER_BACKUP);
    }

    private static Uri buildUriForSettingsPath(String path) {
        return new Uri.Builder().scheme("content").authority(
                SettingsContract.SETTINGS_AUTHORITY).path(path).build();
    }

    private void connectToBackupService() throws TimeoutException {
        mBackupConnection = new BackupConnection();
        mConnected = false;
        mContext.bindService(new Intent().setComponent(
                new ComponentName(mContext.getPackageName(), BackupService.class.getName())),
                mBackupConnection, Context.BIND_AUTO_CREATE);
        long startTime = System.currentTimeMillis();
        while (!mConnected && (System.currentTimeMillis() - startTime < CONNECTION_TIMEOUT)) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Log.e(BACKUP_DEBUG_TAG, "Error sleeping", e);
            }
        }
        if (!mConnected || mBackupService == null) {
            throw new TimeoutException();
        }
    }

    private void detachFromBackupService() {
        if (mConnected) {
            try {
                mContext.unbindService(mBackupConnection);
            } catch (IllegalArgumentException e) {
                Slog.e(BACKUP_DEBUG_TAG, e.toString());
            }
            mConnected = false;
        }
    }

    private class BackupConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mConnected = true;
            try {
                mBackupService = (IBackupService) service;
            } catch (ClassCastException e1) {
                // Test binds with the local instance, actual code must IPC
                try {
                    mBackupService = IBackupService.Stub.asInterface(service);
                } catch (NullPointerException e2) {
                    // Backup Service not available
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBackupService = null;
            mConnected = false;
        }
    }
}
