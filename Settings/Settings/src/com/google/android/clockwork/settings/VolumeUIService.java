package com.google.android.clockwork.settings;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.IVolumeController;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;

/**
 * Service to display a volume bar whenever the volume changes.
 */
public class VolumeUIService extends Service {

    private static final int MUTE_STREAMS_AFFECTED =
            (1 << AudioManager.STREAM_MUSIC) |
                    (1 << AudioManager.STREAM_RING) |
                    (1 << AudioManager.STREAM_NOTIFICATION) |
                    (1 << AudioManager.STREAM_SYSTEM) |
                    (1 << AudioManager.STREAM_ALARM);

    private Context mContext;
    private AudioManager mAudioManager;
    private VolumeController mVolumeController;
    private VolumeUI mVolumeUI;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mVolumeController = new VolumeController();
        mAudioManager.setVolumeController(mVolumeController);
        mVolumeUI = new VolumeUI(new VolumeUI.DefaultUI(mContext));

        // Make sure mute is permissible for streams that are affected by sound settings.
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.MUTE_STREAMS_AFFECTED, MUTE_STREAMS_AFFECTED);
        mAudioManager.reloadAudioSettings();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        mAudioManager.setVolumeController(null);
        super.onDestroy();
    }

    /**
     * Volume controller to listen to volume-related events and to handle them by displaying volume
     * UI with the help of {@link VolumeUI} class.
     *
     * The current implementation is minimalist, feel free to add more functionality on an
     * as-needed basis.
     */
    private class VolumeController extends IVolumeController.Stub {

        @Override
        public void displaySafeVolumeWarning(int flags) throws RemoteException {}

        @Override
        public void volumeChanged(int streamType, int flags) throws RemoteException {
            mVolumeUI.postVolumeChanged(streamType, flags);
        }

        @Override
        public void masterMuteChanged(int flags) throws RemoteException {}

        @Override
        public void setLayoutDirection(int layoutDirection) throws RemoteException {
            mVolumeUI.postSetLayoutDirection(layoutDirection);
        }

        @Override
        public void dismiss() throws RemoteException {
            // There's no need to dismiss an activity.
        }

        @Override
        public void setA11yMode(int mode) throws RemoteException {}
    }

}
