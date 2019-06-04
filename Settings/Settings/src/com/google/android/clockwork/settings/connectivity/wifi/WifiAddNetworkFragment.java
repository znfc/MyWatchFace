package com.google.android.clockwork.settings.connectivity.wifi;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.NetworkSelectionStatus;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.support.wearable.preference.WearableDialogPreference;
import android.support.wearable.preference.WearablePreferenceActivity;
import android.support.wearable.view.AcceptDenyDialog;
import android.util.Log;
import android.widget.BaseAdapter;
import android.widget.ListView;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.WifiTracker;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.common.logging.CwEventLogger;
import com.google.android.clockwork.settings.SettingsIntents;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;
import com.google.android.clockwork.settings.wifi.WifiHelper;
import com.google.android.clockwork.wifi.Constants;
import java.util.Collection;

public class WifiAddNetworkFragment extends SettingsPreferenceFragment
        implements WifiTracker.WifiListener {
    private static final String TAG = "WifiAddNetworkFragment";

    private static final String KEY_AVAILABLE_NETWORKS_GROUP = "pref_available_networks_group";
    private static final String KEY_PREF_ACTIVE_AP = "pref_active_ap";
    private static final String KEY_PREF_WIFI_ADD_NETWORK = "pref_wifi_add_network";
    private static final String KEY_PREF_LOADING_NETWORKS = "pref_wifi_loading_networks";

    private static final String CONFIGURE_NETWORK_FRAGMENT_NAME =
            "com.google.android.clockwork.settings.connectivity.wifi.WifiConfigureNetworkFragment";

    static final String ACTION_SCROLL_TO_TOP =
            "com.google.android.clockwork.settings.connectivity.wifi.SCROLL_TO_TOP";
    static final String ACTION_ACCESS_POINT_CHANGED =
            "com.google.android.clockwork.settings.connectivity.wifi.ACCESS_POINT_CHANGED";
    static final String EXTRA_ACCESS_POINT_KEY = "access_point_key";
    static final String EXTRA_NEW_ACCESS_POINT_KEY = "new_access_point_key";

    private static final int CONFIGURE_NETWORK_REQUEST_CODE = 6;

    /**
     * Used for requesting Wi-Fi password from phone
     */
    private static final String EXTRA_NETWORK_ID = "network_id";
    private static final String EXTRA_DISABLED = "disabled";
    private static final String EXTRA_DISABLE_REASON = "disable_reason";

    private WifiManager mWifiManager;
    private WifiTracker mWifiTracker;

    private WifiInfo mWifiInfo;

    private NetworkCallback mNetworkCallback;

    /** Message services for sending "Open on Phone" intents for Add Network. */
    private Messenger mService;
    private volatile boolean mServiceConnected;
    private final Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * Used for requesting Wi-Fi password from phone when a secure AP is clicked in
     * "Available Networks"; set to null when "Add Network" is clicked. requestWifiPassword() will
     * send the appropriate variation on the Constants.MSG_REQ_WIFI_PASSWORD rpc based on this
     * value, which causes a different dialog to be shown on the phone in these two situations.
     */
    private AccessPoint mSelectedAccessPoint;

    /** Used for logging success rate of entering password on watch or phone. */
    private AccessPointPreference mSelectedAccessPointPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_wifi_add);

        mSelectedAccessPoint = null;

        mWifiManager = (WifiManager) getContext().getSystemService(Context.WIFI_SERVICE);

        IntentFilter filter = new IntentFilter(ACTION_ACCESS_POINT_CHANGED);
        filter.addAction(ACTION_SCROLL_TO_TOP);
        getContext().registerReceiver(mAccessPointChangedReceiver, filter);

        mWifiTracker = new WifiTracker(
                getActivity(), this, true, true);

        PreferenceGroup availableNetworksGroup =
                (PreferenceGroup) findPreference(KEY_AVAILABLE_NETWORKS_GROUP);
        availableNetworksGroup.setLayoutResource(R.layout.preference_group_no_title);

        PreferenceGroup activeNetworkGroup =
                (PreferenceGroup) findPreference(KEY_PREF_ACTIVE_AP);
        activeNetworkGroup.setLayoutResource(R.layout.preference_group_no_title);

        updateCurrentWifiInfo();
        initAddNetwork(findPreference(KEY_PREF_WIFI_ADD_NETWORK));
    }

    @Override
    public void onResume() {
        super.onResume();
        WifiSettingsUtil.setInWifiSettings(getContext(), true);
        if (WifiSettingsUtil.getWearWifiEnabled(getContext())) {
            mNetworkCallback =
                    WifiSettingsUtil.setWifiHoldUp(getContext(), mNetworkCallback, true);
        }
        mWifiTracker.onStart();
    }

    @Override
    public void onPause() {
        super.onPause();
        mWifiTracker.onStop();
        mNetworkCallback =
                WifiSettingsUtil.setWifiHoldUp(getContext(), mNetworkCallback, false);
        WifiSettingsUtil.setInWifiSettings(getContext(), false);
    }

    @Override
    public void onDestroy() {
        if (mServiceConnected) {
            getContext().unbindService(mConnection);
            mServiceConnected = false;
        }
        mWifiTracker.onDestroy();
        getContext().unregisterReceiver(mAccessPointChangedReceiver);
        super.onDestroy();
    }

    @Override
    public void onAccessPointsChanged() {
        // this callback may sometimes be received after this fragment has been detached
        if (getContext() == null) {
            return;
        }
        maybeRemovePreference(findPreference(KEY_PREF_LOADING_NETWORKS));
        updateAccessPoints(
                (PreferenceGroup) findPreference(KEY_AVAILABLE_NETWORKS_GROUP),
                (PreferenceGroup) findPreference(KEY_PREF_ACTIVE_AP));
    }

    @Override
    public void onWifiStateChanged(int state) {
    }

    @Override
    public void onConnectedChanged() {
        updateCurrentWifiInfo();
    }

    private void updateCurrentWifiInfo() {
        ConnectivityManager cm =
                (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            mWifiInfo = mWifiManager.getConnectionInfo();
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onServiceConnected className: " + className);
            }

            mService = new Messenger(service);
            mServiceConnected = true;
            requestWifiPassword();
        }

        public void onServiceDisconnected(ComponentName className) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onServiceDisconnected className: " + className);
            }

            mService = null;
            mServiceConnected = false;
        }
    };

    // AVAILABLE NETWORKS

    private void updateAccessPoints(
            PreferenceGroup availableNetworksGroup, PreferenceGroup activeNetworkGroup) {
        final Collection<AccessPoint> accessPoints = mWifiTracker.getAccessPoints();

        availableNetworksGroup.setTitle(null);

        if (!mWifiTracker.isWifiEnabled() || accessPoints.isEmpty()) {
            activeNetworkGroup.removeAll();
            availableNetworksGroup.removeAll();
            return;
        }

        if (mSelectedAccessPoint != null && !accessPoints.contains(mSelectedAccessPoint)) {
            // Clear active network preference group if mSelectedAccessPoint is no longer in range
            activeNetworkGroup.removeAll();
        }

        // First, remove the access points that are no longer valid from the screen
        int prefCount = availableNetworksGroup.getPreferenceCount();
        for (int i = 0; i < prefCount; i++) {
            AccessPointPreference pref =
                    (AccessPointPreference) availableNetworksGroup.getPreference(i);
            if (!accessPoints.contains(pref.getAccessPoint())) {
                availableNetworksGroup.removePreference(pref);
                prefCount--;
            }
        }

        // Then add the access points that are new to the screen
        for (final AccessPoint accessPoint : accessPoints) {
            String key = AccessPointPreference.getAccessPointKey(accessPoint);
            if (findPreference(key) == null && accessPoint.isReachable()
                    && accessPoint.getSecurity() != AccessPoint.SECURITY_EAP) {
                AccessPointPreference preference = createAvailableNetworkPreference(accessPoint);
                if (mSelectedAccessPoint == null && accessPoint.isActive()) {
                    // If there is no user selected active AP but there is an AP auto connecting,
                    // set the active AP as mSelectedAccessPoint and add it to the active network
                    // group so it shows at the top of the screen.
                    mSelectedAccessPoint = accessPoint;
                    activeNetworkGroup.addPreference(preference);
                } else {
                    availableNetworksGroup.addPreference(preference);
                }
            }
        }

        ((BaseAdapter) getPreferenceScreen().getRootAdapter()).notifyDataSetChanged();
    }

    private AccessPointPreference createAvailableNetworkPreference(AccessPoint accessPoint) {
        AccessPointPreference preference =
                new AccessPointPreference(
                        getContext(), accessPoint, CwEventLogger.getInstance(getContext()));
        preference.setKey(AccessPointPreference.getAccessPointKey(accessPoint));
        preference.setOnPreferenceClickListener((p) -> {
            handleAvailableNetworkClick((AccessPointPreference) p);
            return true;
        });
        return preference;
    }

    private void maybeRemovePreference (Preference preference) {
      if (preference != null) {
        preference.getParent().removePreference(preference);
      }
    }

    private void setSelectedAccessPoint(AccessPointPreference pref) {
        PreferenceGroup availableNetworksGroup =
                (PreferenceGroup) findPreference(KEY_AVAILABLE_NETWORKS_GROUP);
        PreferenceGroup activeNetworkGroup = (PreferenceGroup) findPreference(KEY_PREF_ACTIVE_AP);
        if (mSelectedAccessPoint != null) {
            AccessPointPreference currentSelectedPref =
                    (AccessPointPreference) activeNetworkGroup.findPreference(
                            AccessPointPreference.getAccessPointKey(mSelectedAccessPoint));
            if (currentSelectedPref == null) {
                Log.w(TAG, "current selected add network preference was null");
                return;
            }
            activeNetworkGroup.removePreference(currentSelectedPref);
            availableNetworksGroup.addPreference(currentSelectedPref);
            currentSelectedPref.setOrder(0);
        }
        mSelectedAccessPoint = pref.getAccessPoint();
        availableNetworksGroup.removePreference(pref);
        activeNetworkGroup.addPreference(pref);
    }

    private void handleAvailableNetworkClick(AccessPointPreference pref) {
        setSelectedAccessPoint(pref);
        smoothScrollToTop();

        AccessPoint accessPoint = pref.getAccessPoint();
        if (accessPoint.getSecurity() == AccessPoint.SECURITY_NONE) {
            accessPoint.generateOpenNetworkConfig();
            Log.d(TAG, "Trying to connect to open network with config: " + accessPoint.getConfig());
            mWifiManager.connect(accessPoint.getConfig(), null);
        } else if (accessPoint.getConfig() != null) {
            WifiConfiguration config = accessPoint.getConfig();
            if (config.status == WifiConfiguration.Status.DISABLED) {
                int disableReason =
                        config.getNetworkSelectionStatus().getNetworkSelectionDisableReason();
                if (disableReason == NetworkSelectionStatus.DISABLED_AUTHENTICATION_FAILURE) {
                    startConfigNetworkActivity(pref, accessPoint, true, true);
                } else {
                    startConfigNetworkActivity(pref, accessPoint, true, false);
                }
            } else {
                mWifiManager.connect(config, null);
            }
        } else if (accessPoint.getSecurity() == AccessPoint.SECURITY_WEP
                || accessPoint.getSecurity() == AccessPoint.SECURITY_PSK) {
            startConfigNetworkActivity(pref, accessPoint, false, false);
        }
    }

    private void smoothScrollToTop() {
        ListView listView = getListView();
        if (listView != null) {
            listView.smoothScrollToPosition(0);
        }
    }

    private void startConfigNetworkActivity(
            AccessPointPreference pref,
            AccessPoint accessPoint,
            boolean disabled,
            boolean authFailure) {
        mSelectedAccessPointPref = pref;
        Bundle extras = pref.getExtras();
        extras.putString(WifiConfigureNetworkFragment.EXTRA_SSID_KEY, accessPoint.getSsidStr());
        extras.putBoolean(WifiConfigureNetworkFragment.EXTRA_DISABLED_KEY, disabled);
        extras.putBoolean(WifiConfigureNetworkFragment.EXTRA_AUTH_FAILURE_KEY, authFailure);

        Intent intent = new Intent(WifiAddNetworkFragment.ACTION_SCROLL_TO_TOP);
        getContext().sendBroadcastAsUser(intent, UserHandle.CURRENT_OR_SELF);

        WearablePreferenceActivity preferenceActivity = (WearablePreferenceActivity) getActivity();
        startActivityForResult(
                preferenceActivity.onBuildStartFragmentIntent(
                        CONFIGURE_NETWORK_FRAGMENT_NAME, extras, /* titleRes = */ 0),
                CONFIGURE_NETWORK_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CONFIGURE_NETWORK_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            int resultAction =
                    data.getExtras().getInt(WifiConfigureNetworkFragment.EXTRA_RESULT_ACTION);
            switch (resultAction) {
                case WifiConfigureNetworkFragment.ENTER_ON_WATCH_RESULT:
                    if (mSelectedAccessPointPref != null) {
                        mSelectedAccessPointPref.setWifiPasswordSource(
                                AccessPointPreference.WifiPasswordSource.WATCH);
                    }
                    String key =
                            data.getExtras()
                                    .getString(WifiConfigureNetworkFragment.EXTRA_RESULT_PASSWORD);
                    mWifiManager.connect(
                            WifiHelper.generateConfigForSecuredAp(
                                    mSelectedAccessPoint.getSsid().toString(),
                                    mSelectedAccessPoint.getSecurity(),
                                    key),
                            null);
                    break;
                case WifiConfigureNetworkFragment.ENTER_ON_PHONE_RESULT:
                    if (mSelectedAccessPointPref != null) {
                        mSelectedAccessPointPref.setWifiPasswordSource(
                                AccessPointPreference.WifiPasswordSource.PHONE);
                    }
                    if (!mServiceConnected) {
                        Intent intent = SettingsIntents.getRequestWifiPasswordIntent();
                        getContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
                    } else {
                        requestWifiPassword();
                    }
                    break;
                case WifiConfigureNetworkFragment.FORGET_NETWORK_RESULT:
                    if (!WifiSettingsUtil.canModifyNetwork(
                            getContext(), mSelectedAccessPoint.getConfig())) {
                        RestrictedLockUtils.sendShowAdminSupportDetailsIntent(
                                getActivity(), RestrictedLockUtils.getDeviceOwner(getActivity()));
                    } else {
                        AcceptDenyDialog forgetDialog = new AcceptDenyDialog(getActivity());
                        String ssid =
                            data.getExtras().getString(WifiConfigureNetworkFragment.EXTRA_SSID_KEY);
                        forgetDialog.setTitle(
                                getContext().getString(R.string.wifi_forget_network_title, ssid));
                        forgetDialog.setPositiveButton(
                                (dialog1, which1) -> {
                                    if (which1 == DialogInterface.BUTTON_POSITIVE) {
                                        mWifiManager.forget(
                                                mSelectedAccessPoint.getConfig().networkId, null);
                                    }
                                });
                        forgetDialog.show();
                    }
                    break;
                case WifiConfigureNetworkFragment.RETRY_RESULT:
                    if (mSelectedAccessPoint.getSecurity() == AccessPoint.SECURITY_NONE) {
                        mSelectedAccessPoint.generateOpenNetworkConfig();
                    }
                    WifiConfiguration config = mSelectedAccessPoint.getConfig();
                    if (config != null) {
                        mWifiManager.connect(config, null);
                    } else {
                        Log.w(TAG, "Wi-Fi config was null; not calling connect");
                    }
                    break;
            }
        }
    }

    // Used for updating an AccessPointPreference's key in "Available Networks" preference group
    // when access point changes. Left registered through onPause/onResume to maintain valid cache.
    private BroadcastReceiver mAccessPointChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && ACTION_ACCESS_POINT_CHANGED.equals(intent.getAction())) {
                String curKey = intent.getStringExtra(EXTRA_ACCESS_POINT_KEY);
                String newKey = intent.getStringExtra(EXTRA_NEW_ACCESS_POINT_KEY);
                AccessPointPreference pref = (AccessPointPreference) findPreference(curKey);
                if (pref != null) {
                    pref.setKey(newKey);
                }
            } else if (intent != null && ACTION_SCROLL_TO_TOP.equals(intent.getAction())) {
                ListView listView = getListView();
                if (listView != null) {
                    listView.setSelection(0);
                }
            }
        }
    };

    // ADD NETWORK
    //
    // The codepath for this is fairly complicated once the user taps "Open on phone":
    // 1. Settings binds to Home.WifiSettingsRelayService with ACTION_REQUEST_WIFI_PASSWORD.
    // 2. Home.WifiSettingsRelayService sends MSG_REQ_WIFI_PASSWORD RPC to Companion.
    // 3. If the user enters an SSID, Companion sends PATH_WIFI_ADD_NETWORK RPC to Home.
    // 4. Home.WifiSettingsListener reads the RPC and then binds back to
    //    Settings.WifiSettingsService with ACTION_CHANGE_WIFI_STATE.
    // 5. Settings.WifiSettingsService invokes WifiManager.addNetwork and
    //    attempts to enable the added network.
    // 6. Once the network is added and connected to, WifiManager broadcasts NETWORK_STATE_CHANGE,
    //    which is received by WifiWaitForPhoneActivity as a signal to stop displaying the spinner.

    private void initAddNetwork(Preference preference) {
        WearableDialogPreference pref = (WearableDialogPreference) preference;
        pref.setNeutralIcon(R.drawable.ic_cc_open_on_phone);
        pref.setNeutralButtonText(R.string.action_open_on_phone);

        pref.setOnDialogClosedListener(button -> {
            if (button == DialogInterface.BUTTON_NEUTRAL) {
                mSelectedAccessPoint = null;
                if (!mServiceConnected) {
                    Intent intent = SettingsIntents.getRequestWifiPasswordIntent();
                    getContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
                } else {
                    requestWifiPassword();
                }
            }
        });
    }

    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "handleMessage msg: " + msg);
            }
            switch (msg.what) {
                case Constants.MSG_REQ_WIFI_PASSWORD_FAILED:
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "No nearby node found. Show error dialog.");
                    }
                    // Clear password input logging event as no password was entered.
                    if (mSelectedAccessPointPref != null) {
                        mSelectedAccessPointPref.setWifiPasswordSource(
                                AccessPointPreference.WifiPasswordSource.NONE);
                    }
                    // Send broadcast to finish WifiWaitForPhoneActivity before showing the error
                    // dialog.
                    getContext().sendBroadcast(
                            new Intent(SettingsIntents.ACTION_REQUEST_WIFI_PASSWORD_DONE));
                    showPhoneAwayErrorDialog();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void showPhoneAwayErrorDialog() {
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.phone_away_error_dialog_title)
                .setMessage(R.string.phone_away_error_dialog_subtitle)
                .setPositiveButton(R.string.generic_ok,
                        (DialogInterface.OnClickListener) (dialog1, which) -> {})
                .create();
        dialog.show();
    }

    private void requestWifiPassword() {
        Message msg = Message.obtain();
        msg.what = Constants.MSG_REQ_WIFI_PASSWORD;
        msg.replyTo = mMessenger;
        Bundle data = new Bundle();
        if (mSelectedAccessPoint != null) {
            data.putString(Constants.WIFI_SSID, mSelectedAccessPoint.getSsidStr());
            data.putInt(Constants.WIFI_SECURITY, mSelectedAccessPoint.getSecurity());

            WifiConfiguration config = mSelectedAccessPoint.getConfig();
            if (config != null) {
                data.putInt(EXTRA_NETWORK_ID, config.networkId);
            }

            if (config != null && config.status == WifiConfiguration.Status.DISABLED) {
                data.putBoolean(EXTRA_DISABLED, true);
                data.putInt(EXTRA_DISABLE_REASON,
                        config.getNetworkSelectionStatus().getNetworkSelectionDisableReason());
            } else {
                data.putBoolean(EXTRA_DISABLED, false);
            }
            data.putBoolean(Constants.WIFI_HIDDEN_NETWORK, false);
        } else {
            data.putBoolean(Constants.WIFI_HIDDEN_NETWORK, true);
        }
        msg.setData(data);
        try {
            mService.send(msg);

            // Launches activity which shows a spinner animation while the user enters the password
            // on their phone.
            Intent intent = new Intent(getContext(), WifiWaitForPhoneActivity.class);
            if (mSelectedAccessPoint != null) {
                intent.putExtra(
                        WifiWaitForPhoneActivity.EXTRA_SSID_KEY, mSelectedAccessPoint.getSsidStr());
            }
            startActivity(intent);
        } catch (RemoteException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        mSelectedAccessPoint = null;
    }
}
