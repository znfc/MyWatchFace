package com.google.android.clockwork.settings.connectivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.INetworkPolicyManager;
import android.net.NetworkPolicy;
import android.net.NetworkTemplate;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.format.Time;
import android.util.Log;
import com.google.android.clockwork.settings.SettingsContract;

/**
 * Listens to configure proxy intent from home and configure proxy connection as metered if paired
 * with iOS.
 */
public class BluetoothConfigureProxyReceiver extends BroadcastReceiver {
    private static final String TAG = "BluetoothConfigureProxyReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Configure proxy connection as metered when paired with iOS.  Check the bluetooth mode
        // for iOS pairing confirmation.
        final int bluetoothMode = SettingsContract.getIntValueForKey(
                context.getContentResolver(),
                SettingsContract.BLUETOOTH_MODE_URI,
                SettingsContract.KEY_BLUETOOTH_MODE,
                SettingsContract.BLUETOOTH_MODE_UNKNOWN);

        if (bluetoothMode != SettingsContract.BLUETOOTH_MODE_ALT) {
            Log.i(TAG, "Not paired with iOS. Will not configure proxy connection as metered.");
            return;
        }

        try {
            final INetworkPolicyManager policyManager = INetworkPolicyManager.Stub.asInterface(
                ServiceManager.getService(Context.NETWORK_POLICY_SERVICE));
            NetworkPolicy[] policies = policyManager.getNetworkPolicies(null);
            NetworkPolicy proxyPolicy = null;

            // Find network policy for proxy if one exists.
            for (final NetworkPolicy policy : policies) {
                if (policy.template.getMatchRule() == NetworkTemplate.MATCH_PROXY) {
                    proxyPolicy = policy;
                    break;
                }
            }

            // Add a new network policy for proxy if not found.
            if (proxyPolicy == null) {
                final NetworkPolicy[] newPolicies = new NetworkPolicy[policies.length + 1];
                System.arraycopy(policies, 0, newPolicies, 1, policies.length);
                policies = newPolicies;
                policies[0] = proxyPolicy = new NetworkPolicy(NetworkTemplate.buildTemplateProxy(),
                        NetworkPolicy.CYCLE_NONE, Time.TIMEZONE_UTC, NetworkPolicy.WARNING_DISABLED,
                        NetworkPolicy.LIMIT_DISABLED, NetworkPolicy.SNOOZE_NEVER,
                        NetworkPolicy.SNOOZE_NEVER, false, false);
            }

            if (!proxyPolicy.metered) {
                Log.i(TAG, "Network policy for proxy configured as metered.");
                proxyPolicy.metered = true;
                policyManager.setNetworkPolicies(policies);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Remote exception setting network policy.", e);
        }
    }
}
