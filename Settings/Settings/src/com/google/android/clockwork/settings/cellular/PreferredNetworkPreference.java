package com.google.android.clockwork.settings.cellular;

import static android.os.UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.PersistableBundle;
import android.os.StrictMode;
import android.os.SystemProperties;
import android.os.UserManager;
import android.preference.ListPreference;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.settingslib.RestrictedLockUtils;
import com.google.android.apps.wearable.settings.R;
import java.util.Arrays;
import java.util.List;

/**
 * This preference must run in the phone process
 */
public class PreferredNetworkPreference extends ListPreference {
    private static final String TAG = "PreferredNetworkPref";

    private static boolean DEBUG = false;  // STOPSHIP if true

    private Phone mPhone;
    private boolean mShow4GForLTE;
    private boolean mIsLteOnCdma;
    private boolean mIsGlobalCdma;
    private int mEntryChoicesResId;
    private int mEntryValuesResId;
    private String[] mEntryChoices;
    private String[] mEntryValues;

    private String mCurrentPreferredNetworkName;
    private int mCurrentPreferredNetworkId;
    private String mDefaultNetwork;
    private PersistableBundle mCarrierConfig;

    // TODO(shijianli): Delete this after the key is in CarrierConfigManager.
    private static final String KEY_SUPPORT_TD_SCDMA = "support_td_scdma";

    private static final String DEFAULT_NETWORK_PROPERTY = "ro.telephony.default_network";
    private static final String DEFAULT_NETWORK_TYPE = "9"; // Ril.h::RIL_PreferredNetworkType

    private static final String DEBUG_DEFAULT_NETWORK_PROPERTY = "debug.telephony.default_network";
    private static final String DEBUG_DEFAULT_NETWORK_TYPE = "0"; // Ril.h::RIL_PreferredNetworkType

    public PreferredNetworkPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PreferredNetworkPreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        mPhone = PhoneFactory.getDefaultPhone();
        CarrierConfigManager ccm = (CarrierConfigManager) getContext().getSystemService(
                Context.CARRIER_CONFIG_SERVICE);
        mCarrierConfig = ccm.getConfig();

        setTitle(R.string.preferred_network_type_action);
        setIcon(R.drawable.ic_cc_settings_cellular_4);
        setPersistent(false);
        setDialogTitle(R.string.preferred_network_type_action);

        selectCurrentNetworkType();
        selectChoicesAndValues();

        mEntryChoices = getContext().getResources().getStringArray(mEntryChoicesResId);
        mEntryValues = getContext().getResources().getStringArray(mEntryValuesResId);

        setEntries(mEntryChoices);
        setEntryValues(mEntryValues);

        setValue(Integer.toString(mCurrentPreferredNetworkId));
        setPreferredNetworkName();

