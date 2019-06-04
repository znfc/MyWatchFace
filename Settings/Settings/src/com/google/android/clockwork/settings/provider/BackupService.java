package com.google.android.clockwork.settings.provider;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;


public class BackupService extends Service {

    private ServiceCallbacks mCallbacks;
    private PropertiesMap mPropertiesMap;

    @Override
    public IBinder onBind(Intent intent) {
        return mCallbacks;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mCallbacks = new ServiceCallbacks();
        mPropertiesMap = new PropertiesMap(BackupService.this::getApplicationContext);
    }

    class ServiceCallbacks extends IBackupService.Stub {
        @Override
        public byte[] getBackupData(String path) throws RemoteException {
            if (mPropertiesMap.containsKey(path)) {
                return mPropertiesMap.get(path).getBackupData();
            } else {
                Log.w(SettingsBackupAgent.BACKUP_DEBUG_TAG,
                        "Unable to find backup property " + path);
                return new byte[0];
            }
        }

        @Override
        public void restoreBackupData(String path, byte[] data) throws RemoteException {
            if (mPropertiesMap.containsKey(path)) {
                SettingProperties property = mPropertiesMap.get(path);
                property.restore(data);
            } else {
                Log.w(SettingsBackupAgent.BACKUP_DEBUG_TAG,
                        "Unable to find restore property " + path);
            }
        }
    }
}
