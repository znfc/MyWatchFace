package com.google.android.clockwork.settings.sound;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.os.UserManager;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.util.Log;
import android.util.SparseIntArray;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.google.android.apps.wearable.settings.R;

public class VolumeActivity extends Activity {
    private static final String TAG = "VolumeActivity";

    public static final String EXTRA_STREAM = "stream";
    public static final String EXTRA_AUTO_DISMISSABLE = "auto_dismissable";

    private static final int SAMPLE_CUTOFF = 2000;  // manually cap sample playback at 2 seconds

    // If the user doesn't change the volume, it should be automatically dismissed in 3 seconds.
    private static final int DELAY_FINISH_IN_MS = 3000;

    // STREAM_* starts from -1. See android.media.AudioSystem
    private static final int STREAM_UNKNOWN = -2;

    private static final int ON_ICONS = 0;
    private static final int OFF_ICONS = 1;

    private final VolumeHandler mHandler = new VolumeHandler();

    private int mStream;
    private boolean mStopped = true;
    private boolean mMuted;
    private boolean mZenMuted;
    private SeekBarVolumizer mVolumizer;
    private TextView mHeader;
    private SeekBar mSeekBar;
    private ImageView mIcon;
    private AudioManager mAudioManager;
    private boolean mAutoDimissable = true;

    private static SparseIntArray ICONS_MAP[] = new SparseIntArray[2];

    static {
        ICONS_MAP[ON_ICONS] = new SparseIntArray(5);
        ICONS_MAP[ON_ICONS].put(AudioManager.STREAM_VOICE_CALL, R.drawable.ic_cc_phone);
        ICONS_MAP[ON_ICONS].put(AudioManager.STREAM_MUSIC, R.drawable.ic_cc_note);
        ICONS_MAP[ON_ICONS].put(AudioManager.STREAM_ALARM, R.drawable.ic_cc_alarm);
        ICONS_MAP[ON_ICONS].put(AudioManager.STREAM_RING, R.drawable.ic_cc_ring);
        ICONS_MAP[ON_ICONS].put(AudioManager.STREAM_NOTIFICATION, R.drawable.ic_cc_ring);

        ICONS_MAP[OFF_ICONS] = new SparseIntArray(5);
        ICONS_MAP[OFF_ICONS].put(AudioManager.STREAM_VOICE_CALL, R.drawable.ic_cc_phone);
        ICONS_MAP[OFF_ICONS].put(AudioManager.STREAM_MUSIC, R.drawable.ic_cc_note_off);
        ICONS_MAP[OFF_ICONS].put(AudioManager.STREAM_ALARM, R.drawable.ic_cc_alarm_off);
        ICONS_MAP[OFF_ICONS].put(AudioManager.STREAM_RING, R.drawable.ic_cc_vibrate);
        ICONS_MAP[OFF_ICONS].put(AudioManager.STREAM_NOTIFICATION, R.drawable.ic_cc_vibrate);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        setContentView(R.layout.sound_volume_activity);
        mSeekBar = (SeekBar) findViewById(R.id.seekbar);
        mIcon = (ImageView) findViewById(R.id.icon);

        readFromIntent();

        // Set the header and action bar title appropriately, for accessibility.
        String headerString;
        TextView view = (TextView) findViewById(R.id.header);
        switch (mStream) {
            case AudioManager.STREAM_RING:
                headerString = getString(R.string.pref_ringVolume);
                break;
            case AudioManager.STREAM_MUSIC:
                headerString = getString(R.string.pref_mediaVolume);
                break;
            case AudioManager.STREAM_ALARM:
                headerString = getString(R.string.pref_alarmVolume);
                break;
            case AudioManager.STREAM_ACCESSIBILITY:
                headerString = getString(R.string.pref_accessibilityVolume);
                break;
            default:
                headerString = getString(R.string.volume_activity_header);
                break;
        }
        view.setText(headerString);
        setTitle(headerString);

        // Disable based on device owner policy.
        if (checkVolumeAdjustDisallowed()) {
            finish();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mStopped) {
            initVolumizer();
        }

        updateIcon(mAudioManager.getStreamVolume(mStream) == 0);

        if (mAutoDimissable) {
            scheduleDismiss();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        mStopped = true;
        if (mVolumizer != null) {
            mVolumizer.stop();
        }
    }

    @Override
    protected void onDestroy() {
        if (mHandler != null) {
            mHandler.removeMessages(VolumeHandler.FINISH_ACTIVITY);
        }
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_STEM_1:
                mAudioManager.adjustStreamVolume(mStream, AudioManager.ADJUST_RAISE, 0);
                return true;

            case KeyEvent.KEYCODE_STEM_2:
                mAudioManager.adjustStreamVolume(mStream, AudioManager.ADJUST_LOWER, 0);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void readFromIntent() {
        mAutoDimissable = getIntent().getBooleanExtra(EXTRA_AUTO_DISMISSABLE,
                true /* auto dismissable by default*/);
        mStream = getIntent().getIntExtra(EXTRA_STREAM, STREAM_UNKNOWN);
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "mStream: " + mStream);
        }
        if (mStream == STREAM_UNKNOWN) {
            throw new IllegalArgumentException("Stream type must be provided.");
        }
    }

