package com.google.android.clockwork.settings.cellular;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.telecom.TelecomManager;
import android.telephony.CarrierConfigManager;
import android.telephony.ims.feature.ImsFeature;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.ims.ImsConfig;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;

/** Preference for toggling options for advanced cellular options of the device. */
public class CellularAdvancedFragment extends SettingsPreferenceFragment {
    private static final String TAG = "CellularAdvancedFrag";

    private static final String KEY_PREF_ENHANCED_4G_LTE_KEY = "pref_enhanced_4g_lte";
    private static final String KEY_PREF_PREFERRED_NETWORK = "pref_preferredNetwork";
    private static final String KEY_PREF_ACCESS_POINT_NAMES = "pref_accessPointNames";
    private static final String KEY_PREF_NETWORK_OPERATORS = "pref_networkOperators";
    private static final String KEY_PREF_TWINNING = "pref_twinning";
    private static final String KEY_PREF_ACCOUNTS = "pref_accounts";

    private PersistableBundle mCarrierConfig;
    private SubscriptionManager mSubscriptionManager;
    private int mSubId;
    private TelephonyManager mTelephonyManager;
    private ImsManager mImsMgr;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final CarrierConfigManager ccm = (CarrierConfigManager) getContext().getSystemService(
                Context.CARRIER_CONFIG_SERVICE);
        mCarrierConfig = ccm.getConfig();

        mSubscriptionManager = SubscriptionManager.from(getContext());
        mSubId = SubscriptionManager.getDefaultSubscriptionId();
        mTelephonyManager = new TelephonyManager(getContext(), mSubId);
        mImsMgr = ImsManager.getInstance(getContext(), SubscriptionManager.getPhoneId(mSubId));

        addPreferencesFromResource(R.xml.prefs_cellular_advanced);

