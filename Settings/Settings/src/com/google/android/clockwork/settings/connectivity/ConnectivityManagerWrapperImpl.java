package com.google.android.clockwork.settings.connectivity;

import android.net.ConnectivityManager;
import android.net.ProxyInfo;

public class ConnectivityManagerWrapperImpl implements ConnectivityManagerWrapper {
    private final ConnectivityManager mConnectivityManager;

    public ConnectivityManagerWrapperImpl(ConnectivityManager connectivityManager) {
        mConnectivityManager = connectivityManager;
    }

    @Override
    public ConnectivityManager getConnectivityManager() {
        return mConnectivityManager;
    }

    @Override
    public boolean isAlwaysOnVpnSet(int userId) {
        return mConnectivityManager.getAlwaysOnVpnPackageForUser(userId) != null;
    }

    @Override
    public ProxyInfo getGlobalProxy() {
        return mConnectivityManager.getGlobalProxy();
    }
}
