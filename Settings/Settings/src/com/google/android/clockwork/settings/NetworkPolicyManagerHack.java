package com.google.android.clockwork.settings;

import android.annotation.SystemApi;

/**
 * Hack copy of NetworkPolicyManager's constants added for http://b/23120366.
 * TODO: Delete this file when framework's NetworkPolicyManager is updated.
 */
public class NetworkPolicyManagerHack {
    /**
     * Broadcast intent action for informing a custom component about a network policy
     * notification.
     * @hide
     */
    @SystemApi
    public static final String ACTION_SHOW_NETWORK_POLICY_NOTIFICATION =
            "android.net.action.SHOW_NETWORK_POLICY_NOTIFICATION";

    /**
     * The sequence number associated with the notification - a higher number
     * indicates previous notifications may be disregarded.
     * @hide
     */
    @SystemApi
    public static final String EXTRA_NOTIFICATION_SEQUENCE_NUMBER =
            "android.net.extra.NOTIFICATION_SEQUENCE_NUMBER";

    /**
     * The type of notification that should be presented to the user.
     * @hide
     */
    @SystemApi
    public static final String EXTRA_NOTIFICATION_TYPE = "android.net.extra.NOTIFICATION_TYPE";

    @SystemApi
    public static final int NOTIFICATION_TYPE_NONE = 0;
    @SystemApi
    public static final int NOTIFICATION_TYPE_USAGE_WARNING = 1;
    @SystemApi
    public static final int NOTIFICATION_TYPE_USAGE_REACHED_LIMIT = 2;
    @SystemApi
    public static final int NOTIFICATION_TYPE_USAGE_EXCEEDED_LIMIT = 3;

    /**
     * The number of bytes used on the network in the notification.
     * @hide
     */
    @SystemApi
    public static final String EXTRA_BYTES_USED = "android.net.extra.BYTES_USED";

    /**
     * The network policy for the network in the notification.
     * @hide
     */
    @SystemApi
    public static final String EXTRA_NETWORK_POLICY = "android.net.extra.NETWORK_POLICY";

}
