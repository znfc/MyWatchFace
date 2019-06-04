package com.google.android.clockwork.settings.cellular;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.common.concurrent.AbstractCwRunnable;
import com.google.android.clockwork.common.concurrent.Executors;
import com.google.android.clockwork.phone.Utils;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;

/**
 * Cellular SIM Status
 *
 * Must be run in the phone process to access the correct phone data
 * structures.
 */
public class SimStatusFragment extends SettingsPreferenceFragment {

    private static final String KEY_PREF_SIM_STATUS_NETWORK = "pref_simStatusNetwork";
    private static final String KEY_PREF_SIM_STATUS_SIGNAL_STRENGTH
            = "pref_simStatusSignalStrength";
    private static final String KEY_PREF_SIM_STATUS_NETWORK_TYPE = "pref_simStatusNetworkType";
    private static final String KEY_PREF_SIM_STATUS_SERVICE_STATE = "pref_simStatusServiceState";
    private static final String KEY_PREF_SIM_STATUS_ROAMING_STATE = "pref_simStatusRoamingState";
    private static final String KEY_PREF_SIM_STATUS_NETWORK_STATE = "pref_simStatusNetworkState";
    private static final String KEY_PREF_SIM_STATUS_PHONE_NUMBER = "pref_simStatusPhoneNumber";
    private static final String KEY_PREF_SIM_STATUS_IMEI = "pref_simStatusImei";
    private static final String KEY_PREF_SIM_STATUS_IMEI_SV = "pref_simStatusImeiSv";
    private static final String KEY_PREF_SIM_STATUS_ICCID = "pref_simStatusIccid";

    // These correspond to ServiceState.STATE_*.
    private static final int SERVICE_STATE_STRINGS[] = new int[] {
        R.string.service_state_in_service,
        R.string.service_state_out_of_service,
        R.string.service_state_emergency_only,
        R.string.service_state_off
    };

    // These correspond to TelephonyManager#DATA_*.
    private static final int NETWORK_STATE_STRINGS[] = new int[] {
        R.string.network_state_disconnected,
        R.string.network_state_connecting,
        R.string.network_state_connected,
        R.string.network_state_suspended
    };

    private Phone mPhone;
    private TelephonyManager mTelephonyManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTelephonyManager
                = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        final int phoneId = SubscriptionManager.getPhoneId(
                SubscriptionManager.getDefaultSubscriptionId());
        mPhone = PhoneFactory.getPhone(phoneId);

        addPreferencesFromResource(R.xml.prefs_sim_status);

