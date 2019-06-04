package com.google.android.clockwork.settings.cellular;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.UserManager;
import android.preference.ListPreference;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.OperatorInfo;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.phone.INetworkQueryService;
import com.android.phone.INetworkQueryServiceCallback;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.common.concurrent.CwAsyncTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Network operators settings UI. See com.android.phone.NetworkSetting
 *
 * 1. A service connection is made to the cellular network framework.
 *  -This is done in the fragment for proper lifecycle management.
 * 2. A query scan RPC is sent to this bound service.
 * 3. Handler is notified when the query scan is complete.
 * 4. We present the array of network operators to the user
 *   4.1 Put automatic selection as first option.
 * 5a. The user selects auto
 *   5a.1 Aync thread selects auto via phone interface
 *   5a.2 Handler is notified when auto selection is complete.
 * 5b. The user selects a manual entry.
 *   5b.1 Async thread selects a manual entry via phone interface
 *   5b.2 Handler is notified when manual selection is complete.
 * 6. Service connection is released.
 *  -This is done in the fragment for proper lifecycle management.
 *
 * It is possible for the current network selected to be null
 * under the conditions the user has tried to select a forbidden
 * or otherwise inaccessible network.
 */
public class NetworkOperatorsPreference extends ListPreference {
    private static final String TAG = "NetworkOperatorsPref";

    private static final int NETWORK_QUERY_SERVICE_QUERY_OK = 0;

    private static final int EVENT_NETWORK_SCAN_COMPLETED = 100;
    private static final int EVENT_NETWORK_MANUAL_SELECT_DONE = 200;
    private static final int EVENT_NETWORK_AUTO_SELECT_DONE = 300;
    private static final int EVENT_CHECK_FOR_NETWORK_AUTOMATIC = 400;

    private static final String AUTOMATIC_NETWORK_SELECTION_VALUE = "AUTOMATIC";
    private static final String MANUAL_NETWORK_SELECTION_PREFIX_VALUE = "MANUAL";
    private static final String SELECTED_OPERATOR_INFO = "SELECTED_OPERATOR_INFO";

    /* package */ static final ComponentName NETWORK_QUERY_SERVICE_COMPONENT = new ComponentName(
            "com.android.phone", "com.android.phone.NetworkQueryService");

    private Phone mPhone;
    private boolean mAutomaticNetworkSelected;

    private WakeLock queryScreenWakeLock;

    private HashMap<String, OperatorInfo> mNetworkInfoMap = new HashMap<String, OperatorInfo>();

    public NetworkOperatorsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NetworkOperatorsPreference(Context context) {
        super(context);
        init();
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
        // We should already be clean but try again.
        unregisterNetworkQueryService();
    }

    private void init() {
        final UserManager mUm = (UserManager) getContext().getSystemService(Context.USER_SERVICE);
        if (mUm.hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)) {
            setEnabled(false);
            Log.e(TAG, "Cellular network settings are not available for this user.");
            return;
        }

        // Assuming watch has only one phone.
        mPhone = PhoneFactory.getDefaultPhone();
        if (mPhone == null) {
            Log.e(TAG, "Unable to get default phone");
        }

        if (mPhone != null) {
            Message msg = mHandler.obtainMessage(EVENT_CHECK_FOR_NETWORK_AUTOMATIC);
            mPhone.getNetworkSelectionMode(msg);
        }

        setTitle(R.string.network_operators_action);
        setIcon(R.drawable.ic_settings_networkoperator);
        setPersistent(false);
        setDialogTitle(R.string.network_operators_title);

        // Disable preference and put in empty list until we get results from network query
        setEnabled(false);
        setEntries(new String[0]);
        setEntryValues(new String[0]);

        setOnPreferenceChangeListener((p, newVal) -> {
            setEnabled(false);
            setSummary(R.string.network_state_connecting);
            if (AUTOMATIC_NETWORK_SELECTION_VALUE.equals(newVal)) {
                new SetNetworkAutomaticTask().submit();
            } else {
                OperatorInfo ni = mNetworkInfoMap.get((String) newVal);
                new SetNetworkManuallyTask().submit(ni);
            }
            return true;
        });

