package com.google.android.clockwork.settings;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.wearable.view.WearableDialogActivity;
import android.util.Log;
import android.view.View;

import com.google.android.apps.wearable.settings.R;

public class CloudSyncOptInSettingsActivity extends WearableDialogActivity {
    private static final String TAG = "CloudSyncOptIn";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onCreate CloudSyncOptInSettingsActivity");
        }
        super.onCreate(savedInstanceState);

        View container = getWindow().getDecorView();
        if (container != null) {
            container.setKeepScreenOn(true);
        } else {
            Log.e(TAG, "Failed to find container view");
        }
    }

    @Override
    public void onResume() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onResume CloudSyncOptInSettingsActivity");
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onPause CloudSyncOptInSettingsActivity");
        }
        super.onPause();
    }

    private void sendAction(int type) {
        Intent intent = new Intent().setComponent(SettingsIntents.CLOUD_SYNC_OPT_IN_COMPONENT_NAME);
        intent.putExtra(SettingsIntents.EXTRA_CLOUD_SYNC_OPT_IN_TYPE, type);
        getApplicationContext().startService(intent);
        finish();
    }

    @Override
    public CharSequence getAlertTitle() {
        return getString(R.string.setting_cloudsync_title);
    }

    @Override
    public CharSequence getMessage() {
        return getString(R.string.setting_cloudsync_summary);
    }

    @Override
    public CharSequence getPositiveButtonText() {
        return getString(R.string.cloudsync_opt_in);
    }

    @Override
    public Drawable getPositiveButtonDrawable() {
        return getDrawable(R.drawable.action_accept);
    }

    @Override
    public CharSequence getNegativeButtonText() {
        return getString(R.string.cloudsync_opt_out);
    }

    @Override
    public Drawable getNegativeButtonDrawable() {
        return getDrawable(R.drawable.action_no_thanks);
    }

    @Override
    public void onPositiveButtonClick() {
        sendAction(SettingsIntents.CLOUD_SYNC_OPT_IN_TYPE_OPT_IN);
    }

    @Override
    public void onNegativeButtonClick() {
        sendAction(SettingsIntents.CLOUD_SYNC_OPT_IN_TYPE_OPT_OUT);
    }
}
