package com.google.android.clockwork.settings.wifi;

import static com.android.settingslib.wifi.AccessPoint.SECURITY_EAP;

import android.annotation.WorkerThread;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.util.PrintWriterPrinter;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.IndentingPrintWriter;
import com.android.settingslib.wifi.WifiTracker;

import com.google.android.clockwork.host.GKeys;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.utils.DefaultBluetoothModeManager;
import com.google.android.clockwork.wifi.AccessPoint;
import com.google.android.clockwork.wifi.Constants;
import com.google.android.clockwork.wifi.WifiUtil;
import com.google.android.collect.Lists;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This service is bound to from Home WifiSettingsListener.
 *
 * Do NOT delete or move this class without making the corresponding changes in Home!
 */
public class WifiSettingsService extends Service implements WifiTracker.WifiListener {
    private static final String TAG = "WifiSettings.Service";

    // This is the broadcast that is sent if we fail to connect to the network within
    // WIFI_ADD_NETWORK_TIMEOUT_SEC seconds of getting an add network request from the companion.
    private static final String ACTION_ADD_NETWORK_TIMEOUT =
        "com.google.android.clockwork.settings.wifi.ACTION_ADD_NETWORK_TIMEOUT";

    private Object mLock = new Object();

    private Looper mServiceLooper;
    private Handler mIncomingHandler;
    private Messenger mMessenger;
    private WifiManager mWifiManager;
    private WifiTracker mWifiTracker;
    private Messenger mWifiUpdatesReplyTo;
    private AlarmManager mAlarmManager;
    private PendingIntent mAddNetworkTimeoutIntent;
    @GuardedBy("mLock")
    private Messenger mWifiAddNetworkReplyTo;
    @GuardedBy("mLock")
    private Bundle mWifiAddNetworkReplyData;

    // How often to send WiFi updates back up to the Home app (note we may send less often than this
    // but we will not send more frequently than this).
    private long mWifiUpdateFrequencySec;
    // The time in millis we last sent a WiFi update to the Home app.
    private long mLastNonEmptyWifiUpdateSentTimeMillis;

    private List<com.android.settingslib.wifi.AccessPoint> mLastWifiAccessPoints;

