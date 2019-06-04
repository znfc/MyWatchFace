package com.google.android.clockwork.settings.connectivity.cellular;

import static android.os.UserManager.DISALLOW_DATA_ROAMING;
import static com.google.android.clockwork.settings.cellular.Constants.EXTRA_NEW_LOCK_SIM_STATE;
import static com.google.android.clockwork.settings.cellular.Constants.EXTRA_IS_PUK_PIN;
import static com.google.android.clockwork.settings.cellular.Constants.EXTRA_RESULT_RECEIVER;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.preference.AcceptDenyDialogPreference;
import android.support.wearable.preference.WearableDialogPreference;
import android.support.wearable.view.AcceptDenyDialog;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.SmsApplication;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.common.concurrent.AbstractCwRunnable;
import com.google.android.clockwork.common.concurrent.Executors;
import com.google.android.clockwork.companionrelay.Intents;
import com.google.android.clockwork.phone.Utils;
import com.google.android.clockwork.settings.Constants;
import com.google.android.clockwork.settings.NetworkPolicyNotificationIntentService;
import com.google.android.clockwork.settings.RestrictedAcceptDenySwitchPreference;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.cellular.PhoneNumberActivity;
import com.google.android.clockwork.settings.cellular.SetNumberService;
import com.google.android.clockwork.settings.cellular.SimLockEnableActivity;
import com.google.android.clockwork.settings.cellular.SimUnlockActivity;
import com.google.android.clockwork.settings.cellular.SimPinChangeActivity;
import com.google.android.clockwork.settings.cellular.VoicemailNumberActivity;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;

import com.google.android.clockwork.settings.utils.DefaultBluetoothModeManager;
import com.google.android.clockwork.settings.utils.FeatureManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Cellular settings.
 */
public class CellularSettingsFragment extends SettingsPreferenceFragment {
    private static final String TAG = CellularSettingsFragment.class.getSimpleName();

    private static final String KEY_PREF_CELLULAR_TOGGLE_V1 = "pref_cellularToggle";
    private static final String KEY_PREF_CELLULAR_TOGGLE_V2 = "pref_cellularToggleV2";
    private static final String KEY_PREF_DATA_USAGE = "pref_dataUsage";
    private static final String KEY_PREF_RESUME_DATA = "pref_resumeData";
    private static final String KEY_PREF_DATA_ROAMING = "pref_dataRoaming";
    private static final String KEY_PREF_UNLOCK_SIM = "pref_unlockSim";
    private static final String KEY_PREF_LOCK_SIM_CARD = "pref_lockSimToggle";
    private static final String KEY_PREF_CHANGE_SIM_PIN = "pref_changeSimPin";
    private static final String KEY_PREF_CALL_FORWARDING = "pref_callForwarding";
    private static final String KEY_PREF_CALL_FORWARDING_IOS = "pref_callForwardingIos";
    private static final String KEY_PREF_DEFAULT_SMS = "pref_defaultSms";
    private static final String KEY_PREF_PHONE_NUMBER = "pref_phoneNumber";
    private static final String KEY_PREF_VOICEMAIL_NUMBER = "pref_voicemailNumber";
    private static final String KEY_PREF_ADVANCED_SETTINGS = "pref_advancedSettings";

    private static final int SET_PHONE_NUMBER_REQUEST_CODE = 1;
    private static final int SET_VOICEMAIL_NUMBER_REQUEST_CODE = 2;
    private static final int SET_DEFAULT_SMS_REQUEST_CODE = 3;

    private static final String CELL_AUTO_SETTING_KEY = "clockwork_cell_auto_setting";
    private static final int CELL_AUTO_OFF = 0;
    private static final int CELL_AUTO_ON = 1;

    private static final String CELL_STATE_ON = "on";
    private static final String CELL_STATE_AUTO = "auto";
    private static final String CELL_STATE_OFF = "off";