        initSimStatusPhoneNumber(findPreference(KEY_PREF_SIM_STATUS_PHONE_NUMBER));
        initSimStatusImei(findPreference(KEY_PREF_SIM_STATUS_IMEI));
        initSimStatusImeiSv(findPreference(KEY_PREF_SIM_STATUS_IMEI_SV));
        initSimStatusIccid(findPreference(KEY_PREF_SIM_STATUS_ICCID));
    }

    @Override
    public void onResume() {
        super.onResume();

        mTelephonyManager.listen(mPhoneStateListener,
                PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                | PhoneStateListener.LISTEN_SERVICE_STATE);
    }

    @Override
    public void onPause() {
        super.onPause();

        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
    }

    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            // Fall back on the data registration state, for data only devices,
            // or if the network isn't present (the voice registration state
            // can be wrong on data-only devices in that case)
            int serviceStateValue = serviceState.getState();
            if (serviceStateValue == ServiceState.STATE_OUT_OF_SERVICE
                    || serviceState.getOperatorNumeric() == null) {
                serviceStateValue = serviceState.getDataRegState();
            }

            findPreference(KEY_PREF_SIM_STATUS_NETWORK).setSummary(
                    serviceState.getOperatorAlphaLong());
            findPreference(KEY_PREF_SIM_STATUS_SERVICE_STATE).setSummary(
                    getString(SERVICE_STATE_STRINGS[serviceStateValue]));

            if (serviceState.getRoaming()) {
                findPreference(KEY_PREF_SIM_STATUS_ROAMING_STATE).setSummary(
                        R.string.sim_status_roaming);
            } else {
                findPreference(KEY_PREF_SIM_STATUS_ROAMING_STATE).setSummary(
                        R.string.sim_status_not_roaming);
                findPreference(KEY_PREF_SIM_STATUS_NETWORK_TYPE).setSummary(
                        getString(getNetworkTypeString(serviceState.getRilDataRadioTechnology())));
            }
        }

        @Override
        public void onDataConnectionStateChanged(int state, int networkType) {
            if (state < 0 || state >= NETWORK_STATE_STRINGS.length) {
                state = 0;
            }

            findPreference(KEY_PREF_SIM_STATUS_NETWORK_STATE).setSummary(
                    getString(NETWORK_STATE_STRINGS[state]));
        }

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            final int dbm = signalStrength.getDbm();
            final int asu = signalStrength.getAsuLevel();
            final String text = getString(R.string.sim_status_signal_strength, dbm, asu);

            findPreference(KEY_PREF_SIM_STATUS_SIGNAL_STRENGTH).setSummary(text);
        }
    };

    /**
     * @param radioTechType the data radio tech type of the service state
     * @return the string id for the network type.
     */
    private int getNetworkTypeString(int radioTechType) {
        switch (radioTechType) {
            case ServiceState.RIL_RADIO_TECHNOLOGY_GPRS:
                return R.string.network_type_gprs;
            case ServiceState.RIL_RADIO_TECHNOLOGY_EDGE:
                return R.string.network_type_edge;
            case ServiceState.RIL_RADIO_TECHNOLOGY_GSM:
                return R.string.network_type_gsm;
            case ServiceState.RIL_RADIO_TECHNOLOGY_IS95A:
            case ServiceState.RIL_RADIO_TECHNOLOGY_IS95B:
            case ServiceState.RIL_RADIO_TECHNOLOGY_1xRTT:
                return R.string.network_type_1xrtt;
            case ServiceState.RIL_RADIO_TECHNOLOGY_HSDPA:
                return R.string.network_type_hsdpa;
            case ServiceState.RIL_RADIO_TECHNOLOGY_HSUPA:
                return R.string.network_type_hsupa;
            case ServiceState.RIL_RADIO_TECHNOLOGY_HSPA:
                return R.string.network_type_hspa;
            case ServiceState.RIL_RADIO_TECHNOLOGY_HSPAP:
                return R.string.network_type_hspap;
            case ServiceState.RIL_RADIO_TECHNOLOGY_UMTS:
                return R.string.network_type_umts;
            case ServiceState.RIL_RADIO_TECHNOLOGY_TD_SCDMA:
                return R.string.network_type_td_scdma;
            case ServiceState.RIL_RADIO_TECHNOLOGY_EVDO_0:
                return R.string.network_type_evdo_0;
            case ServiceState.RIL_RADIO_TECHNOLOGY_EVDO_A:
                return R.string.network_type_evdo_a;
            case ServiceState.RIL_RADIO_TECHNOLOGY_EVDO_B:
                return R.string.network_type_evdo_b;
            case ServiceState.RIL_RADIO_TECHNOLOGY_EHRPD:
                return R.string.network_type_ehrpd;
            case ServiceState.RIL_RADIO_TECHNOLOGY_LTE_CA:
            case ServiceState.RIL_RADIO_TECHNOLOGY_LTE:
                return R.string.network_type_lte;
            case ServiceState.RIL_RADIO_TECHNOLOGY_IWLAN:
              // IWLAN doesn't fall into 2/3/4G category.
            case ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN:
                return R.string.network_type_unknown;
            default:
                return R.string.network_type_unknown;
        }
    }

    private void initSimStatusPhoneNumber(Preference pref) {
        if (mPhone == null) {
            return;
        }
        final String rawNumber = mPhone.getLine1Number();
        if (!TextUtils.isEmpty(rawNumber)) {
            pref.setSummary(rawNumber);
        }
        formatPhoneNumbers();
    }

    private void initSimStatusImei(Preference pref) {
        if (mPhone == null) {
            return;
        }
        final String imei = mPhone.getDeviceId();
        if (!TextUtils.isEmpty(imei)) {
            pref.setSummary(imei);
        }
    }

    private void initSimStatusImeiSv(Preference pref) {
        if (mPhone == null) {
            return;
        }
        final String softwareVersion = mPhone.getDeviceSvn();
        if (!TextUtils.isEmpty(softwareVersion)) {
            pref.setSummary(softwareVersion);
        }
    }

    private void initSimStatusIccid(Preference pref) {
        if (mPhone == null) {
            return;
        }
        final String iccid = mPhone.getIccSerialNumber();
        if (!TextUtils.isEmpty(iccid)) {
            pref.setSummary(iccid);
        }
    }

    // Formatting the phone numbers can hit the disk, so use executor
    // to perform the work on a background thread.
    private void formatPhoneNumbers() {
        Executors.INSTANCE.get(getContext()).getUserExecutor().submit(
                new AbstractCwRunnable("LoadPhoneNumber") {
            @Override
            public void run() {
                if (getActivity() == null || mPhone == null
                        || !getActivity().getResources().getBoolean(
                                R.bool.config_displayed_phone_number)) {
                    return;
                }

                final String formattedPhoneNumber = Utils.formatNumber(getContext(),
                        mPhone.getLine1Number());
                getActivity().runOnUiThread(() -> {
                    if (isDetached() || isRemoving()) {
                        return;
                    }
                    findPreference(KEY_PREF_SIM_STATUS_PHONE_NUMBER).setSummary(
                            formattedPhoneNumber);
                });
            }
        });
    }
}
