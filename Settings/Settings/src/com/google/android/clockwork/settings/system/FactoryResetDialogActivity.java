package com.google.android.clockwork.settings.system;

import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.AcceptDenyDialog;
import android.view.WindowManager;

import android.widget.TextView;
import com.google.android.apps.wearable.settings.R;

public class FactoryResetDialogActivity extends WearableActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AcceptDenyDialog diag = new AcceptDenyDialog(this);
        diag.setTitle(getString(R.string.factory_reset_confirmation));
        diag.setMessage(getString(R.string.factory_reset_confirmation_subtitle));
        diag.setPositiveButton((dialog, which) -> {
            if (!ActivityManager.isUserAMonkey()) {
                ProgressDialog.show(this,
                        getString(R.string.pref_factoryReset),
                        getString(R.string.factory_resetting),
                        true, false, null);
                sendBroadcast(new Intent(Intent.ACTION_FACTORY_RESET)
                        .setPackage("android")
                        .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                        .putExtra(Intent.EXTRA_REASON, "FactoryResetDialogActivity")
                        .putExtra(Intent.EXTRA_WIPE_EXTERNAL_STORAGE,
                                true /* always clear storage. */));
            }
        });
        diag.setNegativeButton((dialog, which) -> finish());
        diag.setOnDismissListener((dialog) -> {
            if (!isFinishing()) {
                finish();
            }
        });
        ((TextView) diag.findViewById(android.R.id.title))
            .setTextAppearance(R.style.WearText_Subhead);
        ((TextView) diag.findViewById(android.R.id.message))
            .setTextAppearance(R.style.WearText_Body1);
        diag.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        diag.show();
    }
}
