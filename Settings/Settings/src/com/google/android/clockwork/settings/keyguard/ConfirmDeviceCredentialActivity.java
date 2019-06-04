package com.google.android.clockwork.settings.keyguard;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * This is a port of the {@link com.android.settings.ConfirmDeviceCredentialActivity} class to Wear.
 *
 * Launch this when you want to confirm the user is present by asking them to enter their
 * PIN/password/pattern.
 */
public class ConfirmDeviceCredentialActivity extends Activity {
    public static final String TAG = "ConfirmDeviceCreds";

    public static final String ACTION_CHECK_OFF_BODY_STATE =
        "com.google.android.clockwork.home.CHECK_OFF_BODY_STATE";
    public static final ComponentName CHECK_OFF_BODY_COMPONENT = new ComponentName(
            "com.google.android.wearable.app",
            "com.google.android.clockwork.home.CheckOffBodyService");

    private static final int LAUNCH_CONFIRMATION_ACTIVITY_REQUEST = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!ChooseLockSettingsHelper.launchConfirmationActivity(this,
            LAUNCH_CONFIRMATION_ACTIVITY_REQUEST)) {
            Log.d(TAG, "No pattern, password or PIN set.");
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if ((requestCode == LAUNCH_CONFIRMATION_ACTIVITY_REQUEST) &&
            (resultCode == Activity.RESULT_OK)) {
            setResult(Activity.RESULT_OK);

            // Check whether we're still on-body, and lock within a few seconds if not.
            startService(new Intent(ACTION_CHECK_OFF_BODY_STATE)
                .setComponent(CHECK_OFF_BODY_COMPONENT));
        }
        finish();
    }
}
