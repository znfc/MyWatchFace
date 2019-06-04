package com.google.android.clockwork.settings.connectivity.nfc;

import static com.google.common.base.Preconditions.checkNotNull;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.preference.Preference;
import android.preference.SwitchPreference;

/**
 * NfcEnabler is a helper to manage the Nfc on/off checkbox preference. It
 * turns on/off NFC and ensures the summary of the preference reflects the
 * current state.
 *
 * This has been taken verbatim from Android's NfcEnabler, with the Android Beam
 * related functionality removed.
 */
public class NfcEnabler implements Preference.OnPreferenceChangeListener {
    private final Context mContext;
    private final SwitchPreference mNfcSwitch;
    private final NfcAdapter mNfcAdapter;
    private final IntentFilter mIntentFilter;
    private final NfcStateListener mListener;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (NfcAdapter.ACTION_ADAPTER_STATE_CHANGED.equals(action)) {
                handleNfcStateChanged(intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE,
                        NfcAdapter.STATE_OFF));
            }
        }
    };

    public interface NfcStateListener {
        void onNfcOn();
        void onNfcOff();
    }

    public NfcEnabler(
            Context context,
            SwitchPreference switchPreference,
            NfcStateListener nfcStateListener) {
        checkNotNull(context);
        checkNotNull(switchPreference);
        checkNotNull(nfcStateListener);

        mContext = context;
        mNfcSwitch = switchPreference;
        mListener = nfcStateListener;
        mNfcAdapter = NfcAdapter.getDefaultAdapter(context);

        if (mNfcAdapter == null) {
            throw new UnsupportedOperationException("Can't create NfcEnabler: NFC not supported.");
        }
        mIntentFilter = new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
    }

    public void resume() {
        handleNfcStateChanged(mNfcAdapter.getAdapterState());
        mContext.registerReceiver(mReceiver, mIntentFilter);
        mNfcSwitch.setOnPreferenceChangeListener(this);
    }

    public void pause() {
        mContext.unregisterReceiver(mReceiver);
        mNfcSwitch.setOnPreferenceChangeListener(null);
    }

    public boolean onPreferenceChange(Preference preference, Object value) {
        // Turn NFC on/off

        final boolean desiredState = (Boolean) value;
        mNfcSwitch.setEnabled(false);

        if (desiredState) {
            mNfcAdapter.enable();
        } else {
            mNfcAdapter.disable();
        }

        return false;
    }

    public boolean isNfcEnabled() {
        return mNfcAdapter.getAdapterState() == mNfcAdapter.STATE_ON;
    }

    private void handleNfcStateChanged(int newState) {
        switch (newState) {
        case NfcAdapter.STATE_OFF:
            mNfcSwitch.setChecked(false);
            mNfcSwitch.setEnabled(true);
            break;
        case NfcAdapter.STATE_ON:
            mNfcSwitch.setChecked(true);
            mNfcSwitch.setEnabled(true);
            mListener.onNfcOn();
            break;
        case NfcAdapter.STATE_TURNING_ON:
            mNfcSwitch.setChecked(true);
            mNfcSwitch.setEnabled(false);
            break;
        case NfcAdapter.STATE_TURNING_OFF:
            mNfcSwitch.setChecked(false);
            mNfcSwitch.setEnabled(false);
            mListener.onNfcOff();
            break;
        }
    }
}