        initEnhanced4GLteSwitch((SwitchPreference) findPreference(KEY_PREF_ENHANCED_4G_LTE_KEY));
        initPreferredNetworkSwitch((Preference) findPreference(KEY_PREF_PREFERRED_NETWORK));
        initNetworkOperators((Preference) findPreference(KEY_PREF_NETWORK_OPERATORS));
        initTwinning((Preference) findPreference(KEY_PREF_TWINNING));
        initAccounts((Preference) findPreference(KEY_PREF_ACCOUNTS));
    }

    public void onDestroy() {
        final Context context = getContext();
        if (context != null && mNetworkQueryServiceConnected) {
            context.unbindService(mNetworkQueryServiceConnection);
            mNetworkQueryServiceConnected = false;
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onDestroy(): Unbinding network query service callback");
            }
        }
        super.onDestroy();
    }

    protected void initEnhanced4GLteSwitch(final SwitchPreference pref) {
        if (mCarrierConfig == null
                || mImsMgr == null
                || !mImsMgr.isVolteEnabledByPlatform()
                || !mImsMgr.isVolteProvisionedOnDevice()
                || !isImsServiceStateReady(mImsMgr)
                || mCarrierConfig.getBoolean(CarrierConfigManager.KEY_HIDE_ENHANCED_4G_LTE_BOOL)) {
            getPreferenceScreen().removePreference(pref);
        } else {
            pref.setEnabled(is4gLtePrefEnabled(mCarrierConfig) && hasActiveSubscriptions());
            boolean enhanced4gLteMode = mImsMgr.isEnhanced4gLteModeSettingEnabledByUser()
                    && mImsMgr.isNonTtyOrTtyOnVolteEnabled();
            pref.setChecked(enhanced4gLteMode);
        }

        pref.setOnPreferenceChangeListener((p, newVal) -> {
            boolean enhanced4gMode = (Boolean) newVal;
            pref.setChecked(enhanced4gMode);
            mImsMgr.setEnhanced4gLteModeSetting(pref.isChecked());
            return true;
        });
    }

    protected void initPreferredNetworkSwitch(final Preference pref) {
        if (mCarrierConfig == null) {
            return;
        }

        if (mCarrierConfig.getBoolean(
                CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL)
                || mCarrierConfig.getBoolean(
                CarrierConfigManager.KEY_HIDE_PREFERRED_NETWORK_TYPE_BOOL)) {
            getPreferenceScreen().removePreference(pref);
        }
    }

    private boolean mNetworkQueryServiceConnected;

    /** Service connection */
    private final ServiceConnection mNetworkQueryServiceConnection = new ServiceConnection() {
        /** Handle the task of binding the local object to the service */
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            debugLog("onServiceConnected() connection created, binding service.");
            mNetworkQueryServiceConnected = true;
            final NetworkOperatorsPreference pref = (NetworkOperatorsPreference)
                    findPreference(KEY_PREF_NETWORK_OPERATORS);
            pref.startNetworkOperatorQuery(service);
        }

        /** Handle the task of cleaning up the local binding */
        @Override
        public void onServiceDisconnected(ComponentName className) {
            debugLog("onServiceDisconnected() connection disconnected, cleaning local binding.");
            mNetworkQueryServiceConnected = false;
        }
    };

    protected void initNetworkOperators(final Preference pref) {
        if (!shouldShowNetworkOperators()) {
            getPreferenceScreen().removePreference(pref);
        } else {
            // Bind to the Network Query service
            final Intent intent = new Intent().setComponent(
                    NetworkOperatorsPreference.NETWORK_QUERY_SERVICE_COMPONENT);
            getContext().bindService(intent, mNetworkQueryServiceConnection,
                    Context.BIND_AUTO_CREATE);
        }
    }

    protected void initTwinning(final Preference pref) {
        if (!shouldShowTwinning()) {
            getPreferenceScreen().removePreference(pref);
        }
    }

    protected void initAccounts(final Preference pref) {
        if (!shouldShowAccounts()) {
            getPreferenceScreen().removePreference(pref);
        }
    }

    private boolean shouldShowAccounts() {
        return !com.google.android.clockwork.phone.Utils.getThirdPartyAccounts(
                ((TelecomManager) getContext().getSystemService(Context.TELECOM_SERVICE))
                .getCallCapablePhoneAccounts(true)).isEmpty();
    }

    private boolean shouldShowNetworkOperators() {
        if (mCarrierConfig == null) {
            debugLog("carrierConfig is null");
            return false;
        }

        if (!mCarrierConfig.getBoolean(CarrierConfigManager.KEY_OPERATOR_SELECTION_EXPAND_BOOL)) {
            debugLog("KEY_OPERATOR_SELECTION_EXPAND_BOOL is false");
            return false;
        } else if (mCarrierConfig.getBoolean(CarrierConfigManager.KEY_CSP_ENABLED_BOOL)
                && !PhoneFactory.getDefaultPhone().isCspPlmnEnabled()) {
            debugLog("isCspPlmnEnabled() false");
            return false;
        } else if (PhoneFactory.getDefaultPhone().getPhoneType() != PhoneConstants.PHONE_TYPE_GSM) {
            debugLog("Not a GSM phone");
            return false;
        }
        return true;
    }

    private boolean shouldShowTwinning() {
        //  Device must support the hardware Telephony feature and can show the setting.
        return getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
                && getContext().getResources().getBoolean(R.bool.config_showTwinningSettings);
    }

    private boolean isImsServiceStateReady(ImsManager imsMgr) {
        boolean isImsServiceStateReady = false;

        try {
            if (imsMgr != null && imsMgr.getImsServiceState() == ImsFeature.STATE_READY) {
                isImsServiceStateReady = true;
            }
        } catch (ImsException ex) {
            Log.e(TAG, "Exception when trying to get ImsServiceStatus: " + ex);
        }

        Log.d(TAG, "isImsServiceStateReady=" + isImsServiceStateReady);
        return isImsServiceStateReady;
    }

    private boolean is4gLtePrefEnabled(PersistableBundle carrierConfig) {
        return (mTelephonyManager.getCallState(mSubId)
                == TelephonyManager.CALL_STATE_IDLE)
                && mImsMgr != null
                && mImsMgr.isNonTtyOrTtyOnVolteEnabled()
                && carrierConfig.getBoolean(
                        CarrierConfigManager.KEY_EDITABLE_ENHANCED_4G_LTE_BOOL);
    }

    private boolean hasActiveSubscriptions() {
            return mSubscriptionManager.getActiveSubscriptionInfo(mSubId) != null;
    }

    private void debugLog(String string) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, string);
        }
    }
}