    private final BroadcastReceiver mReceiver =
        new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                debugLog("onReceive: " + action);
                if (ACTION_ADD_NETWORK_TIMEOUT.equals(action)) {
                    handleAddNetworkTimeout();
                } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                    NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(
                        WifiManager.EXTRA_NETWORK_INFO);
                    handleNetworkStateChangedForAddNetwork(networkInfo);
                }
            }
        };

    @Override
    public void onCreate() {
        super.onCreate();
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();

        mServiceLooper = thread.getLooper();
        mIncomingHandler = new IncomingHandler(mServiceLooper);
        mMessenger = new Messenger(mIncomingHandler);

        mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(ACTION_ADD_NETWORK_TIMEOUT).setPackage(getPackageName());
        mAddNetworkTimeoutIntent =
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_ADD_NETWORK_TIMEOUT);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(mReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        debugLog("onBind: " + intent);

        return mMessenger.getBinder();
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        final IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ");
        ipw.println(TAG);
        ipw.println("#####################################");
        mIncomingHandler.dump(new PrintWriterPrinter(ipw), "");
        if (!"user".equals(Build.TYPE) && args != null && args.length == 3) {
            Message msg = Message.obtain();
            msg.what = Constants.MSG_ADD_NETWORK;
            Bundle data = new Bundle();
            data.putString(Constants.REQUESTER_NODE_ID, "");
            data.putString(Constants.WIFI_SSID, args[0]);
            data.putInt(Constants.WIFI_SECURITY, Integer.valueOf(args[1]));
            data.putString(Constants.WIFI_KEY, args[2]);
            data.putBoolean(Constants.WIFI_HIDDEN_NETWORK, false);
            msg.setData(data);
            msg.replyTo = null;
            mIncomingHandler.sendMessage(msg);
        }
    }

    @WorkerThread
    private void addNetwork(String requesterNodeId, String ssid, int security, String key,
            boolean hiddenSSID, Messenger replyTo) {
        debugLog("addNetwork: ssid=" + ssid + ", security=" + security + ", hidden="
                + hiddenSSID);

        WifiConfiguration conf = null;
        if (security == AccessPoint.SECURITY_NONE) {
            conf = WifiHelper.generateOpenNetworkConfig(ssid);
        } else {
            conf = WifiHelper.generateConfigForSecuredAp(ssid, security, key);
        }

        if (hiddenSSID) {
            conf.hiddenSSID = true;
        }

        if (mWifiManager == null) {
            Log.w(TAG, "Wifi is not available in this device.");
            return;
        }

        Bundle replyData = new Bundle();
        replyData.putString(Constants.REQUESTER_NODE_ID, requesterNodeId);
        replyData.putString(Constants.WIFI_SSID, ssid);
        replyData.putInt(Constants.WIFI_SECURITY, security);

        int networkId = mWifiManager.addNetwork(conf);
        int what = Constants.MSG_ADD_NETWORK_OK;

        // NOTE; networkId being -1 means that addNetwork was not successful.  It is not safe
        // to call enableNetwork on networkId == -1 or it will hang indefinitely.  This is why
        // we have the first check for networkId being -1.
        if (networkId == -1 || !mWifiManager.enableNetwork(networkId, true)) {
            Log.w(TAG, "Failed to enable network: " + conf);
            what = Constants.MSG_ADD_NETWORK_FAILED;
        }

        if (DefaultBluetoothModeManager.INSTANCE.get(this).getBluetoothMode()
            == SettingsContract.BLUETOOTH_MODE_ALT && GKeys.WIFI_ADD_NETWORK_FLOW_FOR_IOS.get()) {
            handleAddNetworkRequestForIos(replyTo, replyData);
        } else {
            sendReply(replyTo, what, replyData);
        }
    }

    private void handleAddNetworkRequestForIos(Messenger replyTo, Bundle replyData) {
        debugLog("handleAddNetworkRequestForIos");

        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        if (wifiInfo != null
            && wifiInfo.getNetworkId() != -1
            && wifiInfo.getSSID().equals(replyData.getString(Constants.WIFI_SSID))) {
            Log.d(TAG, "Already connected to: " + replyData.getString(Constants.WIFI_SSID));
            sendReply(replyTo, Constants.MSG_ADD_NETWORK_OK, replyData);
            return;
        }

        synchronized (mLock) {
            mWifiAddNetworkReplyTo = replyTo;
            mWifiAddNetworkReplyData = replyData;
        }
        setAddNetworkTimeoutAlarm();
    }

    private void handleNetworkStateChangedForAddNetwork(NetworkInfo networkInfo) {
        synchronized (mLock) {
            if (mWifiAddNetworkReplyData == null || mWifiManager == null) {
                // We're not processing an add network request or WiFi is not available on
                // this device, so drop out.
                return;
            }
        }

        debugLog("Got network state change: " + networkInfo.getDetailedState());
        if (networkInfo.getDetailedState() != DetailedState.CONNECTED) {
            return;
        }
        Bundle addNetworkReplyData;
        Messenger addNetworkReplyTo;
        synchronized (mLock) {
            addNetworkReplyData = mWifiAddNetworkReplyData;
            addNetworkReplyTo = mWifiAddNetworkReplyTo;
        }
        String ssid = addNetworkReplyData.getString(Constants.WIFI_SSID);
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        if (wifiInfo == null || wifiInfo.getNetworkId() == -1) {
            return;
        }
        // Trim and remove quotes as the SSID in WifiInfo is surrounded by quotes.
        String wifiInfoSsid = wifiInfo.getSSID().trim().replaceAll("\"", "");
        if (!wifiInfoSsid.equals(ssid)) {
            return;
        }

        debugLog("Connected to: " + ssid);
        cancelAddNetworkAlarm();
        sendReply(
            addNetworkReplyTo, Constants.MSG_ADD_NETWORK_OK, addNetworkReplyData);
        synchronized (mLock) {
            mWifiAddNetworkReplyTo = null;
            mWifiAddNetworkReplyData = null;
        }
    }

    private void handleAddNetworkTimeout() {
        debugLog("handleAddNetworkTimeout");
        synchronized (mLock) {
            if (mWifiAddNetworkReplyData != null) {
                sendReply(
                    mWifiAddNetworkReplyTo, Constants.MSG_ADD_NETWORK_FAILED,
                    mWifiAddNetworkReplyData);
                mWifiAddNetworkReplyData = null;
                mWifiAddNetworkReplyTo = null;
            }
        }
    }

    private void setAddNetworkTimeoutAlarm() {
        debugLog("Setting add network alarm");
        long wakeupTime =
            SystemClock.elapsedRealtime()
                + TimeUnit.SECONDS.toMillis(GKeys.WIFI_ADD_NETWORK_TIMEOUT_SEC.get());
        mAlarmManager.set(
            AlarmManager.ELAPSED_REALTIME_WAKEUP, wakeupTime, mAddNetworkTimeoutIntent);
    }

    private void cancelAddNetworkAlarm() {
        debugLog("Canceling add network alarm");
        mAlarmManager.cancel(mAddNetworkTimeoutIntent);
    }

    @WorkerThread
    private void reportSavedAps(String requesterNodeId, Messenger replyTo) {
        if (TextUtils.isEmpty(requesterNodeId) || replyTo == null) {
            return;
        }

        if (mWifiManager == null) {
            Log.w(TAG, "Wifi is not available in this device.");
            return;
        }
        ArrayList<String> savedAps = new ArrayList<String>();
        final List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();

        int replyWhat = Constants.MSG_REQUEST_SAVED_APS_FAILED;
        Bundle replyData = new Bundle();
        replyData.putString(Constants.REQUESTER_NODE_ID, requesterNodeId);
        if (configs != null) {
            for (WifiConfiguration config : configs) {
                if (config.SSID != null) {
                    savedAps.add(WifiUtil.removeDoubleQuotes(config.SSID));
                }
            }

            replyWhat = Constants.MSG_REQUEST_SAVED_APS_OK;
            replyData.putStringArrayList(Constants.WIFI_SAVED_APS, savedAps);
        }

        sendReply(replyTo, replyWhat, replyData);
    }

    @WorkerThread
    private void forgetSavedAp(Bundle msgData, Messenger replyTo) {
        String requesterNodeId = msgData.getString(Constants.REQUESTER_NODE_ID);
        String networkToForget = msgData.getString(Constants.WIFI_AP_TO_FORGET);

        debugLog(String.format("Received request to forget %s by %s ",
            networkToForget, requesterNodeId));

        int replyWhat = Constants.MSG_FORGET_NETWORK_FAILED;
        Bundle replyData = new Bundle();
        replyData.putString(Constants.REQUESTER_NODE_ID, requesterNodeId);
        if (mWifiManager == null) {
            Log.w(TAG, "Wifi is not available in this device.");
            return;
        }
        final List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();
        if (configs != null) {
            // If the network to forget is part of the saved network, call WifiManager to remove it.
            for (WifiConfiguration config : configs) {
                if (!TextUtils.isEmpty(config.SSID) && TextUtils.equals(networkToForget,
                        WifiUtil.removeDoubleQuotes(config.SSID))) {
                    if (mWifiManager.removeNetwork(config.networkId)) {
                        replyWhat = Constants.MSG_FORGET_NETWORK_OK;
                    }
                    break;
                }
            }

        }

        sendReply(replyTo, replyWhat, replyData);
    }

    private void sendReply(Messenger replyTo, int what, @Nullable Bundle data) {
        if (replyTo != null) {
            Message reply = Message.obtain();
            reply.what = what;
            if(data != null) {
                reply.setData(data);
            }
            try {
                replyTo.send(reply);
            } catch (RemoteException e) {
                Log.w(TAG, "reply could not be sent", e);
            }
        } else {
            Log.w(TAG, "reply to is null");
        }
    }

    @Override
    public void onWifiStateChanged(int state) {
    }

    @Override
    public void onConnectedChanged() {
    }

    @Override
    public void onAccessPointsChanged() {
        if (System.currentTimeMillis() <=
            mLastNonEmptyWifiUpdateSentTimeMillis
                + TimeUnit.SECONDS.toMillis(mWifiUpdateFrequencySec)) {
            Log.d(TAG, "Not sending access points to companion as we recently sent an update");
            return;
        }

        List<com.android.settingslib.wifi.AccessPoint> accessPoints =
            filterAccessPoints(mWifiTracker.getAccessPoints());
        debugLog("Access points: " + accessPoints);
        if (!accessPointsHaveChanged(accessPoints)) {
            Log.d(TAG, "Not sending redundant access points to companion: " + accessPoints);
            return;
        }

        // TODO(b/68308400): Look into why WiFiTracker sometimes returns an empty access point list
        // even though there are networks in range.
        if (accessPoints.size() > 0) {
            mLastNonEmptyWifiUpdateSentTimeMillis = System.currentTimeMillis();
        }
        mLastWifiAccessPoints = accessPoints;

        ArrayList<Bundle> bundles = getWifiAccessPointBundles(accessPoints);

        Bundle replyData = new Bundle();
        replyData.putParcelableArrayList(Constants.WIFI_ACCESS_POINTS, bundles);

        sendReply(mWifiUpdatesReplyTo, Constants.MSG_WIFI_ACCESS_POINTS, replyData);
    }

    private List<com.android.settingslib.wifi.AccessPoint> filterAccessPoints(
        List<com.android.settingslib.wifi.AccessPoint> accessPoints) {
        List<com.android.settingslib.wifi.AccessPoint> filteredAccessPoints = Lists.newArrayList();
        for (com.android.settingslib.wifi.AccessPoint accessPoint : accessPoints) {
            if (!accessPoint.isReachable() || accessPoint.getSecurity() == SECURITY_EAP) {
                Log.d(TAG, "Skipping unreachable or eap security access point: " + accessPoint);
                continue;
            }
            filteredAccessPoints.add(accessPoint);
        }
        return filteredAccessPoints;
    }

    private boolean accessPointsHaveChanged(
        List<com.android.settingslib.wifi.AccessPoint> accessPoints) {
        if (mLastWifiAccessPoints == null  || mLastWifiAccessPoints.size() != accessPoints.size()) {
            return true;
        }

        for (int i = 0; i < mLastWifiAccessPoints.size(); i++) {
            com.android.settingslib.wifi.AccessPoint oldAccessPoint = mLastWifiAccessPoints.get(i);
            com.android.settingslib.wifi.AccessPoint newAccessPoint = accessPoints.get(i);
            if (!oldAccessPoint.getSsidStr().equals(newAccessPoint.getSsidStr())
                || oldAccessPoint.getSecurity() != newAccessPoint.getSecurity()
                || oldAccessPoint.getLevel() != newAccessPoint.getLevel()
                || oldAccessPoint.isActive() != newAccessPoint.isActive()
                || oldAccessPoint.isSaved() != newAccessPoint.isSaved()) {
                return true;
            }
        }
        return false;
    }

    private ArrayList<Bundle> getWifiAccessPointBundles(
        List<com.android.settingslib.wifi.AccessPoint> accessPoints) {
        ArrayList<Bundle> bundles = Lists.newArrayList();
        for (com.android.settingslib.wifi.AccessPoint accessPoint : accessPoints) {
            Bundle bundle = new Bundle();
            bundle.putString(Constants.WIFI_SSID, accessPoint.getSsidStr());
            bundle.putInt(Constants.WIFI_SECURITY, accessPoint.getSecurity());
            bundle.putInt(Constants.WIFI_LEVEL, accessPoint.getLevel());
            bundle.putBoolean(Constants.WIFI_ACTIVE, accessPoint.isActive());
            bundle.putBoolean(Constants.WIFI_SAVED, accessPoint.isSaved());
            bundles.add(bundle);
        }
        return bundles;
    }

    private class IncomingHandler extends Handler {
        IncomingHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            debugLog("handleMessage msg: " + msg);

            switch (msg.what) {
                case Constants.MSG_ADD_NETWORK:
                    Bundle data = msg.getData();
                    addNetwork(data.getString(Constants.REQUESTER_NODE_ID),
                            data.getString(Constants.WIFI_SSID),
                            data.getInt(Constants.WIFI_SECURITY, AccessPoint.SECURITY_NONE),
                            data.getString(Constants.WIFI_KEY),
                            data.getBoolean(Constants.WIFI_HIDDEN_NETWORK),
                            msg.replyTo);
                    break;
                case Constants.MSG_REQUEST_SAVED_APS:
                    reportSavedAps(msg.getData().getString(Constants.REQUESTER_NODE_ID),
                            msg.replyTo);
                    break;
                case Constants.MSG_FORGET_NETWORK:
                    forgetSavedAp(msg.getData(), msg.replyTo);
                    break;
                case Constants.MSG_START_WIFI_UPDATES:
                    startWifiUpdates(msg.getData().getLong(Constants.WIFI_UPDATE_FREQUENCY_SECONDS),
                        msg.replyTo);
                    break;
                case Constants.MSG_STOP_WIFI_UPDATES:
                    stopWifiUpdates();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void stopWifiUpdates() {
        debugLog("stopWifiUpdates");
        if (mWifiTracker != null) {
            mWifiTracker.onStop();
            mWifiTracker.onDestroy();
            mWifiTracker = null;
            mLastNonEmptyWifiUpdateSentTimeMillis = 0;
            mLastWifiAccessPoints = null;
        }
    }

    private void startWifiUpdates(long updateFrequencySec, Messenger replyTo) {
        debugLog("startWifiUpdates");
        if (mWifiTracker == null) {
            mWifiTracker = new WifiTracker(
                this, this, true, true);
            mWifiTracker.onStart();
            mWifiUpdatesReplyTo = replyTo;
            mWifiUpdateFrequencySec = (updateFrequencySec > 0) ? updateFrequencySec
                : GKeys.WIFI_UPDATES_DEFAULT_FREQUENCY_SEC.get();
        }
    }

    private void debugLog(String message) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, message);
        }
    }
}
