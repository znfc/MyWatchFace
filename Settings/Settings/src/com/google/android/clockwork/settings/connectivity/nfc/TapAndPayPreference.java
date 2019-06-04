package com.google.android.clockwork.settings.connectivity.nfc;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import com.android.internal.content.PackageMonitor;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.connectivity.nfc.PaymentBackend.PaymentAppInfo;

/**
 * Preference screen with a list of the apps the user can choose as their preferred Tap and Pay app.
 */
public class TapAndPayPreference extends ListPreference {
    private static final String TAG = "TapAndPayPreference";

    private PaymentBackend mPaymentBackend;

    private final PackageMonitor mSettingsPackageMonitor = new SettingsPackageMonitor();

    private final Handler mHandler = new Handler() {
        @Override
        public void dispatchMessage(Message msg) {
            refreshTapAndPayAppOptions();
        }
    };

    public TapAndPayPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TapAndPayPreference(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        setDialogTitle(R.string.pref_nfc_tap_and_pay_pref_screen_title);
        setIcon(R.drawable.ic_cc_settings_tap_and_pay);
        setPersistent(false);

        setOnPreferenceChangeListener((p, newVal) -> {
            if (mPaymentBackend != null) {
                ComponentName newComponent = ComponentName.unflattenFromString(newVal.toString());
                mPaymentBackend.setDefaultPaymentApp(newComponent);
            }
            return true;
        });
    }

    /**
     * Sets the PaymentBackend to use for actually setting the user's choice of app in the system.
     * This is shared with the NfcSettingsFragment so that when the setting is updated then the
     * fragment can update its label displaying the current setting.
     */
    public void setPaymentBackend(PaymentBackend newPaymentBackend) {
        mPaymentBackend = newPaymentBackend;

        refreshTapAndPayAppOptions();
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        // Hide the cancel button.
        builder.setNegativeButton(null, null);
        Context context = getContext();

        // Register for package changes.
        mSettingsPackageMonitor.register(context, context.getMainLooper(), false);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        // Unregister the package change monitor we registered in onPrepareDialogBuilder.
        mSettingsPackageMonitor.unregister();
    }

    /**
     * Refreshes the list of available options for Tap and Pay apps, using the PaymentBackend.
     */
    private void refreshTapAndPayAppOptions() {
        if (mPaymentBackend == null) {
            return;
        }

        String currentApp = null;
        List<CharSequence> entries = new ArrayList<>();
        List<CharSequence> entryValues = new ArrayList<>();
        List<PaymentAppInfo> appInfos = mPaymentBackend.getPaymentAppInfos();

        // Go through the PaymentAppInfo list we got from the PaymentBackend and create list entries
        // for all the items.
        if (appInfos != null && appInfos.size() > 0) {
            for (PaymentAppInfo appInfo : appInfos) {
                entries.add(appInfo.caption);
                String flatComponentName = appInfo.componentName.flattenToString();
                entryValues.add(flatComponentName);
                if (appInfo.isDefault) {
                    currentApp = flatComponentName;
                }
            }
        }

        // Once we have entries and entry values for all the apps, actually add them to the list.
        setEntries(entries.toArray(new String[entries.size()]));
        setEntryValues(entryValues.toArray(new String[entryValues.size()]));

        // And then set the current value.
        if (currentApp != null) {
            setValue(currentApp.toString());
        }
    }

    /**
     * Internal package monitor to listen for package changes and refresh the list of available
     * Tap and Pay apps.
     */
    private class SettingsPackageMonitor extends PackageMonitor {
        @Override
        public void onPackageAdded(String packageName, int uid) {
           mHandler.obtainMessage().sendToTarget();
        }

        @Override
        public void onPackageAppeared(String packageName, int reason) {
            mHandler.obtainMessage().sendToTarget();
        }

        @Override
        public void onPackageDisappeared(String packageName, int reason) {
            mHandler.obtainMessage().sendToTarget();
        }

        @Override
        public void onPackageRemoved(String packageName, int uid) {
            mHandler.obtainMessage().sendToTarget();
        }
    }
}
