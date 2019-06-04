package com.google.android.clockwork.settings.cellular;

import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.AcceptDenyDialog;
import android.view.View;

import com.google.android.apps.wearable.settings.R;

public class SimLockedDialog extends WearableActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AcceptDenyDialog diag = new AcceptDenyDialog(this);
        diag.setTitle(getString(R.string.sim_locked_dialog_title));
        diag.setMessage(getString(R.string.sim_locked_dialog_subtitle));
        diag.setPositiveButton((dialog, which) -> {
            setResult(RESULT_OK);
            startActivity(new Intent(this, SimUnlockActivity.class));
        });
        diag.setNegativeButton((dialog, which) -> setResult(RESULT_CANCELED));
        diag.setOnCancelListener((dialog) -> setResult(RESULT_CANCELED));
        diag.setOnDismissListener((dialog) -> finish());
        diag.show();
    }
}
