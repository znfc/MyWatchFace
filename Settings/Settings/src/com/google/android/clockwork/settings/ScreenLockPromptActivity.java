package com.google.android.clockwork.settings;

import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.AcceptDenyDialog;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.keyguard.LockSettingsActivity;

public class ScreenLockPromptActivity extends WearableActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AcceptDenyDialog diag = new AcceptDenyDialog(this);
        diag.setTitle(getString(R.string.screen_lock_prompt_title));
        diag.setMessage(getString(R.string.screen_lock_prompt_subtitle));
        diag.setPositiveButton((dialog, which) ->
            startActivity(new Intent(getApplicationContext(), LockSettingsActivity.class)));
        diag.setNegativeButton((dialog, which) -> { /* do nothing */ });
        diag.setOnDismissListener((dialog) -> finish());
        diag.show();
    }
}
