package com.google.android.clockwork.settings;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.IUsbManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ServiceManager;
import android.os.Vibrator;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.wearable.preference.WearablePreferenceActivity;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;

/**
 * Secure ADB Debugging confirmation, which appears when a user tries to connect an ADB debugger to
 * this Wear device from an unwhitelisted host computer.
 *
 * The clockwork framework overlay has been configured to send UsbDebuggingManager intents here.
 * See device/google/clockwork/overlay/frameworks/base/core/res/res/values/config.xml -
 * the config_customAdbPublicKeyConfirmationComponent value.
 */
public class SecureAdbActivity extends WearablePreferenceActivity {
    private static final String TAG = "SecureAdbActivity";

    private static final String KEY_PREF_SECURE_ADB_CANCEL = "pref_secureAdbCancel";
    private static final String KEY_PREF_SECURE_ADB_OK = "pref_secureAdbOk";
    private static final String KEY_PREF_SECURE_ADB_WHITELIST = "pref_secureAdbWhitelist";
    private static final String KEY_PREF_SECURE_ADB_FINGERPRINT = "pref_secureAdbFingerprint";

    // These two constants are hardcoded in UsbDebuggingManager. Do not change them here.
    private static final String EXTRA_KEY_FROM_ADBD = "key";
    private static final String EXTRA_FINGERPRINT_FROM_ADBD = "fingerprints";

    private String mPublicKey;
    private String mFingerprint;

    private boolean mReplied;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Window window = getWindow();
        window.addPrivateFlags(
                WindowManager.LayoutParams.PRIVATE_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS);
        window.setType(WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);

        super.onCreate(savedInstanceState);
        mPublicKey = getIntent().getStringExtra(EXTRA_KEY_FROM_ADBD);
        mFingerprint = getIntent().getStringExtra(EXTRA_FINGERPRINT_FROM_ADBD);

        if (savedInstanceState == null) {
            startPreferenceFragment(new SecureAdbFragment(), false);
        }
    }

    /**
     * This is called when adbd detects another connection attempt. This might mean the user
     * plugged the watch into a different computer, but it usually means the watch is bouncing
     * in a charging cradle, so the USB connection is going up and down.
     */
    @Override
    public void onNewIntent(Intent newIntent) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Activity onNewIntent");
        }
        String newPublicKey = newIntent.getStringExtra(EXTRA_KEY_FROM_ADBD);
        String newFingerprint = newIntent.getStringExtra(EXTRA_FINGERPRINT_FROM_ADBD);

        // Replace the intent, so the new token will persist even after changing orientation,
        // which triggers a fresh onCreate() and getIntent().
        setIntent(newIntent);

        // This could be a repeat, due to bouncing in a cradle, or other bounces.
        if (mFingerprint.equals(newFingerprint) && mPublicKey.equals(newPublicKey)) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Duplicate intent ignored");
            }
        } else {
            // This is a significantly different request.
            mPublicKey = newPublicKey;
            mFingerprint = newFingerprint;
            mReplied = false;
            vibrate();
        }
    }

    @Override
    public void onDestroy() {
        if (isFinishing()) {
            // Deny ADB access by default.
            replyToAdbd(KEY_PREF_SECURE_ADB_CANCEL);
        }
        super.onDestroy();
    }

    /**
     * Only the first reply is used. Subsequent replies are ignored.
     * @param response One of the KEY_PREF* codes.
     */
    private boolean replyToAdbd(String response) {
        if (!mReplied) {
            try {
                IUsbManager service = IUsbManager.Stub.asInterface(
                        ServiceManager.getService(USB_SERVICE));
                switch (response) {
                    case KEY_PREF_SECURE_ADB_CANCEL:
                        service.denyUsbDebugging();
                        Log.i(TAG, "denied USB debugging");
                        break;
                    case KEY_PREF_SECURE_ADB_OK:
                        service.allowUsbDebugging(false /* whitelist */, mPublicKey);
                        Log.i(TAG, "allowed USB debugging");
                        break;
                    case KEY_PREF_SECURE_ADB_WHITELIST:
                        service.allowUsbDebugging(true /* whitelist */, mPublicKey);
                        Log.i(TAG, "allowed USB debugging and whitelisted host");
                        break;
                    default:
                        Log.e(TAG, "Unhandled response: " + response);
                }
                mReplied = true;
            } catch (Exception e) {
                Log.e(TAG, "Unable to notify USB service", e);
            }
        }
        finish();
        return true;
    }

    private void vibrate() {
        ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(300);
    }

    public static class SecureAdbFragment extends SettingsPreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            final SecureAdbActivity activity = (SecureAdbActivity) getActivity();

            addPreferencesFromResource(R.xml.prefs_secure_adb);

            findPreference(KEY_PREF_SECURE_ADB_CANCEL).setOnPreferenceClickListener(
                    (p) -> activity.replyToAdbd(p.getKey()));
            findPreference(KEY_PREF_SECURE_ADB_OK).setOnPreferenceClickListener(
                    (p) -> activity.replyToAdbd(p.getKey()));
            findPreference(KEY_PREF_SECURE_ADB_WHITELIST).setOnPreferenceClickListener(
                    (p) -> activity.replyToAdbd(p.getKey()));
            findPreference(KEY_PREF_SECURE_ADB_FINGERPRINT).setOnPreferenceClickListener((p) -> {
                Toast.makeText(getContext(),
                        getContext().getString(R.string.secure_adb_fingerprint_text,
                                activity.mFingerprint),
                        Toast.LENGTH_LONG).show();
                return true;
            });
            activity.vibrate();
        }
    }
}
