package com.google.android.clockwork.settings.personal;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.preference.AcceptDenyDialogPreference;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;

/**
 * Shows account details and allows user to remove acct.
 */
public class AccountDetailsFragment extends SettingsPreferenceFragment {
    private static final String TAG = "AccountDetails";
    static final String EXTRA_ACCOUNT = "account";
    private static final String KEY_REMOVE_ACCOUNT = "pref_removeAccount";
    private static final int REMOVE_ACCOUNT_REQUEST = 1;
    private static final int REMOVE_ACCOUNT_TIMEOUT_SECS = 5;

    private AccountManager mAccountManager;
    private Toast mToast;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_account_details);

        mAccountManager = (AccountManager) getContext().getSystemService(Context.ACCOUNT_SERVICE);

        Bundle bundle = getArguments();
        Account account = bundle.getParcelable(EXTRA_ACCOUNT);

        // TODO(nzheng): add icon and user name to match UX.

        setupRemoveAccountPref(account);
    }

    private void setupRemoveAccountPref(final Account account) {
        AcceptDenyDialogPreference pref =
                (AcceptDenyDialogPreference) findPreference(KEY_REMOVE_ACCOUNT);
        pref.setDialogTitle(getString(R.string.remove_acct_conf, account.name));
        pref.setOnDialogClosedListener((positiveResult) -> {
            if (positiveResult) {
                mAccountManager.removeAccount(account, getActivity(), (future) -> {
                    try {
                        future.getResult(REMOVE_ACCOUNT_TIMEOUT_SECS, TimeUnit.SECONDS);
                        startActivity(new Intent(getContext(), ConfirmationActivity.class)
                                .putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                                        ConfirmationActivity.SUCCESS_ANIMATION));
                        getActivity().finish();
                    } catch (OperationCanceledException e) {
                        showToast(R.string.remove_account_failed);
                        Log.e(TAG, "Login failed because operation canceled.", e);
                    } catch (AuthenticatorException e) {
                        showToast(R.string.remove_account_failed);
                        Log.e(TAG,
                                "Login failed because authenticator failed: " + e.getMessage(), e);
                    } catch (IOException e) {
                        showToast(R.string.remove_account_failed);
                        Log.e(TAG, "Login failed because connection failed: " + e.getMessage(), e);
                    }
                }, null);
            }
        });
    }

    private void showToast(int resId) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(getContext(), resId, Toast.LENGTH_SHORT);
        mToast.show();
    }
}
