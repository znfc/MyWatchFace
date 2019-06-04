package com.google.android.clockwork.settings;

import static com.google.android.clockwork.settings.NetworkPolicyManagerHack.ACTION_SHOW_NETWORK_POLICY_NOTIFICATION;
import static com.google.android.clockwork.settings.NetworkPolicyManagerHack.EXTRA_BYTES_USED;
import static com.google.android.clockwork.settings.NetworkPolicyManagerHack.EXTRA_NETWORK_POLICY;
import static com.google.android.clockwork.settings.NetworkPolicyManagerHack.EXTRA_NOTIFICATION_TYPE;
import static com.google.android.clockwork.settings.NetworkPolicyManagerHack.NOTIFICATION_TYPE_NONE;
import static com.google.android.clockwork.settings.NetworkPolicyManagerHack.NOTIFICATION_TYPE_USAGE_EXCEEDED_LIMIT;
import static com.google.android.clockwork.settings.NetworkPolicyManagerHack.NOTIFICATION_TYPE_USAGE_REACHED_LIMIT;
import static com.google.android.clockwork.settings.NetworkPolicyManagerHack.NOTIFICATION_TYPE_USAGE_WARNING;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.net.INetworkPolicyManager;
import android.net.NetworkPolicy;
import android.net.NetworkPolicyManager;
import android.net.NetworkTemplate;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.text.format.Formatter;
import android.util.Log;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.common.content.CwPrefs;
import com.google.android.clockwork.settings.connectivity.cellular.CellularSettingsActivity;

/**
 * Receives network policy notifications from the system and displays
 * them in a format suitable for Wear.
 */
public class NetworkPolicyNotificationIntentService extends IntentService {
    private final static String TAG = "WearDataUsage";

    public final static String ACTION_CLEAR_STORED_DISMISSALS =
            "com.google.android.clockwork.settings.action.CLEAR_STORED_DISMISSALS";

    private final static String ACTION_DISMISS_NOTIFICATION =
            "com.google.android.clockwork.settings.action.DISMISS_NOTIFICATION";

    private final static String NOTIFICATION_TAG = "wear:network_usage";
    private final static int NOTIFICATION_USAGE_ID = 1;

    private final static String PREF_FILE_NAME = "network_usage";
    private final static String CAN_RESUME_DATA_KEY = "can_resume_data";
    private final static String REACHED_DISMISS_TIME_KEY = "reached_dismiss_time_key";
    private final static String EXCEEDED_DISMISS_TIME_KEY = "exceeded_dismiss_time_key";
    private final static String EXCEEDED_DISMISS_BYTES_KEY = "exceeded_dismiss_bytes_key";

    /**
     * The repeating threshold at which to show the "exceeded data usage" notification again
     */
    private final static long EXCEEDED_DEJA_VU_THRESHOLD_BYTES = 100 * 1024 * 1024;

    public NetworkPolicyNotificationIntentService() {
        super("NetworkPolicyNotifications");
    }

    // Needs to be a global to be readable accross process bounds (phone & settings)
    public static boolean dataIsResumable(Context context) {
        return Settings.Global.getInt(
            context.getContentResolver(), CAN_RESUME_DATA_KEY, 0) != 0;
    }

    public static void notifyDataResumed(Context context) {
        writeCanResumeData(context, false);
    }

    @Override
    public void onHandleIntent(Intent intent) {
        final int type = intent.getIntExtra(EXTRA_NOTIFICATION_TYPE, NOTIFICATION_TYPE_NONE);
        final NetworkPolicy policy = intent.getParcelableExtra(EXTRA_NETWORK_POLICY);

        switch (intent.getAction()) {
            case ACTION_SHOW_NETWORK_POLICY_NOTIFICATION:
              handleShowNotification(intent, type, policy);
              break;
            case ACTION_DISMISS_NOTIFICATION:
              handleDismissNotification(intent, type, policy);
              break;
            case ACTION_CLEAR_STORED_DISMISSALS:
              clearStoredDismissals();
              break;
        }
    }

    private void handleDismissNotification(Intent intent, int type, NetworkPolicy policy) {
        final INetworkPolicyManager policyService = INetworkPolicyManager.Stub.asInterface(
                ServiceManager.getService(Context.NETWORK_POLICY_SERVICE));

        switch (type) {
            case NOTIFICATION_TYPE_USAGE_WARNING:
                // TODO: Uncomment when framework's NetworkPolicyManager is updated.
                // http://b/23120366
                /*
                try {
                    policyService.snoozeWarning(policy.template);
                } catch (RemoteException e) {
                    Log.w(TAG, "problem snoozing warning", e);
                }
                */
                break;
            case NOTIFICATION_TYPE_USAGE_REACHED_LIMIT:
                getPreferences(this)
                    .edit()
                    .putLong(REACHED_DISMISS_TIME_KEY, System.currentTimeMillis())
                    .commit();
                break;
            case NOTIFICATION_TYPE_USAGE_EXCEEDED_LIMIT:
                getPreferences(this)
                    .edit()
                    .putLong(EXCEEDED_DISMISS_TIME_KEY, System.currentTimeMillis())
                    .putLong(EXCEEDED_DISMISS_BYTES_KEY, intent.getLongExtra(EXTRA_BYTES_USED, 0))
                    .commit();
                break;
        }
    }

