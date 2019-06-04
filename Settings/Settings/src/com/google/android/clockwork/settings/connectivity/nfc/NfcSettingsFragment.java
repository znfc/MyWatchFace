package com.google.android.clockwork.settings.connectivity.nfc;

import android.content.Context;
import android.content.ContentResolver;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.nfc.cardemulation.CardEmulation;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.Log;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;

import java.util.List;

/**
 * NFC Settings screen, allowing the user to change NFC related settings, such as turning on/off NFC
 * or selecting which Tap and Pay app to use.
 */
public class NfcSettingsFragment extends SettingsPreferenceFragment {
    private static final String TAG = "NfcSettingsFragment";

    private static final String KEY_PREF_NFC = "pref_nfc";
    private static final String KEY_PREF_TAP_AND_PAY = "pref_tap_and_pay";

    private NfcEnabler mNfcEnabler;
    private PaymentBackend mPaymentBackend;
    private TapAndPayPreference mTapAndPayPreference;
    private SwitchPreference mNfcPreference;

    /**
     * Listens to any changes of which Tap and Pay app to use, so we can update the Tap and Pay
     * preference's subtitle.
     */
    private PaymentBackend.Listener mPaymentAppChangedListener = new PaymentBackend.Listener() {
        @Override
        public void onDefaultAppChanged() {
            refreshCurrentTapAndPayApp();
        }
    };

    /**
     * Listens to whether NFC is being turned on or off, so we can update the TapAndPay preference.
     */
    private NfcEnabler.NfcStateListener mNfcStateListener = new NfcEnabler.NfcStateListener() {
        @Override
        public void onNfcOn() {
            if (shouldShowTapAndPayPreference()) {
                getPreferenceScreen().addPreference(mTapAndPayPreference);
                refreshCurrentTapAndPayApp();
            } else {
                getPreferenceScreen().removePreference(mTapAndPayPreference);
            }
        }

        @Override
        public void onNfcOff() {
            getPreferenceScreen().removePreference(mTapAndPayPreference);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.prefs_nfc);

        mNfcPreference = (SwitchPreference)findPreference(KEY_PREF_NFC);
        mTapAndPayPreference = (TapAndPayPreference)(findPreference(KEY_PREF_TAP_AND_PAY));

        initNfc();
        initTapAndPay(getContext());
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mNfcEnabler != null) {
            mNfcEnabler.resume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mNfcEnabler != null) {
            mNfcEnabler.pause();
        }
    }

    /**
     * Initializes the NFC enabler, which manages the NFC enabled/disabled switch.
     */
    void initNfc() {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        mNfcEnabler = new NfcEnabler(
            getActivity(),
            mNfcPreference,
            mNfcStateListener);
    }

    /**
     * Initialises the Tap and Pay preference, including the PaymentBackend which is used to display
     * the currently selected Tap and Pay app as a subtitle.
     */
    void initTapAndPay(Context context) {
        PackageManager packageManager = context.getPackageManager();
        ContentResolver contentResolver = context.getContentResolver();
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(context);
        CardEmulation cardEmuManager = CardEmulation.getInstance(nfcAdapter);
        PaymentBackend.CardEmuProvider cardEmuProvider = new PaymentBackend.DefaultCardEmuProvider(cardEmuManager);
        mPaymentBackend = new PaymentBackend(packageManager, contentResolver, cardEmuProvider);
        mPaymentBackend.addListener(mPaymentAppChangedListener);

        // Share the PaymentBackend with the TapAndPay preference, so that if the user changes the
        // app to use, we can update the TapAndPay preference's subtitle.
        mTapAndPayPreference.setPaymentBackend(mPaymentBackend);

        if (shouldShowTapAndPayPreference()) {
            refreshCurrentTapAndPayApp();
        } else {
            getPreferenceScreen().removePreference(mTapAndPayPreference);
        }
    }

    boolean shouldShowTapAndPayPreference() {
        return mNfcEnabler != null && mNfcEnabler.isNfcEnabled() &&
                mPaymentBackend != null && mPaymentBackend.getPaymentAppInfos().size() > 0;
    }

    /**
     * Refreshes the subtitle of the Tap and Pay preference, which displays the currently selected
     * Tap and Pay app.
     */
    void refreshCurrentTapAndPayApp() {
        CharSequence currentTapAndPayApp = mPaymentBackend.getDefaultPaymentCaption();

        if (currentTapAndPayApp == null) {
            return;
        }

        mTapAndPayPreference.setSummary(currentTapAndPayApp);
    }
}
