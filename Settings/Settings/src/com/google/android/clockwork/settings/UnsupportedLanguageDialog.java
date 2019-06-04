package com.google.android.clockwork.settings;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.wearable.view.WearableDialogActivity;
import android.util.Log;
import android.view.View;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.actions.WearableHostWithRpcCallback;
import com.google.android.clockwork.remoteintent.RemoteIntentConstants;

import java.util.concurrent.TimeUnit;

public class UnsupportedLanguageDialog extends WearableDialogActivity {
    private static final String TAG = "UnsupportedLangDlg";

    private static final String OPEN_ON_PHONE_REMOTE_CONFIRM_ACTION =
            "com.google.android.clockwork.home.OPEN_ON_PHONE_ACTION";

    private static final int ACTION_OPEN_ON_PHONE = 1;

    private static final long SCREEN_ON_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(15);
    private static final int MSG_SCREEN_ON_TIMEOUT = 1;

    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final View container = getWindow().getDecorView();
        if (container != null) {
            container.setKeepScreenOn(true);
        } else {
            Log.e(TAG, "Failed to find container view");
        }

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MSG_SCREEN_ON_TIMEOUT) {
                    Log.d(TAG, "No longer keeping screen on");
                    if (container != null) {
                        container.setKeepScreenOn(false);
                    }
                }
            }
        };
        mHandler.sendEmptyMessageDelayed(MSG_SCREEN_ON_TIMEOUT, SCREEN_ON_TIMEOUT_MS);
    }

    @Override
    protected void onDestroy() {
        mHandler.removeMessages(MSG_SCREEN_ON_TIMEOUT);
        super.onDestroy();
    }

    private void sendAction(int type) {
        Bundle rpcData = new Bundle();
        rpcData.putString(RemoteIntentConstants.KEY_ACTION, Settings.ACTION_LOCALE_SETTINGS);
        rpcData.putInt(RemoteIntentConstants.KEY_START_MODE,
                RemoteIntentConstants.START_MODE_ACTIVITY);
        Bundle options = new Bundle();
        options.putBoolean(RemoteIntentConstants.ACTIVITY_OPTIONS_WAKE_PHONE, true);
        rpcData.putBundle(RemoteIntentConstants.KEY_ACTIVITY_OPTIONS, options);
        Intent openOnPhoneIntent = new Intent(OPEN_ON_PHONE_REMOTE_CONFIRM_ACTION);
        openOnPhoneIntent.putExtra(WearableHostWithRpcCallback.KEY_FEATURE_TAG,
                        RemoteIntentConstants.FEATURE_TAG)
                .putExtra(WearableHostWithRpcCallback.KEY_RPC_PATH,
                        RemoteIntentConstants.PATH_RPC)
                .putExtra(WearableHostWithRpcCallback.KEY_RPC_DATA, rpcData)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityForResult(openOnPhoneIntent, 0 /* requestCode not used */);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        finish();
    }

    @Override
    public CharSequence getAlertTitle() {
        return getString(R.string.language_not_supported_title);
    }

    @Override
    public CharSequence getPositiveButtonText() {
        return getString(R.string.language_not_supported_open_on_phone);
    }

    @Override
    public Drawable getPositiveButtonDrawable() {
        return getDrawable(R.drawable.action_open_on_phone);
    }

    @Override
    public void onPositiveButtonClick() {
        sendAction(ACTION_OPEN_ON_PHONE);
    }
}
