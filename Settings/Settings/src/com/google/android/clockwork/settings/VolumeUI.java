package com.google.android.clockwork.settings;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.util.Log;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.common.LogUtils;
import com.google.android.clockwork.settings.sound.VolumeActivity;
import com.google.common.base.Preconditions;

/**
 * Minimalist system volume UI for clockwork.
 *
 * Displays volume bar when current stream's volume changes.
 */
public class VolumeUI extends Handler {
    private static final String TAG = LogUtils.getSafeTag(VolumeUI.class);

    private static final int MSG_VOLUME_CHANGED = 1;
    private static final int MSG_SET_LAYOUT_DIRECTION = 2;

    interface Ui {
        void showVolumeActivityForStream(int streamType);
    }

    private final Ui mUi;

    public VolumeUI(Ui ui) {
        super(Looper.getMainLooper());
        mUi = Preconditions.checkNotNull(ui);
    }

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case MSG_VOLUME_CHANGED:
                boolean showUi = message.arg2 != 0;
                if (showUi) {
                    mUi.showVolumeActivityForStream(message.arg1);
                }
                break;
            case MSG_SET_LAYOUT_DIRECTION:
                // TODO: Ask VolumeActivity to setLayoutDirection.
                break;
        }
    }

    /**
     * Post volume changed message to the handler.
     */
    public void postVolumeChanged(int streamType, int flags) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "postVolumeChanged() streamType: " + streamType + ", flags: " + flags);
        }

        // MediaControlsPage uses a customized volume dialog that is different from
        // VolumeUI. Disable 'absolute volume' so that users won't see two volume
        // dialogs. See b/26031368 for details.
        if ((flags & AudioManager.FLAG_BLUETOOTH_ABS_VOLUME) != 0) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Ignore Bluetooth absolute volume.");
            }
            return;
        }

        int showUi = flags & AudioManager.FLAG_SHOW_UI;
        if (showUi != 0) {
            Message message = obtainMessage(MSG_VOLUME_CHANGED);
            message.arg1 = streamType;
            message.arg2 = showUi;
            message.sendToTarget();
        }
    }

    /**
     * Post set layout direction message to the handler.
     */
    public void postSetLayoutDirection(int layoutDirection) {
        Message message = obtainMessage(MSG_SET_LAYOUT_DIRECTION);
        message.arg1 = layoutDirection;
        message.sendToTarget();
    }

    static final class DefaultUI implements Ui {
        private Context mContext;

        public DefaultUI(Context context) {
            mContext = context;
        }

        @Override
        public void showVolumeActivityForStream(int streamType) {
            Intent intent = new Intent(mContext, VolumeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(VolumeActivity.EXTRA_STREAM, streamType);
            mContext.startActivity(intent);
        }
    }
}
