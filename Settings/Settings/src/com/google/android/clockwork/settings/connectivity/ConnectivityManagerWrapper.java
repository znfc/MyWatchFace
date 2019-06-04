package com.google.android.clockwork.settings.connectivity;

import android.net.ConnectivityManager;
import android.net.ProxyInfo;

/**
 * This interface replicates a subset of the android.net.ConnectivityManager (CM). The interface
 * exists so that we can use a thin wrapper around the CM in production code and a mock in tests.
 * We cannot directly mock or shadow the CM, because some of the methods we rely on are marked as
 * hidden and are thus invisible to Robolectric.
 */
public interface ConnectivityManagerWrapper {

    /**
     * Returns the real ConnectivityManager object wrapped by this wrapper.
     */
    public ConnectivityManager getConnectivityManager();

    /**
     * Calls {@code ConnectivityManager.getAlwaysOnVpnPackageForUser()}.
     *
     * @see android.net.ConnectivityManager#getAlwaysOnVpnPackageForUser
     */
    boolean isAlwaysOnVpnSet(int userId);

    /**
     * Calls {@code ConnectivityManager.getGlobalProxy()}.
     *
     * @see android.net.ConnectivityManager#getGlobalProxy
     */
    ProxyInfo getGlobalProxy();
}
