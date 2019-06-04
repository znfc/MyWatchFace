package com.google.android.clockwork.settings.provider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.content.ServiceConnection;
import android.net.Uri;
import android.util.Log;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.OngoingStubbing;
import org.robolectric.annotation.Config;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowContentResolver;

@RunWith(ClockworkRobolectricTestRunner.class)
@Config(sdk = 27)
public class SettingsBackupAgentTest {

    private static final byte[] BACKUP_DATA = {0, 1};

    private SettingsBackupAgent mBackupAgent;
    private ContentResolver mContentResolver;
    private Intent mLastBroadcast;
    @Mock
    private BackupDataOutput mBackupDataOutput;
    @Mock
    private BackupDataInput mBackupDataInput;
    @Mock
    private BackupService.ServiceCallbacks mBackupService;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        SettingsProvider settingsProvider = new SettingsProvider();
        ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.authority = SettingsContract.SETTINGS_AUTHORITY;
        settingsProvider.attachInfoForTesting(RuntimeEnvironment.application, providerInfo);

        mContentResolver = RuntimeEnvironment.application.getContentResolver();
        ShadowContentResolver.registerProviderInternal(SettingsContract.SETTINGS_AUTHORITY,
                settingsProvider);
        mBackupAgent = new SettingsBackupAgent(
                new ContextWrapper(RuntimeEnvironment.application) {
                    @Override
                    public ContentResolver getContentResolver() {
                        return mContentResolver;
                    }

                    @Override
                    public void sendBroadcast(Intent intent, String permission) {
                        mLastBroadcast = intent;
                    }

                    @Override
                    public Context getApplicationContext() {
                        return getContext();
                    }

                    @Override
                    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
                        conn.onServiceConnected(new ComponentName("", ""), mBackupService);
                        return true;
                    }

                    @Override
                    public String getPackageName() {
                        return "com.google.android.apps.wearable.settings";
                    }
                });
        when(mBackupService.getBackupData(anyString())).thenReturn(BACKUP_DATA);
    }

    private Context getContext() {
        return RuntimeEnvironment.application;
    }

    @Test
    public void testBackup_noBackup() throws Exception {
        // GIVEN we have no new backup Data

        // WHEN we have a backup pass
        mBackupAgent.onBackup(null, mBackupDataOutput, null);

        // THEN dont save any data
        verify(mBackupDataOutput, never()).writeEntityData(any(byte[].class), anyInt());
        verify(mBackupDataOutput, never()).writeEntityHeader(anyString(), anyInt());
    }

    @Test
    public void testBackup_shouldBackup() throws Exception {
        // GIVEN we have backup Data
        Uri uriTilt = new Uri.Builder()
                .scheme("content")
                .authority(SettingsContract.SETTINGS_AUTHORITY)
                .path(SettingsContract.AMBIENT_CONFIG_PATH)
                .build();
        ContentValues cv = new ContentValues();
        cv.put(SettingsContract.KEY_AMBIENT_TILT_TO_WAKE, false);
        mContentResolver.update(uriTilt, cv, null, null);

        // WHEN we have a backup pass
        mBackupAgent.onBackup(null, mBackupDataOutput, null);

        // THEN save all data and broadcast backup
        PropertiesMap map = new PropertiesMap(this::getContext);
        for (SettingProperties properties : map.toArray()) {
            verify(mBackupDataOutput).writeEntityHeader(properties.getPath(), BACKUP_DATA.length);
            verify(mBackupService).getBackupData(properties.getPath());
        }
        verify(mBackupDataOutput, times(map.size())).writeEntityData(BACKUP_DATA,
                BACKUP_DATA.length);
        Assert.assertEquals("com.google.android.clockwork.settings.ACTION_BACKUP",
                mLastBroadcast.getAction());
    }

    @Test
    public void testRestore() throws Exception {
        // GIVEN we have restore Data
        final SettingProperties[] map = new PropertiesMap(this::getContext).toArray();
        when(mBackupDataInput.readNextHeader()).thenAnswer(new Answer() {
            private int count = 0;

            public Object answer(InvocationOnMock invocation) {
                return count++ != map.length;
            }
        });
        when(mBackupDataInput.getKey()).thenAnswer(new Answer() {
            private int count = 0;

            public Object answer(InvocationOnMock invocation) {
                return map[count++].getPath();
            }
        });
        when(mBackupDataInput.getDataSize()).thenReturn(3);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                byte[] data = (byte[]) args[0];
                for (int i = 0; i < 3; i++) {
                    data[i] = 5;
                }
                return 1;
            }
        }).when(mBackupDataInput).readEntityData(new byte[3], 0, 3);

        // WHEN we have a restore pass
        mBackupAgent.onRestore(mBackupDataInput, 0, null);

        // THEN restore all data
        for (SettingProperties prop : map) {
            verify(mBackupService).restoreBackupData(prop.getPath(), new byte[]{5, 5, 5});
        }
        Assert.assertEquals("com.google.android.clockwork.settings.ACTION_RESTORE",
                mLastBroadcast.getAction());
    }
}
