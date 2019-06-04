package com.google.android.clockwork.settings.cellular;

import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.wearable.view.AcceptDenyDialogFragment;
import android.support.wearable.preference.WearablePreferenceActivity;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import com.google.android.clockwork.phone.Utils;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;
import com.google.android.clockwork.telephony.fragments.EnablePhoneAccountFragment;

import java.util.ArrayList;
import java.util.List;

import com.google.android.apps.wearable.settings.R;

public class PhoneAccountsActivity extends WearablePreferenceActivity
        implements AcceptDenyDialogFragment.OnDismissListener {
    private PhoneAccountsFragment mPhoneAccountsFragment;

    /** Tag for enable account dialog fragment. */
    private static final String ENABLE_ACCOUNT_DIALOG_TAG = "enableaccount";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startPreferenceFragment(mPhoneAccountsFragment = new PhoneAccountsFragment(), false);
    }

    @Override
    public void onDismiss(AcceptDenyDialogFragment fragment) {
        if (fragment instanceof EnablePhoneAccountFragment) {
            mPhoneAccountsFragment.updateAccountStatus(
                    ((EnablePhoneAccountFragment) fragment).getPhoneAccountHandle());
        }
    }

    public static class PhoneAccountsFragment extends SettingsPreferenceFragment {
        private TelecomManager mManager;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.prefs_phone_accounts);

            mManager = (TelecomManager) getContext().getSystemService(TELECOM_SERVICE);

            List<PhoneAccountHandle> accounts = new ArrayList<>(
                    Utils.getThirdPartyAccounts(mManager.getCallCapablePhoneAccounts(true)));
            for (PhoneAccountHandle handle : accounts) {
                final PhoneAccount account = mManager.getPhoneAccount(handle);

                final SwitchPreference pref = new SwitchPreference(getContext());
                pref.setPersistent(false);
                pref.setTitle(account.getLabel().toString());
                pref.setChecked(account.isEnabled());
                pref.setOnPreferenceChangeListener((p, newValue) -> {
                    if ((Boolean) newValue) {
                        FragmentTransaction transaction = getFragmentManager().beginTransaction();
                        transaction.addToBackStack(null);
                        EnablePhoneAccountFragment.newInstance(getContext(), account, null)
                                .show(transaction, ENABLE_ACCOUNT_DIALOG_TAG);
                        pref.setEnabled(false);
                    } else {
                        mManager.enablePhoneAccount(account.getAccountHandle(), false);
                        pref.setChecked(false);
                    }
                    return false;
                });
                getPreferenceScreen().addPreference(pref);
            }
        }

        public void updateAccountStatus(PhoneAccountHandle handle) {
            SwitchPreference p = (SwitchPreference) findPreference(handle.getId());
            if (p != null) {
                p.setChecked(mManager.getPhoneAccount(handle).isEnabled());
                p.setEnabled(true);
            }
        }
    }
}