    private Preference mDataUsage;
    private RestrictedAcceptDenySwitchPreference mDataRoaming;
    private Preference mUnlockSim;
    private SwitchPreference mLockSimCard;
    private Preference mChangeSimPin;
    private Preference mPhoneNumber;
    private Preference mVoicemail;

    private ContentResolver mContentResolver;
    private PackageManager mPackageManager;
    private TelephonyManager mTelephonyManager;
    private Resources mResources;

    private ServiceState mServiceState;
    private Phone mPhone;
    private PhoneStateListener mPhoneStateListener;
    private boolean mCellMediatorCellAutoEnabled;

    private boolean mLocalEdition;

    /**
     * Called when requested cellular data stuctures have changed.
     */
    private ResultReceiver mReceiver = new ResultReceiver(new Handler()) {
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            updatePreferenceStates(resultCode, resultData);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContentResolver = getContext().getContentResolver();
        mPackageManager = getContext().getPackageManager();
        mResources = getContext().getResources();
        mTelephonyManager =
                (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        mPhone = PhoneFactory.getDefaultPhone();
        mLocalEdition = FeatureManager.INSTANCE.get(getContext()).isLocalEditionDevice();

        mPhoneStateListener = new PhoneStateListener() {
            @Override
            public void onServiceStateChanged(ServiceState serviceState) {
                if (serviceState == null) {
                    Log.e(TAG, "onServiceStateChanged(), serviceState=null");
                    return;
                }
                mServiceState = serviceState;
                updatePreferenceStates(Activity.RESULT_OK, null);
            }
        };

        addPreferencesFromResource(R.xml.prefs_cellular);
        mCellMediatorCellAutoEnabled =
                SystemProperties.getBoolean("config.enable_cellmediator_cell_auto", false);

        initCellularToggleV1((SwitchPreference) findPreference(KEY_PREF_CELLULAR_TOGGLE_V1));
        initCellularToggleV2((ListPreference) findPreference(KEY_PREF_CELLULAR_TOGGLE_V2));
        mDataUsage = findPreference(KEY_PREF_DATA_USAGE);
        initResumeData((AcceptDenyDialogPreference) findPreference(KEY_PREF_RESUME_DATA));
        initDataRoaming(mDataRoaming = (RestrictedAcceptDenySwitchPreference) findPreference(
                KEY_PREF_DATA_ROAMING));
        initUnlockSim(mUnlockSim = findPreference(KEY_PREF_UNLOCK_SIM));
        initLockSimCard(mLockSimCard = (SwitchPreference) findPreference(KEY_PREF_LOCK_SIM_CARD));
        initChangeSimPin(mChangeSimPin = findPreference(KEY_PREF_CHANGE_SIM_PIN));
        initCallForwarding((WearableDialogPreference) findPreference(KEY_PREF_CALL_FORWARDING));
        initCallForwardingIos(
                (WearableDialogPreference) findPreference(KEY_PREF_CALL_FORWARDING_IOS));
        initDefaultSms((ListPreference) findPreference(KEY_PREF_DEFAULT_SMS));
        initPhoneNumber(mPhoneNumber = findPreference(KEY_PREF_PHONE_NUMBER));
        initVoicemailNumber(mVoicemail = findPreference(KEY_PREF_VOICEMAIL_NUMBER));

        updatePreferenceStates(Activity.RESULT_OK, null);
    }

    @Override
    public void onResume() {
        super.onResume();
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE);
    }

    @Override
    public void onPause() {
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        super.onPause();
    }