        PowerManager pm = getContext().getSystemService(PowerManager.class);
        queryScreenWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE,
            TAG);
        queryScreenWakeLock.setReferenceCounted(false);
    }

    /**
     * Clean up and release all resources
     * This method is idempotent; may be called repeatedly.
     */
    private void unregisterNetworkQueryService() {
        if (mNetworkQueryService != null) {
            try {
                debugLog("unregisterNetworkQueryService(): Unregister query service callback");
                mNetworkQueryService.unregisterCallback(mCallback);
                mNetworkQueryService = null;
            } catch (RemoteException e) {
                Log.e(TAG, "parseQueriedNetworkList: exception from unregisterCallback " + e);
            }
        }

        queryScreenWakeLock.release();
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            AsyncResult ar;
            switch (msg.what) {
                case EVENT_NETWORK_SCAN_COMPLETED:
                    if (mAutomaticNetworkSelected) {
                        setAutomaticNetworkSelected();
                    } else {
                        setSummary(R.string.network_type_unknown);
                    }
                    parseQueriedNetworkList((List<OperatorInfo>) msg.obj, msg.arg1);
                    debugLog("EVENT_NETWORK_SCAN_COMPLETED Network scan has completed");
                    break;
                case EVENT_NETWORK_MANUAL_SELECT_DONE:
                    ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        debugLog("EVENT_NETWORK_MANUAL_SELECT_DONE manual network selection"
                                + " complete: failed!");
                        onNetworkSelectionFailed(ar.exception);
                    } else {
                        debugLog("EVENT_NETWORK_MANUAL_SELECT_DONE manual network selection"
                                + " complete: succeeded!");
                        final Bundle b = msg.getData();
                        if (b != null) {
                            OperatorInfo currentNetwork = b.getParcelable(SELECTED_OPERATOR_INFO);
                            setManualNetworkSelected(currentNetwork);
                        }
                        Log.i(TAG, "Registered on network.");
                    }
                    setEnabled(true);
                    break;
                case EVENT_NETWORK_AUTO_SELECT_DONE:
                    ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        debugLog("EVENT_NETWORK_AUTO_SELECT_DONE automatic network selection"
                                + " complete: failed!");
                        onNetworkSelectionFailed(ar.exception);
                    } else {
                        debugLog("EVENT_NETWORK_AUTO_SELECT_DONE automatic network selection"
                                + " complete: succeeded!");
                        setAutomaticNetworkSelected();
                        Log.i(TAG, "Registered on network.");
                    }
                    setEnabled(true);
                    break;
                case EVENT_CHECK_FOR_NETWORK_AUTOMATIC:
                    ar = (AsyncResult) msg.obj;
                    if (ar.exception != null) {
                        debugLog("EVENT_CHECK_FOR_NETWORK_AUTOMATIC query automatic network"
                                + " selection complete: failed!");
                    } else if (ar.result != null) {
                        try {
                            int[] modes = (int[]) ar.result;
                            if (modes[0] == 0) {
                                debugLog("EVENT_CHECK_FOR_NETWORK_AUTOMATIC query automatic"
                                        + " network selection complete: succeeded! automatic");
                                mAutomaticNetworkSelected = true;
                                setAutomaticNetworkSelected();
                            } else {
                                mAutomaticNetworkSelected = false;
                                debugLog("EVENT_CHECK_FOR_NETWORK_AUTOMATIC query automatic"
                                        + " network selection complete: succeeded! manual");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Unable to parse automatic network selection query");
                        }
                    }
                    break;
            }
        }
    };

    /** Local service interface */
    private INetworkQueryService mNetworkQueryService = null;

    /**
     * This implementation of INetworkQueryServiceCallback is used to receive
     * callback notifications from the network query service.
     */
    private final INetworkQueryServiceCallback mCallback = new INetworkQueryServiceCallback.Stub() {
        /** place the message on the looper queue upon query completion. */
        public void onQueryComplete(List<OperatorInfo> networkInfoArray, int status) {
            debugLog("INetworkQueryServiceCallback notifying message loop of query completion.");
            final Message msg = mHandler.obtainMessage(EVENT_NETWORK_SCAN_COMPLETED,
                    status, 0, networkInfoArray);
            msg.sendToTarget();
        }
    };

    /* package */ void startNetworkOperatorQuery(IBinder service) {
        if (mNetworkQueryService != null) {
            unregisterNetworkQueryService();
        }
        mNetworkQueryService = INetworkQueryService.Stub.asInterface(service);
        queryNetwork();
    }

    /**
     * Start a network query request to the network query service.
     */
    private void queryNetwork() {
        if (mNetworkQueryService != null && mPhone != null) {
            setSummary(R.string.network_operators_searching);
            try {
                queryScreenWakeLock.acquire();
                debugLog("queryNetwork() started network query");
                mNetworkQueryService.startNetworkQuery(mCallback, mPhone.getPhoneId());
            } catch (RemoteException e) {
                Log.e(TAG, "queryNetwork: exception from startNetworkQuery " + e);
            } finally {
              queryScreenWakeLock.release();
            }
        }
    }

    /**
     * Create a preference list from the list of network query results.
     */
    private void parseQueriedNetworkList(final List<OperatorInfo> result, final int status) {
        if (status != NETWORK_QUERY_SERVICE_QUERY_OK) {
            Log.e(TAG, "Error while searching for networks: " + status);
        } else {
            if (result != null) {
                ArrayList<String> entryChoices = new ArrayList<String>();
                ArrayList<String> entryValues = new ArrayList<String>();

                // Add the automatic selection as first entry
                entryChoices.add(getContext().getResources().getString(
                        R.string.network_operators_select_automatically));
                entryValues.add(AUTOMATIC_NETWORK_SELECTION_VALUE);

                // Current network of null is valid indicating there is no
                // current cell network connection.
                OperatorInfo currentNetwork = null;
                for (OperatorInfo ni : result) {
                    if (ni != null) {
                        entryChoices.add(getNetworkTitle(ni));
                        entryValues.add(MANUAL_NETWORK_SELECTION_PREFIX_VALUE + ni.toString());
                        mNetworkInfoMap.put(MANUAL_NETWORK_SELECTION_PREFIX_VALUE + ni.toString(),
                                ni);
                        debugLog("parseQueriedNetworkList()   " + ni);
                        if (ni.getState() == OperatorInfo.State.CURRENT) {
                            currentNetwork = ni;
                        }
                    }
                }

                String[] entryArr = new String[entryChoices.size()];
                entryArr = entryChoices.toArray(entryArr);

                String[] valueArr = new String[entryValues.size()];
                valueArr = entryValues.toArray(valueArr);

                setEntries(entryArr);
                setEntryValues(valueArr);

                if (mAutomaticNetworkSelected) {
                    setAutomaticNetworkSelected();
                } else {
                    setManualNetworkSelected(currentNetwork);
                }
                debugLog("parseQueriedNetworkList() Created list preference");
                setEnabled(true);
            } else {
                Log.w(TAG, "Unable to gather network operator info");
            }
        }
        unregisterNetworkQueryService();
    }

    private void setAutomaticNetworkSelected() {
        setSummary(R.string.generic_automatic);
        setValue(AUTOMATIC_NETWORK_SELECTION_VALUE);
        debugLog("setAutomaticNetworkSelected() Setting automatic as selected for modem choice");
        notifyChanged();
    }

    private void setManualNetworkSelected(final OperatorInfo ni) {
        if (ni == null) {
            setSummary(R.string.network_state_disconnected);
            setValue(MANUAL_NETWORK_SELECTION_PREFIX_VALUE);
            debugLog("setManualNetworkSelected() no active cell network");
        } else {
            setSummary(getNetworkTitle(ni));
            setValue(MANUAL_NETWORK_SELECTION_PREFIX_VALUE + ni.toString());
            debugLog("setManualNetworkSelected() Setting manual as selected ni: " + ni.toString());
        }
        notifyChanged();
    }

    private void onNetworkSelectionFailed(final Throwable ex) {
        String status = "Cannot connect to this network right now. Try again later.";
        if (ex != null && ex instanceof CommandException) {
            final CommandException ce = (CommandException) ex;
            if (ce.getCommandError() == CommandException.Error.ILLEGAL_SIM_OR_ME) {
                status = "Your SIM card does not allow a connection to this network.";
            }
        }
        Log.e(TAG, status);
        setSummary(R.string.network_state_disconnected);
        notifyChanged();
    }

    /**
     * Inform the telephony framework to use the specified network operator.
     */
    private class SetNetworkManuallyTask extends CwAsyncTask<OperatorInfo, Void, Void> {
        public SetNetworkManuallyTask() {
            super("SetNetworkManuallyTask");
        }

        /**
         * @param  Operatorinfo The selected operator info cell network
         *                      Must not be null.
         */
        @Override
        public Void doInBackground(OperatorInfo... params) {
            final OperatorInfo ni = params[0];
            debugLog(SetNetworkManuallyTask.class.getName() + " doing in background");
            if (mPhone != null) {
                final Bundle b = new Bundle();
                b.putParcelable(SELECTED_OPERATOR_INFO, ni);
                final Message msg = mHandler.obtainMessage(EVENT_NETWORK_MANUAL_SELECT_DONE);
                msg.setData(b);
                mPhone.selectNetworkManually(ni, true, msg);
            }
            return null;
        }
    }

    /**
     * Inform the telephony framework to use automatic cell network selection.
     */
    private class SetNetworkAutomaticTask extends CwAsyncTask<Void, Void, Void> {
        public SetNetworkAutomaticTask() {
            super("SetNetworkAutomaticTask");
        }

        @Override
        public Void doInBackground(Void... params) {
            debugLog(SetNetworkAutomaticTask.class.getName() + " doing in background");
            if (mPhone != null) {
                Message msg = mHandler.obtainMessage(EVENT_NETWORK_AUTO_SELECT_DONE);
                mPhone.setNetworkSelectionModeAutomatic(msg);
            }
            return null;
        }
    }
    /**
     * Returns the title of the network obtained in the manual search.
     *
     * @param OperatorInfo contains the information of the network.
     *
     * @return Long Name if not null/empty, otherwise Short Name if not null/empty,
     * else MCCMNC string.
     */
    private static String getNetworkTitle(final OperatorInfo ni) {
        if (!TextUtils.isEmpty(ni.getOperatorAlphaLong())) {
            return ni.getOperatorAlphaLong();
        } else if (!TextUtils.isEmpty(ni.getOperatorAlphaShort())) {
            return ni.getOperatorAlphaShort();
        } else {
            BidiFormatter bidiFormatter = BidiFormatter.getInstance();
            return bidiFormatter.unicodeWrap(ni.getOperatorNumeric(), TextDirectionHeuristics.LTR);
        }
    }

    private void debugLog(final String string) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, string);
        }
    }
}