    private void initVolumizer() {
        if (mSeekBar == null) {
            return;
        }

        final SeekBarVolumizer.Callback sbvc = new SeekBarVolumizer.Callback() {
            @Override
            public void onSampleStarting(SeekBarVolumizer sbv) {
                if (mVolumizer != null && mVolumizer != sbv) {
                    mVolumizer.stopSample();
                }
                mVolumizer = sbv;
                if (mVolumizer != null) {
                    mHandler.removeMessages(VolumeHandler.STOP_SAMPLE);
                    mHandler.sendEmptyMessageDelayed(VolumeHandler.STOP_SAMPLE, SAMPLE_CUTOFF);
                }
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
                updateIcon(progress == 0);
                if (mAutoDimissable) {
                    scheduleDismiss();
                }
            }

            @Override
            public void onMuted(boolean muted, boolean zenMuted) {
                if (mMuted == muted && mZenMuted == zenMuted) return;
                mMuted = muted;
                mZenMuted = zenMuted;
            }
        };
        final Uri sampleUri = mStream == AudioManager.STREAM_MUSIC ? getMediaVolumeUri() : null;
        if (mVolumizer == null) {
            mVolumizer = new SeekBarVolumizer(this, mStream, sampleUri, sbvc);
        }
        mVolumizer.start();
        mVolumizer.setSeekBar(mSeekBar);
    }

    // See com.android.settings.notification.VolumeSeekBarPreference
    private Uri getMediaVolumeUri() {
        return new Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(getPackageName()).path(Integer.toString(R.raw.media_volume)).build();
    }

    /**
     * Update the volume icon.
     * @param off True if the volume is 0 for the stream type.
     */
    private void updateIcon(boolean off) {
        mIcon.setImageResource(ICONS_MAP[off ? OFF_ICONS : ON_ICONS].get(mStream));
    }

    private void scheduleDismiss() {
        mHandler.removeMessages(VolumeHandler.FINISH_ACTIVITY);
        mHandler.sendEmptyMessageDelayed(VolumeHandler.FINISH_ACTIVITY, DELAY_FINISH_IN_MS);
    }

    private boolean checkVolumeAdjustDisallowed() {
        final EnforcedAdmin admin = RestrictedLockUtils.checkIfRestrictionEnforced(this,
                UserManager.DISALLOW_ADJUST_VOLUME, UserHandle.myUserId());
        if (admin != null) {
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(this, admin);
            return true;
        }
        return false;
    }

    private final class VolumeHandler extends Handler {
        private static final int STOP_SAMPLE = 1;
        private static final int FINISH_ACTIVITY = 2;

        private VolumeHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case STOP_SAMPLE:
                    mVolumizer.stopSample();
                    break;
                case FINISH_ACTIVITY:
                    finish();
                    break;
            }
        }
    }
}