    private void handleShowNotification(Intent intent, int type, NetworkPolicy policy) {
        writeCanResumeData(this, type == NOTIFICATION_TYPE_USAGE_REACHED_LIMIT);

        if (type == NOTIFICATION_TYPE_NONE) {
            clearNotification();
            return;
        }

        if (!policy.template.isMatchRuleMobile()) {
            Log.e(TAG, "Network type is not mobile.");
            return;
        }

        SharedPreferences preferences = getPreferences(this);
        Resources resources = getResources();
        Notification.Builder builder = new Notification.Builder(this);
        String title = null;

        switch (type) {
            case NOTIFICATION_TYPE_USAGE_WARNING:
                title = resources.getString(R.string.data_usage_warning_title);
                break;
            case NOTIFICATION_TYPE_USAGE_REACHED_LIMIT:
                if (withinCycleBoundary(this, policy, REACHED_DISMISS_TIME_KEY)) {
                    clearNotification();
                    return;
                }

                title = resources.getString(R.string.data_usage_reached_limit_title);
                builder.setContentText(resources.getString(R.string.data_usage_reached_limit_text));
                builder.addAction(
                        R.drawable.ic_full_data_usage,
                        resources.getString(R.string.pref_resumeData),
                        PendingIntent.getActivity(
                                this, 0, new Intent(this, CellularSettingsActivity.class), 0));
                break;
            case NOTIFICATION_TYPE_USAGE_EXCEEDED_LIMIT: {
                long bytesUsed = intent.getLongExtra(EXTRA_BYTES_USED, 0);
                long bytesOver = bytesUsed - policy.limitBytes;
                long lastDismissBytesUsed = preferences.getLong(EXCEEDED_DISMISS_BYTES_KEY, 0);

                if (withinCycleBoundary(this, policy, EXCEEDED_DISMISS_TIME_KEY)
                        && lastDismissBytesUsed < (bytesUsed + EXCEEDED_DEJA_VU_THRESHOLD_BYTES)) {
                    clearNotification();
                    return;
                }

                title = resources.getString(R.string.data_usage_exceeded_limit_title);
                builder.setContentText(
                    resources.getString(
                        R.string.data_usage_exceeded_limit_text,
                        Formatter.formatFileSize(this, bytesOver)
                    )
                );
                break;
            }
            default:
                clearNotification();
                return;
        }

        Intent deleteIntent = new Intent(this, NetworkPolicyNotificationReceiver.class);
        deleteIntent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        deleteIntent.setAction(ACTION_DISMISS_NOTIFICATION);
        deleteIntent.putExtra(EXTRA_NOTIFICATION_TYPE, type);
        deleteIntent.putExtra(EXTRA_NETWORK_POLICY, policy);
        deleteIntent.putExtra(EXTRA_BYTES_USED, intent.getLongExtra(EXTRA_BYTES_USED, 0));

        builder.setDeleteIntent(PendingIntent.getBroadcast(this, 0, deleteIntent, 0));
        builder.setLocalOnly(true);
        builder.setPriority(Notification.PRIORITY_MAX);
        builder.setSmallIcon(R.drawable.ic_sys_nt_data_limit);
        builder.setContentTitle(title);
        builder.extend(
            new Notification.WearableExtender().setBackground(
                BitmapFactory.decodeResource(
                    resources,
                    R.drawable.bg_cellular_connection
                )
            )
        );

        getSystemService(NotificationManager.class)
            .notify(NOTIFICATION_TAG, NOTIFICATION_USAGE_ID, builder.build());
    }

    private void clearStoredDismissals() {
        Log.d(TAG, "Clearing stored dismissals");

        getPreferences(this)
            .edit()
            .remove(REACHED_DISMISS_TIME_KEY)
            .remove(EXCEEDED_DISMISS_TIME_KEY)
            .remove(EXCEEDED_DISMISS_BYTES_KEY)
            .commit();

        writeCanResumeData(this, false);
    }

    private void clearNotification() {
        getSystemService(NotificationManager.class)
            .cancel(NOTIFICATION_TAG, NOTIFICATION_USAGE_ID);
    }

    private static void writeCanResumeData(Context context, boolean value) {
        Settings.Global.putInt(
            context.getContentResolver(),
            CAN_RESUME_DATA_KEY,
            value ? 1 : 0);
    }

    private static boolean withinCycleBoundary(
            Context context,
            NetworkPolicy policy,
            String dismissKey) {
        return getPreferences(context).getLong(dismissKey, 0) > getLastCycleBoundary(policy);
    }

    private static long getLastCycleBoundary(NetworkPolicy policy) {
        return NetworkPolicyManager.cycleIterator(policy).next().first.toInstant().toEpochMilli();
    }

    private static SharedPreferences getPreferences(Context context) {
        return CwPrefs.wrap(context, PREF_FILE_NAME);
    }
}
