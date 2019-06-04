package com.google.android.clockwork.settings.connectivity.wifi;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.wearable.view.AcceptDenyDialog;
import android.text.TextUtils;
import com.google.android.clockwork.settings.connectivity.DefaultPermissionReviewModeUtils;
import com.google.android.clockwork.settings.connectivity.PermissionReviewModeUtils;

import com.google.android.apps.wearable.settings.R;

public class WifiSettingsEnableActivity extends Activity {
    private WifiManager mWifiManager;
    private BroadcastReceiver mWifiStateReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWifiManager = getSystemService(WifiManager.class);
        PermissionReviewModeUtils permissionReviewModeUtils =
                DefaultPermissionReviewModeUtils.INSTANCE.get(this);

        String requestorPackage = getIntent().getStringExtra(Intent.EXTRA_PACKAGE_NAME);
        if (permissionReviewModeUtils.isPackageWhitelistedForOmittingCmiitDialog(
                requestorPackage)) {
            mWifiManager.setWifiEnabled(true);
            setResult(RESULT_OK);
            finish();
        }

        mWifiStateReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)
                        && intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.ERROR)
                        != WifiManager.WIFI_STATE_DISABLED) {
                    WifiSettingsEnableActivity.this.setResult(RESULT_OK);
                    finish();
                }
            }
        };

        registerReceiver(
                mWifiStateReceiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));

        CharSequence appLabel =
                permissionReviewModeUtils.getAppLabelFromPackage(
                        getIntent().getStringExtra(Intent.EXTRA_PACKAGE_NAME));

        if (TextUtils.isEmpty(appLabel)) {
            // EXTRA_PACKAGE_NAME is mandatory in this case, so fail the request if missing or
            // cannot be resolved.
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        AcceptDenyDialog diag = new AcceptDenyDialog(this);
        diag.setTitle(
                permissionReviewModeUtils.getConsentDialogTitle(
                        appLabel, R.string.wifi_enable_request, 0));
        diag.setPositiveButton((dialog, which) -> mWifiManager.setWifiEnabled(true));
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
        if (mWifiStateReceiver != null) {
            unregisterReceiver(mWifiStateReceiver);
            mWifiStateReceiver = null;
        }
    }
}
