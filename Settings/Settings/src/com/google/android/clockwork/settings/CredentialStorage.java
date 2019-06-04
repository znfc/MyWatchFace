package com.google.android.clockwork.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.security.Credentials;
import android.security.KeyChain;
import android.security.KeyChain.KeyChainConnection;
import android.security.KeyStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.widget.LockPatternUtils;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.keyguard.ChooseLockSettingsHelper;
import com.google.android.clockwork.settings.keyguard.LockSettingsActivity;

/**
 * Taken from apps/Settings/ Package
 *
 * CredentialStorage handles KeyStore reset, unlock, and install.
 *
 * CredentialStorage has a pretty convoluted state machine to migrate
 * from the old style separate keystore password to a new key guard
 * based password, as well as to deal with setting up the key guard if
 * necessary.
 *
 * KeyStore: UNINITALIZED
 * KeyGuard: OFF
 * Action:   set up key guard
 * Notes:    factory state
 *
 * KeyStore: UNINITALIZED
 * KeyGuard: ON
 * Action:   confirm key guard
 * Notes:    user had key guard but no keystore and upgraded from pre-ICS
 *           OR user had key guard and pre-ICS keystore password which was then reset
 *
 * KeyStore: LOCKED
 * Action:   ignore
 *
 * KeyStore: UNLOCKED
 * KeyGuard: OFF
 * Action:   set up key guard
 * Notes:    ensure key guard, then proceed
 *
 * KeyStore: UNLOCKED
 * keyguard: ON
 * Action:   normal unlock/install
 * Notes:    this is the common case
 */
public final class CredentialStorage extends Activity {

    private static final String TAG = "CredentialStorage";

    public static final String ACTION_UNLOCK = "com.android.credentials.UNLOCK";
    public static final String ACTION_INSTALL = "com.android.credentials.INSTALL";
    public static final String ACTION_RESET = "com.android.credentials.RESET";

    // This is the minimum acceptable password quality.  If the current password quality is
    // lower than this, keystore should not be activated.
    static final int MIN_PASSWORD_QUALITY = DevicePolicyManager.PASSWORD_QUALITY_SOMETHING;

    private static final int CONFIRM_KEY_GUARD_REQUEST = 1;

    private final KeyStore mKeyStore = KeyStore.getInstance();
    private LockPatternUtils mLockPatternUtils;

    /**
     * When non-null, the bundle containing credentials to install.
     */
    private Bundle mInstallBundle;

    /**
     * After unsuccessful KeyStore.unlock, the number of unlock
     * attempts remaining before the KeyStore will reset itself.
     *
     * Reset to -1 on successful unlock or reset.
     */
    private int mRetriesRemaining = -1;

    @Override
    protected void onResume() {
        super.onResume();
        mLockPatternUtils = new LockPatternUtils(this);
        Intent intent = getIntent();
        String action = intent.getAction();

        if (ACTION_RESET.equals(action)) {
            new ResetDialog();
        } else {
            if (ACTION_INSTALL.equals(action)
                    && "com.android.certinstaller".equals(getCallingPackage())) {
                mInstallBundle = intent.getExtras();
            }
            // ACTION_UNLOCK also handled here in addition to ACTION_INSTALL
            handleUnlockOrInstall();
        }
    }

    /**
     * Based on the current state of the KeyStore and key guard, try to
     * make progress on unlocking or installing to the keystore.
     */
    private void handleUnlockOrInstall() {
        // something already decided we are done, do not proceed
        if (isFinishing()) {
            return;
        }
        UserInfo parent = UserManager.get(this).getProfileParent(UserHandle.myUserId());
        int uid = parent != null ? parent.id : UserHandle.myUserId();
        switch (mKeyStore.state()) {
            case UNINITIALIZED: {
                ensureKeyGuard(uid);
                return;
            }
            case LOCKED:
                // This should not happen, we are not supporting passwords.
                return;
            case UNLOCKED: {
                if (!checkKeyGuardQuality(uid)) {
                    new ConfigureKeyGuardDialog();
                    return;
                }
                installIfAvailable();
                finish();
                return;
            }
        }
    }

    /**
     * Make sure the user enters the key guard to set or change the
     * keystore password. This can be used in UNINITIALIZED to set the
     * keystore password or UNLOCKED to change the password (as is the
     * case after unlocking with an old-style password).
     */
    private void ensureKeyGuard(int uid) {
        if (!checkKeyGuardQuality(uid)) {
            // key guard not setup, doing so will initialize keystore
            new ConfigureKeyGuardDialog();
            // will return to onResume after Activity
            return;
        }
        // force key guard confirmation
        if (confirmKeyGuard(uid)) {
            // will return password value via onActivityResult
            return;
        }
        finish();
    }

    /**
     * Returns true if the currently set key guard matches our minimum quality requirements.
     */
    private boolean checkKeyGuardQuality(int uid) {
        int quality = new LockPatternUtils(this).getActivePasswordQuality(uid);
        return (quality >= MIN_PASSWORD_QUALITY);
    }

