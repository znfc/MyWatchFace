package com.google.android.clockwork.settings.cellular;
import static com.google.android.clockwork.settings.cellular.Constants.EXTRA_IS_PUK_PIN;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.IccCardConstants;
import com.google.android.apps.wearable.settings.R;


/**
 * A util class to send notification of the SIM state.
 */
public class SimStateNotification {
    public static final String TAG = SimStateNotification.class.getSimpleName();
    private static final int NOTIFICATION_ID = 1;

    private static final String PUK_MAGIC = "**05*";

    private SimStateNotification() {}

    public static void maybeCreateNotification(Context context) {
        cancelNotification(context);
        TelephonyManager tm = (TelephonyManager) context.getSystemService(
                Context.TELEPHONY_SERVICE);
        final int simState = tm.getSimState();
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "SIM state:" + simState);
        }

        if (shouldCreateNotification(simState)) {
            createNotification(context, simState);
        }
    }

    static int translateSimState(final String simState, final String lockedReason) {
        if (IccCardConstants.INTENT_VALUE_ICC_UNKNOWN.equals(simState)) {
            return TelephonyManager.SIM_STATE_UNKNOWN;
        } else if (IccCardConstants.INTENT_VALUE_ICC_NOT_READY.equals(simState)) {
            return TelephonyManager.SIM_STATE_NOT_READY;
        } else if (IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(simState)) {
            return TelephonyManager.SIM_STATE_ABSENT;
        } else if (IccCardConstants.INTENT_VALUE_ICC_CARD_IO_ERROR.equals(simState)) {
            return TelephonyManager.SIM_STATE_CARD_IO_ERROR;
        } else if (IccCardConstants.INTENT_VALUE_ICC_READY.equals(simState)
                || IccCardConstants.INTENT_VALUE_ICC_IMSI.equals(simState)
                || IccCardConstants.INTENT_VALUE_ICC_LOADED.equals(simState)) {
            // IMSI and LOADED are treated as READY. See bug: b/7197471
            return TelephonyManager.SIM_STATE_READY;
        } else if (IccCardConstants.INTENT_VALUE_ICC_LOCKED.equals(simState)) {
            if (IccCardConstants.INTENT_VALUE_LOCKED_ON_PIN.equals(lockedReason)) {
                return TelephonyManager.SIM_STATE_PIN_REQUIRED;
            } else if (IccCardConstants.INTENT_VALUE_LOCKED_ON_PUK.equals(lockedReason)) {
                return TelephonyManager.SIM_STATE_PUK_REQUIRED;
            } else if (IccCardConstants.INTENT_VALUE_LOCKED_NETWORK.equals(lockedReason)) {
                return TelephonyManager.SIM_STATE_NETWORK_LOCKED;
            } else if (IccCardConstants.INTENT_VALUE_ABSENT_ON_PERM_DISABLED.equals(lockedReason)) {
                return TelephonyManager.SIM_STATE_PERM_DISABLED;
            }
        }
        return TelephonyManager.SIM_STATE_UNKNOWN;
    }

    static boolean shouldCreateNotification(final int simState) {
        return simState == TelephonyManager.SIM_STATE_PIN_REQUIRED
                || simState == TelephonyManager.SIM_STATE_PUK_REQUIRED
                || simState == TelephonyManager.SIM_STATE_CARD_IO_ERROR
                || simState == TelephonyManager.SIM_STATE_NETWORK_LOCKED
                || simState == TelephonyManager.SIM_STATE_PERM_DISABLED;
    }

    static void createNotification(final Context context, int simState) {
        final Resources resources = context.getResources();
        Notification.Builder builder = new Notification.Builder(context);
        builder.setCategory(Notification.CATEGORY_SYSTEM)
                .setLocalOnly(true)
                .setPriority(Notification.PRIORITY_MAX)
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setSmallIcon(R.drawable.ic_sys_nt_sim);
        if (simState == TelephonyManager.SIM_STATE_PIN_REQUIRED) {
            builder.setContentTitle(resources.getString(R.string.sim_locked_notification_title));
            builder.setContentText(resources.getString(R.string.sim_locked_notification_summary));
            builder.addAction(buildPinRequiredAction(context));
            builder.addAction(buildDismissAction(context));
            builder.setOngoing(true);
            builder.extend(new Notification.WearableExtender()
                    .setBackground(BitmapFactory.decodeResource(resources,
                            R.drawable.bg_locked_sim))
                    .setHintHideIcon(true)
                    .setContentAction(0));
        } else if (simState == TelephonyManager.SIM_STATE_PUK_REQUIRED) {
            builder.setContentTitle(resources.getString(R.string.sim_locked_notification_title));
            builder.setContentText(resources.getString(R.string.puk_required_notification_summary));
            builder.addAction(buildPukRequiredAction(context));
            builder.addAction(buildDismissAction(context));
            builder.setOngoing(true);
            builder.extend(new Notification.WearableExtender()
                    .setBackground(BitmapFactory.decodeResource(resources,
                            R.drawable.bg_alert_sim))
                    .setHintHideIcon(true)
                    .setContentAction(0));
        } else if (simState == TelephonyManager.SIM_STATE_CARD_IO_ERROR
                || simState == TelephonyManager.SIM_STATE_NETWORK_LOCKED
                || simState == TelephonyManager.SIM_STATE_PERM_DISABLED) {
            int titleRes = R.string.sim_invalid_notification_title;
            if (simState == TelephonyManager.SIM_STATE_PERM_DISABLED) {
                titleRes = R.string.perm_locked_notification_title;
            }
            builder.setContentTitle(resources.getString(titleRes));
            builder.extend(new Notification.WearableExtender()
                    .setBackground(BitmapFactory.decodeResource(resources,
                            R.drawable.bg_alert_sim)));
        }
        NotificationManager notificationManager = NotificationManager.from(context);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    static void cancelNotification(Context context) {
        NotificationManager notificationManager = NotificationManager.from(context);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private static Notification.Action buildPinRequiredAction(final Context context) {
        Intent resultIntent = new Intent(context, SimUnlockActivity.class);
        resultIntent.putExtra(EXTRA_IS_PUK_PIN, false);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        Notification.Action.Builder actionBuilder =
                new Notification.Action.Builder(
                        R.drawable.ic_lock_permissions,
                        context.getResources().getString(R.string.sim_unlock_action_title),
                        resultPendingIntent);
        return actionBuilder.build();
    }

    private static Notification.Action buildPukRequiredAction(final Context context) {
        Intent resultIntent = new Intent(context, SimUnlockActivity.class);
        resultIntent.putExtra(EXTRA_IS_PUK_PIN, true);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        Notification.Action.Builder actionBuilder =
                new Notification.Action.Builder(
                        R.drawable.ic_lock_permissions,
                        context.getResources().getString(R.string.sim_unlock_action_title),
                        resultPendingIntent);
        return actionBuilder.build();
    }

    private static Notification.Action buildDismissAction(final Context context) {
        Intent intent = new Intent(context, SimStateIntentService.class);
        intent.setAction(SimStateIntentService.DISMISS_NOTIFICATION);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);

        Notification.Action.Builder actionBuilder =
                new Notification.Action.Builder(
                        R.drawable.ic_cc_cancel,
                        context.getResources().getString(R.string.sim_dismiss_action_title),
                        pendingIntent);
        return actionBuilder.build();
    }
}