        setOnPreferenceChangeListener((p, newVal) -> {
            if (((UserManager) getContext().getSystemService(Context.USER_SERVICE))
                    .hasUserRestriction(DISALLOW_CONFIG_MOBILE_NETWORKS)) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(
                    getContext(), RestrictedLockUtils.getDeviceOwner(getContext()));
                return false;
            }
            mCurrentPreferredNetworkId = Integer.parseInt((String) newVal);
            setPreferredNetworkName();
            StrictMode.ThreadPolicy oldPolicy = StrictMode.allowThreadDiskReads();
            debugLog("new settingsNetworkMode: " + mCurrentPreferredNetworkId);
            Settings.Global.putInt(getContext().getContentResolver(),
                    Settings.Global.PREFERRED_NETWORK_MODE + mPhone.getSubId(),
                    mCurrentPreferredNetworkId);
            mPhone.setPreferredNetworkType(mCurrentPreferredNetworkId, null);
            StrictMode.setThreadPolicy(oldPolicy);
            return true;
        });
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        // Hide the cancel button.
        builder.setNegativeButton(null, null);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
    }

    private void setPreferredNetworkName() {
        for (int i = 0; i < mEntryValues.length; ++i) {
            if (Integer.parseInt(mEntryValues[i]) == mCurrentPreferredNetworkId) {
                mCurrentPreferredNetworkName = mEntryChoices[i];
                break;
            }
        }
        setSummary(mCurrentPreferredNetworkName);
    }

    private void selectCurrentNetworkType() {
        if (DEBUG) { // Debug
            mDefaultNetwork = SystemProperties.get(DEBUG_DEFAULT_NETWORK_PROPERTY,
                    DEBUG_DEFAULT_NETWORK_TYPE);
            mCurrentPreferredNetworkId = Integer.parseInt(mDefaultNetwork);
        } else {
            mDefaultNetwork = SystemProperties.get(DEFAULT_NETWORK_PROPERTY,
                    DEFAULT_NETWORK_TYPE);
            mCurrentPreferredNetworkId = Settings.Global.getInt(getContext().getContentResolver(),
                    Settings.Global.PREFERRED_NETWORK_MODE + mPhone.getSubId(),
                    Integer.parseInt(mDefaultNetwork));
        }
        debugLog("SettingsNetworkMode: " + mCurrentPreferredNetworkId);
    }

    /**
     * Parse carrier config manager for settings that affect our
     * preferred network operations.
     */
    private void selectChoicesAndValues() {
        if (mCarrierConfig == null) {
            Log.e(TAG, "Unable to get carrier config!");
            return;
        }

        try {
            getContext().createPackageContext("com.android.systemui", 0);
            int id = getContext().getResources().getIdentifier("config_show4GForLTE",
                    "bool", "com.android.systemui");
            mShow4GForLTE = getContext().getResources().getBoolean(id);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "NameNotFoundException for show4GForLTE");
            mShow4GForLTE = true;
        }

        mIsLteOnCdma = mPhone.getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE;
        mIsGlobalCdma = mIsLteOnCdma
                && mCarrierConfig.getBoolean(CarrierConfigManager.KEY_SHOW_CDMA_CHOICES_BOOL);
        if (mCarrierConfig.getBoolean(CarrierConfigManager.KEY_WORLD_PHONE_BOOL)) {
            mEntryChoicesResId = R.array.preferred_network_mode_choices;
            mEntryValuesResId = R.array.preferred_network_mode_values;
        } else {
            final int phoneType = mPhone.getPhoneType();
            debugLog("phoneType: " + phoneType);
            if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                updateCdmaPhone();
            } else if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
                updateGsmPhone();
            } else {
                throw new IllegalStateException("Unexpected phone type: " + phoneType);
            }
        }
    }

    private boolean isWorldMode() {
        boolean worldModeOn = false;
        final TelephonyManager tm = (TelephonyManager) getContext().getSystemService(
                Context.TELEPHONY_SERVICE);
        final String configString = getContext().getResources().getString(
                R.string.config_world_mode);

        if (!TextUtils.isEmpty(configString)) {
            String[] configArray = configString.split(";");
            // Check if we have World mode configuration set to True only or config is set to True
            // and SIM GID value is also set and matches to the current SIM GID.
            if (configArray != null
                    && ((configArray.length == 1 && configArray[0].equalsIgnoreCase("true"))
                    || (configArray.length == 2 && !TextUtils.isEmpty(configArray[1])
                    && tm != null && configArray[1].equalsIgnoreCase(tm.getGroupIdLevel1())))) {
                               worldModeOn = true;
            }
        }

        debugLog("isWorldMode: " + worldModeOn);
        if (tm.getNetworkOperator() != null) {
            debugLog("getNetworkOperator(): " + tm.getNetworkOperator());
        }
        return worldModeOn;
    }

    private void updateCdmaPhone() {
        final int lteForced = Settings.Global.getInt(getContext().getContentResolver(),
                Settings.Global.LTE_SERVICE_FORCED + mPhone.getSubId(), 0);
        if (mIsLteOnCdma) {
            if (lteForced == 0) {
                mEntryChoicesResId = R.array.enabled_networks_cdma_choices;
                mEntryValuesResId = R.array.enabled_networks_cdma_values;
            } else {
                switch (mCurrentPreferredNetworkId) {
                    case Phone.NT_MODE_CDMA:
                    case Phone.NT_MODE_CDMA_NO_EVDO:
                    case Phone.NT_MODE_EVDO_NO_CDMA:
                        mEntryChoicesResId = R.array.enabled_networks_cdma_no_lte_choices;
                        mEntryValuesResId = R.array.enabled_networks_cdma_no_lte_values;
                        break;
                    case Phone.NT_MODE_GLOBAL:
                    case Phone.NT_MODE_LTE_CDMA_AND_EVDO:
                    case Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                    case Phone.NT_MODE_LTE_ONLY:
                        mEntryChoicesResId = R.array.enabled_networks_cdma_only_lte_choices;
                        mEntryValuesResId = R.array.enabled_networks_cdma_only_lte_values;
                        break;
                    default:
                        mEntryChoicesResId = R.array.enabled_networks_cdma_choices;
                        mEntryValuesResId = R.array.enabled_networks_cdma_values;
                        break;
                }
            }
        }
    }

    private void updateGsmPhone() {
        if (mCarrierConfig.getBoolean(KEY_SUPPORT_TD_SCDMA)) {
            mEntryChoicesResId = mShow4GForLTE
                    ? R.array.enabled_networks_4g_tdscdma_wcdma_gsm_choices
                    : R.array.enabled_networks_lte_tdscdma_wcdma_gsm_choices;
            mEntryValuesResId = R.array.enabled_networks_lte_tdscdma_wcdma_gsm_values;
        } else if (!mCarrierConfig.getBoolean(CarrierConfigManager.KEY_PREFER_2G_BOOL)
                && !getContext().getResources().getBoolean(R.bool.config_enabled_lte)) {
            mEntryChoicesResId = R.array.enabled_networks_wcdma_choices;
            mEntryValuesResId = R.array.enabled_networks_wcdma_values;
        } else if (!mCarrierConfig.getBoolean(CarrierConfigManager.KEY_PREFER_2G_BOOL)) {
            mEntryChoicesResId = mShow4GForLTE ? R.array.enabled_networks_4g_wcdma_choices
                    : R.array.enabled_networks_lte_wcdma_choices;
            mEntryValuesResId = R.array.enabled_networks_lte_wcdma_default9_values;
        } else if (!getContext().getResources().getBoolean(R.bool.config_enabled_lte)) {
            mEntryChoicesResId = R.array.enabled_networks_wcdma_gsm_choices;
            mEntryValuesResId = R.array.enabled_networks_wcdma_gsm_values;
        } else if (mIsGlobalCdma) {
            mEntryChoicesResId = R.array.enabled_networks_cdma_choices;
            mEntryValuesResId = R.array.enabled_networks_cdma_values;
        } else if (isWorldMode()) {
            mEntryChoicesResId = R.array.preferred_network_mode_choices_world_mode;
            mEntryValuesResId = R.array.preferred_network_mode_values_world_mode;
        } else {
            String[] defaultPreferredNetworkLookup = getContext().getResources().getStringArray(
                    R.array.default_preferred_network_lookup);
            List defaultPreferredNetworkList = Arrays.asList(defaultPreferredNetworkLookup);

            final int idx = defaultPreferredNetworkList.indexOf(mDefaultNetwork);
            if (idx == -1) {
                Log.w(TAG, "Unknown entry for default network:" + mDefaultNetwork);
                mEntryChoicesResId = mShow4GForLTE ? R.array.enabled_networks_4g_wcdma_gsm_choices
                        : R.array.enabled_networks_lte_wcdma_gsm_choices;
                mEntryValuesResId = R.array.enabled_networks_lte_wcdma_gsm_values;
            } else {
                String[] defaultPreferredNetworkChoiceArray;
                if (mShow4GForLTE) {
                        defaultPreferredNetworkChoiceArray = getContext().getResources()
                                .getStringArray(
                                R.array.default_preferred_network_4g_type_array_choices);
                } else {
                        defaultPreferredNetworkChoiceArray = getContext().getResources()
                                .getStringArray(
                                R.array.default_preferred_network_lte_type_array_choices);
                }
                String entryChoiceId = defaultPreferredNetworkChoiceArray[idx];
                mEntryChoicesResId = getContext().getResources().getIdentifier(entryChoiceId,
                        "array", "com.google.android.apps.wearable.settings");
                String[] defaultPreferredNetworkValueArray = getContext().getResources()
                        .getStringArray(R.array.default_preferred_network_type_array_values);
                String entryValueId = defaultPreferredNetworkValueArray[idx];
                mEntryValuesResId = getContext().getResources().getIdentifier(entryValueId, "array",
                        "com.google.android.apps.wearable.settings");
            }
        }
    }

    private void debugLog(final String s) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, s);
        }
    }
}
