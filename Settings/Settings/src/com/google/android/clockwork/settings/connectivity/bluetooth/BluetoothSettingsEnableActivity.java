package com.google.android.clockwork.settings.connectivity.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.wearable.view.AcceptDenyDialog;
import com.google.android.clockwork.settings.connectivity.DefaultPermissionReviewModeUtils;
import com.google.android.clockwork.settings.connectivity.PermissionReviewModeUtils;

import com.google.android.apps.wearable.settings.R;

public class BluetoothSettingsEnableActivity extends Activity {
    private BluetoothAdapter mBluetoothAdapter;
    private BroadcastReceiver mBluetoothStateReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        PermissionReviewModeUtils permissionReviewModeUtils =
                DefaultPermissionReviewModeUtils.INSTANCE.get(this);

        String requestorPackage = getIntent().getStringExtra(Intent.EXTRA_PACKAGE_NAME);
        if (permissionReviewModeUtils.isPackageWhitelistedForOmittingCmiitDialog(
                requestorPackage)) {
            mBluetoothAdapter.enable();
            setResult(RESULT_OK);
            finish();
            return;
        }

        mBluetoothStateReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)
                        && intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                        != BluetoothAdapter.STATE_OFF) {
                    BluetoothSettingsEnableActivity.this.setResult(RESULT_OK);
                    finish();
                }
            }
        };

        registerReceiver(
                mBluetoothStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        CharSequence appLabel = permissionReviewModeUtils.getAppLabelFromPackage(requestorPackage);

        AcceptDenyDialog diag = new AcceptDenyDialog(this);
        diag.setTitle(
                permissionReviewModeUtils.getConsentDialogTitle(
                        appLabel, R.string.bluetooth_enable_request, R.string.bluetooth_disabled));
        diag.setPositiveButton((dialog, which) -> mBluetoothAdapter.enable());
        diag.setNegativeButton((dialog, which) -> {
            setResult(RESULT_CANCELED);
            finish();
        });
        diag.setOnCancelListener((dialog) -> {
            setResult(RESULT_CANCELED);
            finish();
        });
        diag.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetoothStateReceiver != null) {
            unregisterReceiver(mBluetoothStateReceiver);
            mBluetoothStateReceiver = null;
        }
    }
}