    /**
     * Install credentials if available, otherwise do nothing.
     */
    private void installIfAvailable() {
        if (mInstallBundle != null && !mInstallBundle.isEmpty()) {
            Bundle bundle = mInstallBundle;
            mInstallBundle = null;

            final int uid = bundle.getInt(Credentials.EXTRA_INSTALL_AS_UID, -1);

            if (bundle.containsKey(Credentials.EXTRA_USER_PRIVATE_KEY_NAME)) {
                String key = bundle.getString(Credentials.EXTRA_USER_PRIVATE_KEY_NAME);
                byte[] value = bundle.getByteArray(Credentials.EXTRA_USER_PRIVATE_KEY_DATA);

                int flags = KeyStore.FLAG_ENCRYPTED;

                if (!mKeyStore.importKey(key, value, uid, flags)) {
                    Log.e(TAG, "Failed to install " + key + " as user " + uid);
                    return;
                }
            }

            int flags = KeyStore.FLAG_ENCRYPTED;

            if (bundle.containsKey(Credentials.EXTRA_USER_CERTIFICATE_NAME)) {
                String certName = bundle.getString(Credentials.EXTRA_USER_CERTIFICATE_NAME);
                byte[] certData = bundle.getByteArray(Credentials.EXTRA_USER_CERTIFICATE_DATA);

                if (!mKeyStore.put(certName, certData, uid, flags)) {
                    Log.e(TAG, "Failed to install " + certName + " as user " + uid);
                    return;
                }
            }

            if (bundle.containsKey(Credentials.EXTRA_CA_CERTIFICATES_NAME)) {
                String caListName = bundle.getString(Credentials.EXTRA_CA_CERTIFICATES_NAME);
                byte[] caListData = bundle.getByteArray(Credentials.EXTRA_CA_CERTIFICATES_DATA);

                if (!mKeyStore.put(caListName, caListData, uid, flags)) {
                    Log.e(TAG, "Failed to install " + caListName + " as user " + uid);
                    return;
                }
            }

            setResult(RESULT_OK);
        }
    }

    /**
     * Prompt for reset confirmation, resetting on confirmation, finishing otherwise.
     */
    private class ResetDialog
            implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
        private boolean mResetConfirmed;

        private ResetDialog() {
            AlertDialog dialog = new AlertDialog.Builder(CredentialStorage.this)
                    .setTitle(android.R.string.dialog_alert_title)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setMessage(R.string.credentials_reset_hint)
                    .setPositiveButton(android.R.string.ok, this)
                    .setNegativeButton(android.R.string.cancel, this)
                    .create();
            dialog.setOnDismissListener(this);
            dialog.show();
        }

        @Override public void onClick(DialogInterface dialog, int button) {
            mResetConfirmed = (button == DialogInterface.BUTTON_POSITIVE);
        }

        @Override public void onDismiss(DialogInterface dialog) {
            if (mResetConfirmed) {
                mResetConfirmed = false;
                new ResetKeyStoreAndKeyChain().execute();
                return;
            }
            finish();
        }
    }

    /**
     * Background task to handle reset of both keystore and user installed CAs.
     */
    private class ResetKeyStoreAndKeyChain extends AsyncTask<Void, Void, Boolean> {
        @Override protected Boolean doInBackground(Void... unused) {

            mKeyStore.reset();

            try {
                KeyChainConnection keyChainConnection = KeyChain.bind(CredentialStorage.this);
                try {
                    return keyChainConnection.getService().reset();
                } catch (RemoteException e) {
                    return false;
                } finally {
                    keyChainConnection.close();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        @Override protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(CredentialStorage.this,
                        R.string.credentials_erased, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(CredentialStorage.this,
                        R.string.credentials_not_erased, Toast.LENGTH_SHORT).show();
            }
            finish();
        }
    }

    /**
     * Prompt for key guard configuration confirmation.
     */
    private class ConfigureKeyGuardDialog
            implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener {
        private boolean mConfigureConfirmed;

        private ConfigureKeyGuardDialog() {
            AlertDialog dialog = new AlertDialog.Builder(CredentialStorage.this)
                    .setTitle(android.R.string.dialog_alert_title)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setMessage(R.string.credentials_configure_lock_screen_hint)
                    .setPositiveButton(android.R.string.ok, this)
                    .setNegativeButton(android.R.string.cancel, this)
                    .create();
            dialog.setOnDismissListener(this);
            dialog.show();
        }

        @Override public void onClick(DialogInterface dialog, int button) {
            mConfigureConfirmed = (button == DialogInterface.BUTTON_POSITIVE);
        }

        @Override public void onDismiss(DialogInterface dialog) {
            if (mConfigureConfirmed) {
                mConfigureConfirmed = false;
                startActivity(new Intent(CredentialStorage.this, LockSettingsActivity.class));
                return;
            }
            finish();
        }
    }

    /**
     * Confirm existing key guard, returning password via onActivityResult.
     */
    private boolean confirmKeyGuard(int uid) {
        return ChooseLockSettingsHelper.launchConfirmationActivity(this, CONFIRM_KEY_GUARD_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CONFIRM_KEY_GUARD_REQUEST) {
            // Receive key guard password initiated by confirmKeyGuard.
            if (resultCode == Activity.RESULT_OK) {
                String password = data.getStringExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD);
                if (!TextUtils.isEmpty(password)) {
                    // success
                    mKeyStore.unlock(password);
                    // return to onResume
                    return;
                }
            }
            // failed confirmation, bail
            finish();
        }
    }
}
