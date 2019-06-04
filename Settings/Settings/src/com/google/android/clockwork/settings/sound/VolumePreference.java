package com.google.android.clockwork.settings.sound;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.media.AudioManager;
import android.preference.Preference;
import android.util.AttributeSet;

import com.google.android.apps.wearable.settings.R;

public class VolumePreference extends Preference {
    private int mStream;

    public VolumePreference(
            Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    public VolumePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    public VolumePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0, 0);
    }

    public VolumePreference(Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        setPersistent(false);
        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.VolumePreference, defStyleAttr, defStyleRes);
        mStream = a.getInt(
                R.styleable.VolumePreference_stream, AudioManager.USE_DEFAULT_STREAM_TYPE);
        a.recycle();
    }

    @Override
    protected void onClick() {
        getContext().startActivity(new Intent(getContext(), VolumeActivity.class)
                .putExtra(VolumeActivity.EXTRA_STREAM, mStream)
                .putExtra(VolumeActivity.EXTRA_AUTO_DISMISSABLE, false));
    }

    public int getStream() {
        return mStream;
    }

    public void setStream(int stream) {
        mStream = stream;
    }
}