    private void initCellularToggleV1(final SwitchPreference pref) {
        if (mCellMediatorCellAutoEnabled) {
            getPreferenceScreen().removePreference(pref);
            return;
        }

        pref.setChecked(Settings.Global.getInt(mContentResolver,
                Settings.Global.CELL_ON, PhoneConstants.CELL_ON_FLAG)
                == PhoneConstants.CELL_ON_FLAG);
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            if ((Boolean) newVal) {
                String toggleableRadios = Settings.Global.getString(mContentResolver,
                        Settings.Global.AIRPLANE_MODE_TOGGLEABLE_RADIOS);
                boolean cellToggleable = toggleableRadios != null
                        && toggleableRadios.contains(Settings.Global.RADIO_CELL);
                boolean airplaneMode = Settings.Global.getInt(mContentResolver,
                        Settings.Global.AIRPLANE_MODE_ON, 0) == 1;
                if (airplaneMode && !cellToggleable) {
                    AcceptDenyDialog d = new AcceptDenyDialog(getContext());
                    d.setPositiveButton((dialog, which) -> {
                        dialog.dismiss();
                    });
                    d.setTitle(mResources.getString(
                            R.string.pref_cellularToggle_noCellAirplane_dialogTitle));
                    d.setMessage(mResources.getString(
                            R.string.pref_cellularToggle_noCellAirplane_dialogMessage));
                    d.show();
                    return false;
                }
                Settings.Global.putInt(mContentResolver,
                        Settings.Global.ENABLE_CELLULAR_ON_BOOT, 1);
                Settings.Global.putInt(mContentResolver,
                        Settings.Global.CELL_ON, PhoneConstants.CELL_ON_FLAG);
                mPhone.setRadioPower(true);
                if (!mLocalEdition) {
                    mPhone.setUserDataEnabled(true);
                }
            } else {
                Settings.Global.putInt(mContentResolver,
                        Settings.Global.CELL_ON, PhoneConstants.CELL_OFF_FLAG);
                Settings.Global.putInt(mContentResolver,
                        Settings.Global.ENABLE_CELLULAR_ON_BOOT, 0);
                // We don't want to setDataEnabled(false) here because setRadioPower(false)
                // can also disable the cell data. setDataEnabled(false) may cause the Carrier
                // Certifier test fail (b/30420345).
                mPhone.setRadioPower(false);
            }
            return true;
        });
    }

    private String cellStateToPrefValue(boolean cellOn, boolean cellAuto) {
        if (!cellOn) {
            return CELL_STATE_OFF;
        } else if (cellAuto) {
            return CELL_STATE_AUTO;
        } else {
            return CELL_STATE_ON;
        }
    }

    private int cellPrefValueToIcon(String value) {
        if (value.equals(CELL_STATE_ON)) {
            return R.drawable.ic_cc_settings_cellular_4;
        } else if (value.equals(CELL_STATE_AUTO)) {
            return R.drawable.ic_settings_cellular_automatic_mode;
        } else {
            return R.drawable.ic_settings_cellular_off;
        }
    }

    private void initCellularToggleV2(final ListPreference pref) {
        if (!mCellMediatorCellAutoEnabled) {
            getPreferenceScreen().removePreference(pref);
            return;
        }

        pref.setEntryValues(new String[] {
            CELL_STATE_ON,
            CELL_STATE_AUTO,
            CELL_STATE_OFF
        });

        pref.setValue(
            cellStateToPrefValue(
                Settings.Global.getInt(
                    mContentResolver,
                    Settings.Global.CELL_ON,
                    PhoneConstants.CELL_ON_FLAG) == PhoneConstants.CELL_ON_FLAG,
                // TODO implement default cell auto-on value better (b/33589216)
                Settings.System.getInt(
                    mContentResolver,
                    CELL_AUTO_SETTING_KEY,
                    CELL_AUTO_ON) == CELL_AUTO_ON
            )
        );
        pref.setIcon(cellPrefValueToIcon(pref.getValue()));

        pref.setOnPreferenceChangeListener((p, newVal) -> {
            String value = (String)newVal;
            boolean cellOn = !value.equals(CELL_STATE_OFF);
            boolean cellAuto = value.equals(CELL_STATE_AUTO);

            if (cellOn) {
                String toggleableRadios = Settings.Global.getString(mContentResolver,
                        Settings.Global.AIRPLANE_MODE_TOGGLEABLE_RADIOS);
                boolean cellToggleable = toggleableRadios != null
                        && toggleableRadios.contains(Settings.Global.RADIO_CELL);
                boolean airplaneMode = Settings.Global.getInt(mContentResolver,
                        Settings.Global.AIRPLANE_MODE_ON, 0) == 1;
                if (airplaneMode && !cellToggleable) {
                    AcceptDenyDialog d = new AcceptDenyDialog(getContext());
                    d.setPositiveButton((dialog, which) -> {
                        dialog.dismiss();
                    });
                    d.setTitle(mResources.getString(
                            R.string.pref_cellularToggle_noCellAirplane_dialogTitle));
                    d.setMessage(mResources.getString(
                            R.string.pref_cellularToggle_noCellAirplane_dialogMessage));
                    d.show();
                    return false;
                }
                Settings.Global.putInt(mContentResolver,
                        Settings.Global.ENABLE_CELLULAR_ON_BOOT, 1);
                Settings.Global.putInt(mContentResolver,
                        Settings.Global.CELL_ON, PhoneConstants.CELL_ON_FLAG);
                if (!mLocalEdition) {
                    mPhone.setUserDataEnabled(true);
                }
            } else {
                Settings.Global.putInt(mContentResolver,
                        Settings.Global.CELL_ON, PhoneConstants.CELL_OFF_FLAG);
                Settings.Global.putInt(mContentResolver,
                        Settings.Global.ENABLE_CELLULAR_ON_BOOT, 0);
                // We don't want to setDataEnabled(false) here because setRadioPower(false)
                // can also disable the cell data. setDataEnabled(false) may cause the Carrier
                // Certifier test fail (b/30420345).
            }
            Settings.System.putInt(
                mContentResolver,
                CELL_AUTO_SETTING_KEY,
                cellAuto ? CELL_AUTO_ON : CELL_AUTO_OFF
            );
            pref.setIcon(cellPrefValueToIcon(value));
            return true;
        });
    }

    private void initResumeData(final AcceptDenyDialogPreference pref) {
        if (NetworkPolicyNotificationIntentService.dataIsResumable(getContext())) {
            pref.setOnDialogClosedListener((positiveResult) -> {
                if (positiveResult) {
                    setResumeData();
                }
            });
        } else {
            getPreferenceScreen().removePreference(pref);
        }
    }

    private void setResumeData() {
        final Context context = getContext();
        if (context != null){
            final Intent intent = new Intent(ResumeDataService.ACTION_RESUME_DATA);
            final Bundle b = new Bundle();
            b.putString(ResumeDataService.ACTION_RESUME_DATA, ResumeDataService.ACTION_RESUME_DATA);
            b.putString(ResumeDataService.SUBSCRIBER_ID, mTelephonyManager.getSubscriberId());
            b.putStringArray(ResumeDataService.MERGED_SUBSCRIBER_IDS,
                    mTelephonyManager.getMergedSubscriberIds());
            intent.putExtra(CallForwardingService.EXTRA_RESULT_RECEIVER, mReceiver);
            intent.putExtra(CallForwardingService.EXTRA_BUNDLE, b);
            intent.setClass(context.getApplicationContext(), ResumeDataService.class);
            context.getApplicationContext().startService(intent);
        }
    }

    private void initDataRoaming(RestrictedAcceptDenySwitchPreference pref) {
        pref.setTitle(getDataRoamingTitle(mLocalEdition));
        pref.setRestriction(DISALLOW_DATA_ROAMING);
        pref.setChecked(mPhone != null && mPhone.getDataRoamingEnabled());
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            mPhone.setDataRoamingEnabled(((Boolean) newVal));
            return true;
        });
    }

    private void initUnlockSim(Preference pref) {
        final Context context = getContext();
        if (context == null) {
            return;
        }

        int simState = mTelephonyManager.getSimState();
        Intent intent = new Intent(context, SimUnlockActivity.class);
        intent.putExtra(EXTRA_RESULT_RECEIVER, mReceiver);
        if (simState == TelephonyManager.SIM_STATE_PIN_REQUIRED) {
            intent.putExtra(EXTRA_IS_PUK_PIN, false);
            pref.setIntent(intent);
        } else if (simState == TelephonyManager.SIM_STATE_PUK_REQUIRED) {
            intent.putExtra(EXTRA_IS_PUK_PIN, true);
            pref.setIntent(intent);
        } else {
            getPreferenceScreen().removePreference(pref);
        }
    }

    private void initLockSimCard(SwitchPreference pref) {
        final Context context = getContext();
        if (context == null) {
            return;
        }

        int simState = mTelephonyManager.getSimState();
        if (simState == TelephonyManager.SIM_STATE_ABSENT || mPhone == null) {
            getPreferenceScreen().removePreference(pref);
        } else if (simState == TelephonyManager.SIM_STATE_PIN_REQUIRED ||
                    simState == TelephonyManager.SIM_STATE_PUK_REQUIRED) {
            pref.setChecked(true);
            pref.setEnabled(false);
        } else if(simState == TelephonyManager.SIM_STATE_READY) {
            boolean currentLockSimState = mPhone.getIccCard().getIccLockEnabled();
            pref.setChecked(currentLockSimState);
            Intent intent = new Intent(context, SimLockEnableActivity.class);
            intent.putExtra(EXTRA_NEW_LOCK_SIM_STATE, !currentLockSimState);
            intent.putExtra(EXTRA_RESULT_RECEIVER, mReceiver);
            pref.setIntent(intent);
            pref.setEnabled(true);
        } else {
            pref.setEnabled(false);
        }
    }

    private void initChangeSimPin(Preference pref) {
        final Context context = getContext();
        if (context == null) {
            return;
        }

        int simState = mTelephonyManager.getSimState();
        Intent intent = new Intent(context, SimPinChangeActivity.class);
        intent.putExtra(EXTRA_RESULT_RECEIVER, mReceiver);
        if (simState == TelephonyManager.SIM_STATE_ABSENT || mPhone == null) {
            getPreferenceScreen().removePreference(pref);
        } else if (simState == TelephonyManager.SIM_STATE_READY) {
            boolean currentLockSimState = mPhone.getIccCard().getIccLockEnabled();
            pref.setEnabled(currentLockSimState);
            pref.setIntent(intent);
        } else {
            pref.setEnabled(false);
        }
    }

    private void initCallForwarding(WearableDialogPreference pref) {
        if (Utils.isCallForwardingAllowed()
                && (DefaultBluetoothModeManager.INSTANCE.get(getContext()).getBluetoothMode()
                        != SettingsContract.BLUETOOTH_MODE_ALT)) {
            getCallForwardingState();
            pref.setOnDialogClosedListener((button) -> {
                Bundle data = new Bundle();
                switch (button) {
                case DialogInterface.BUTTON_POSITIVE:
                    getActivity().startActivity(
                            new Intent(getContext(), CallForwardingActivity.class));
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    data.putInt(Constants.FIELD_COMMAND,
                            Constants.RPC_TURN_OFF_CALL_FORWARDING);
                    setCallForwardingState(SettingsContract.CALL_FORWARD_ACTION_OFF);
                    getContext().startService(Intents.getRelayRpcIntent(
                            Constants.PATH_RPC_WITH_FEATURE, data));
                    getActivity().startActivity(new Intent(getContext(), ConfirmationActivity.class)
                            .putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                                    ConfirmationActivity.OPEN_ON_PHONE_ANIMATION));
                    break;
                case DialogInterface.BUTTON_NEUTRAL:
                    data.putInt(Constants.FIELD_COMMAND,
                            Constants.RPC_OPEN_CALL_FORWARDING_HELP);
                    getContext().startService(Intents.getRelayRpcIntent(
                            Constants.PATH_RPC_WITH_FEATURE, data));
                    getActivity().startActivity(new Intent(getContext(), ConfirmationActivity.class)
                            .putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                                    ConfirmationActivity.OPEN_ON_PHONE_ANIMATION));
                    break;
                }
                getCallForwardingState();
            });
        } else {
            getPreferenceScreen().removePreference(pref);
        }
    }

    private void getCallForwardingState() {
        Context context = getContext();
        if (context != null){
            final Intent intent =
                    new Intent(CallForwardingService.ACTION_GET_CALL_FORWARDING_STATE);
            intent.putExtra(CallForwardingService.EXTRA_RESULT_RECEIVER, mReceiver);
            intent.putExtra(CallForwardingService.EXTRA_BUNDLE, new Bundle());
            intent.setClass(context.getApplicationContext(), CallForwardingService.class);
            context.getApplicationContext().startService(intent);
        }
    }

    private void setCallForwardingState(final int action) {
        final Bundle b = new Bundle();
        b.putInt(CallForwardingService.CALL_FORWARDING_ACTION, action);
        final Intent intent = new Intent(CallForwardingService.ACTION_SET_CALL_FORWARDING_STATE);
        intent.putExtra(CallForwardingService.EXTRA_RESULT_RECEIVER, mReceiver);
        intent.putExtra(CallForwardingService.EXTRA_BUNDLE, b);
        intent.setClass(getContext().getApplicationContext(), CallForwardingService.class);
        getContext().getApplicationContext().startService(intent);
    }

    private void updateCallForwardingDialogMessage(final int lastAction, final String date) {
        final WearableDialogPreference pref = (WearableDialogPreference)
                findPreference(KEY_PREF_CALL_FORWARDING);
        if (lastAction == SettingsContract.CALL_FORWARD_ACTION_ON) {
            pref.setDialogMessage(getResources().getString(
                    R.string.pref_callForwarding_turnOn_requested, date));
        } else if (lastAction == SettingsContract.CALL_FORWARD_ACTION_OFF) {
            pref.setDialogMessage(getResources().getString(
                    R.string.pref_callForwarding_turnOff_requested, date));
        } else {
            pref.setDialogMessage(null);
        }
    }

    private void initCallForwardingIos(WearableDialogPreference pref) {
        if (Utils.isCallForwardingAllowed()
                && (DefaultBluetoothModeManager.INSTANCE.get(getContext()).getBluetoothMode()
                        == SettingsContract.BLUETOOTH_MODE_ALT)) {
            pref.setOnDialogClosedListener((button) -> {
                if (button == DialogInterface.BUTTON_NEUTRAL) {
                    Bundle data = new Bundle();
                    data.putInt(Constants.FIELD_COMMAND, Constants.RPC_OPEN_CALL_FORWARDING_HELP);

                    getContext().startService(
                            Intents.getRelayRpcIntent(Constants.PATH_RPC_WITH_FEATURE, data));
                }
            });
        } else {
            getPreferenceScreen().removePreference(pref);
        }
    }

    private void initDefaultSms(final ListPreference pref) {
        if (mResources.getBoolean(R.bool.config_default_sms_app)
                && mTelephonyManager.isSmsCapable()) {
            List<String> entries = new ArrayList<>();
            List<String> entryValues = new ArrayList<>();

            for (SmsApplication.SmsApplicationData app :
                    SmsApplication.getApplicationCollection(getContext())) {
                try {
                    String packageName = app.mPackageName;
                    ApplicationInfo appInfo =
                            mPackageManager.getApplicationInfo(packageName, 0/*flags*/);
                    if (appInfo != null) {
                        entries.add(appInfo.loadLabel(mPackageManager).toString());
                        entryValues.add(packageName);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    // Ignore package can't be found
                }
            }

            pref.setEntries(entries.toArray(new String[entries.size()]));
            pref.setEntryValues(entryValues.toArray(new String[entryValues.size()]));

            // setup default SMS package if available
            ComponentName component = SmsApplication.getDefaultSmsApplication(getContext(), false);
            if (component != null) {
                String defaultPackage = component.getPackageName();
                int index = pref.findIndexOfValue(defaultPackage);
                if (index != -1) {
                    pref.setValue(defaultPackage);
                    pref.setSummary(pref.getEntries()[index]);
                }
            }

            pref.setOnPreferenceChangeListener((p, newValue) -> {
                String packageName = (String) newValue;
                SmsApplication.setDefaultApplication(packageName, getContext());
                pref.setSummary(pref.getEntries()[pref.findIndexOfValue(packageName)]);
                return true;
            });
        } else {
            getPreferenceScreen().removePreference(pref);
        }
    }

    private void initPhoneNumber(Preference pref) {
        if (mResources.getBoolean(R.bool.config_displayed_phone_number)) {
            pref.setSummary(mPhone.getLine1Number());
            pref.setOnPreferenceClickListener((p) -> {
                Intent intent = new Intent(getContext(), PhoneNumberActivity.class)
                    .putExtra(SetNumberService.EXTRA_OLD_NUMBER, mPhone.getLine1Number())
                    .putExtra(SetNumberService.EXTRA_RESULT_RECEIVER, mReceiver);
                startActivity(intent);
                return true;
            });
        } else {
            getPreferenceScreen().removePreference(pref);
        }
    }

    private void initVoicemailNumber(Preference pref) {
        pref.setSummary(mPhone.getVoiceMailNumber());
        pref.setOnPreferenceClickListener((p) -> {
            Intent intent = new Intent(getContext(), VoicemailNumberActivity.class)
                    .putExtra(SetNumberService.EXTRA_OLD_NUMBER, mPhone.getVoiceMailNumber())
                    .putExtra(SetNumberService.EXTRA_RESULT_RECEIVER, mReceiver);
            startActivity(intent);
            return true;
        });
    }

    private void updatePreferenceStates(int resultCode, final Bundle b) {
        if (b != null) {
            if (b.getString(ResumeDataService.ACTION_RESUME_DATA) != null) {
                if (resultCode == Activity.RESULT_OK) {
                    NetworkPolicyNotificationIntentService.notifyDataResumed(getContext());
                    getPreferenceScreen().removePreference(findPreference(KEY_PREF_RESUME_DATA));
                }
            } else {
                final int action = b.getInt(CallForwardingService.CALL_FORWARDING_ACTION);
                final String date = b.getString(CallForwardingService.CALL_FORWARDING_DATE);
                updateCallForwardingDialogMessage(action, date);
            }
        }

        initUnlockSim(mUnlockSim);
        initLockSimCard(mLockSimCard);
        initChangeSimPin(mChangeSimPin);

        formatPhoneNumbers();
    }

    private void formatPhoneNumbers() {
        final Context context = getContext();
        if (context == null) {
            return;
        }
        // Formatting the phone numbers can hit the disk, so use an AsyncTask to perform the work
        // on a background thread.
        Executors.INSTANCE.get(context).getUserExecutor().submit(
                new AbstractCwRunnable("LoadPhoneAndVoicemailNumber") {
            @Override
            public void run() {
                final Activity activity = getActivity();
                final Context context = getContext();
                if (activity == null || context == null) {
                    return;
                }

                final String formattedPhoneNumber =
                        mResources.getBoolean(R.bool.config_displayed_phone_number)
                        ? Utils.formatNumber(context, mPhone.getLine1Number())
                        : null;
                final String formattedVoiceMailNumber = Utils.formatNumber(
                        context, mPhone.getVoiceMailNumber());

                activity.runOnUiThread(() -> {
                    if (isDetached() || isRemoving()) {
                        return;
                    }
                    mPhoneNumber.setSummary(formattedPhoneNumber);
                    mVoicemail.setSummary(formattedVoiceMailNumber);
                });
            }
        });
    }

    @VisibleForTesting
    @StringRes
    static int getDataRoamingTitle(boolean isLocalEditionDevice) {
        return isLocalEditionDevice
                ? R.string.pref_internationalDataRoaming
                : R.string.pref_dataRoaming;
    }
}
