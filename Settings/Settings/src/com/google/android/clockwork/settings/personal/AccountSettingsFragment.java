package com.google.android.clockwork.settings.personal;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AppGlobals;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;
import com.google.android.clockwork.settings.common.SettingsPreferenceLogConstants;
import com.google.android.clockwork.settings.connectivity.ConnectivityManagerWrapperImpl;
import com.google.android.clockwork.settings.enterprise.DevicePolicyManagerWrapper;
import com.google.android.clockwork.settings.enterprise.DevicePolicyManagerWrapperImpl;
import com.google.android.clockwork.settings.enterprise.EnterprisePrivacyFeatureProvider;
import com.google.android.clockwork.settings.enterprise.EnterprisePrivacyFeatureProviderImpl;

/**
 * Account settings.
 */
public class AccountSettingsFragment extends SettingsPreferenceFragment {

    private static final String KEY_ADD_ACCOUNT = "pref_addAccount";

    private static final String ENTERPRISE_PRIVACY_FRAGMENT =
            "com.google.android.clockwork.settings.enterprise.EnterprisePrivacySettingsFragment";

    /** The type used for Google accounts. */
    private static final String GOOGLE_ACCOUNT_TYPE = "com.google";

    private PreferenceScreen mPreferenceScreen;
    private Preference mEnterpriseDisclosurePref;
    private AccountManager mAccountManager;
    private EnterprisePrivacyFeatureProvider mFeatureProvider;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_account);

        Context context = getContext();
        mPreferenceScreen = getPreferenceScreen();
        mAccountManager = (AccountManager) getContext().getSystemService(Context.ACCOUNT_SERVICE);
        mFeatureProvider = new EnterprisePrivacyFeatureProviderImpl(context,
                new DevicePolicyManagerWrapperImpl(context
                        .getSystemService(DevicePolicyManager.class)),
                context.getPackageManager(),
                AppGlobals.getPackageManager(),
                context.getSystemService(UserManager.class),
                new ConnectivityManagerWrapperImpl(context
                        .getSystemService(ConnectivityManager.class)),
                context.getResources());
    }

    @Override
    public void onResume() {
        super.onResume();

        updateAccountList();
    }

    private void updateAccountList() {
        Preference addAccountPref = findPreference(KEY_ADD_ACCOUNT);
        final OnPreferenceClickListener clickListener =
                addAccountPref.getOnPreferenceClickListener();
        addAccountPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference pref) {
                if (!checkAddAccountDisallowed()) {
                    if (clickListener != null) {
                        return clickListener.onPreferenceClick(pref);
                    }
                    return false;
                }
                return true;
            }
        });

        mPreferenceScreen.removeAll();
        // In LE, we added an overlay to remove account settings, as it only shows Google accounts
        // which are unavailable in China. If we start to support third-party accounts, please
        // notify @ogaymond to add this setting back.
        Account[] accountList = mAccountManager.getAccountsByType(GOOGLE_ACCOUNT_TYPE);

        for (Account acct : accountList) {
            if (acct == null) {
                continue;
            }

            Preference pref = new Preference(getContext());
            // TODO(nzheng): use real name and use email as summary.
            pref.setTitle(acct.name);
            pref.setKey(SettingsPreferenceLogConstants.IGNORE_SUBSTRING + acct.name);
            // TODO(nzheng): use user avatar
            pref.setIcon(R.drawable.ic_cc_settings_account);
            pref.setFragment(AccountDetailsFragment.class.getName());
            Bundle fragmentArgs = pref.getExtras();
            fragmentArgs.putParcelable(AccountDetailsFragment.EXTRA_ACCOUNT, acct);
            mPreferenceScreen.addPreference(pref);
        }

        mPreferenceScreen.addPreference(addAccountPref);
        addEnterpriseDisclosure();
    }

    private boolean checkAddAccountDisallowed() {
        final EnforcedAdmin admin = RestrictedLockUtils.checkIfRestrictionEnforced(getActivity(),
                UserManager.DISALLOW_MODIFY_ACCOUNTS, UserHandle.myUserId());
        if (admin != null) {
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getActivity(), admin);
            return true;
        }
        return false;
    }

    private void addEnterpriseDisclosure() {
        final CharSequence disclosure = mFeatureProvider.getDeviceOwnerDisclosure();
        if (disclosure == null) {
            return;
        }
        if (mEnterpriseDisclosurePref == null) {
            mEnterpriseDisclosurePref = new Preference(getContext());
            mEnterpriseDisclosurePref.setIcon(R.drawable.ic_info_outline_white);
            mEnterpriseDisclosurePref.setFragment(ENTERPRISE_PRIVACY_FRAGMENT);
        }
        mEnterpriseDisclosurePref.setSummary(disclosure);
        mPreferenceScreen.addPreference(mEnterpriseDisclosurePref);
    }
}
