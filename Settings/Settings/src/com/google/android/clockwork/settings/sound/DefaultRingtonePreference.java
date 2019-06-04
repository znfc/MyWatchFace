package com.google.android.clockwork.settings.sound;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.preference.RingtonePreference;
import android.util.AttributeSet;
import android.widget.Toast;

import com.google.android.apps.wearable.settings.R;;
import com.google.android.clockwork.common.concurrent.Executors;
import com.google.android.clockwork.common.concurrent.WrappedCwRunnable;

public class DefaultRingtonePreference extends RingtonePreference {
    private Handler mHandler;
    private Context mContext;
    private AudioManager mAudioManager;

    public DefaultRingtonePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mHandler = new Handler(Looper.getMainLooper());
        mContext = context;
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    protected void onClick() {
        super.onClick();
        if(mAudioManager.getStreamVolume(AudioManager.STREAM_RING) == 0) {
            Toast.makeText(mContext, R.string.ringtone_no_volume_warning, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onSaveRingtone(final Uri ringtoneUri) {
        RingtoneManager.setActualDefaultRingtoneUri(getContext(), getRingtoneType(), ringtoneUri);
        Executors.INSTANCE.get(getContext()).getBackgroundExecutor().submit(
                new WrappedCwRunnable("RingtonePrefUpdate", () -> updateSummary(ringtoneUri)));
    }

    @Override
    protected Uri onRestoreRingtone() {
        return RingtoneManager.getActualDefaultRingtoneUri(getContext(), getRingtoneType());
    }

    @Override
    protected void onAttachedToActivity() {
        super.onAttachedToActivity();
        Executors.INSTANCE.get(getContext()).getBackgroundExecutor().submit(
                new WrappedCwRunnable("RingtonePrefInit",
                        () -> updateSummary(onRestoreRingtone())));
    }

    /**
     * Updates the summary based on the ringtone Uri given. Takes a non trivial amount of time, do
     * not call from UI thread.
     * @param ringtoneUri the Uri to look up the Ringtone with.
     */
    protected void updateSummary(Uri ringtoneUri) {
        final String name = ringtoneUri == null
                ? getContext().getString(com.android.internal.R.string.ringtone_silent)
                : RingtoneManager.getRingtone(getContext(), ringtoneUri).getTitle(getContext());
        mHandler.post(() -> setSummary(name));
    }
}
