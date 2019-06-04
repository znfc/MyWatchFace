package com.google.android.clockwork.settings.cellular;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import com.android.internal.telephony.PhoneConstants;
import com.google.android.clockwork.phone.common.Constants;
import com.google.android.clockwork.settings.cellular.SetNumberService;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.SettingsIntents;

import static com.google.android.clockwork.phone.common.Constants.GLOBAL_CALL_TWINNING_STATE_KEY;
import static com.google.android.clockwork.phone.common.Constants.GLOBAL_TEXT_TWINNING_STATE_KEY;
import static com.google.android.clockwork.phone.common.Constants.GLOBAL_TEXT_BRIDGING_STATE_KEY;
import static com.google.android.clockwork.phone.common.Constants.STATE_ON;
import static com.google.android.clockwork.phone.common.Constants.STATE_OFF;

public class Utils {
    /** The intent to fire to apply updated settings. See the CellBroadcaster package. */
    private static final String ACTION_ENABLE_CHANNELS =
            "com.google.android.clockwork.cmas.ENABLE_CHANNELS";

    public static final boolean isCallTwinningEnabled(Context context) {
        return isCallTwinningEnabled(context.getContentResolver());
    }

    public static final boolean isCallTwinningEnabled(ContentResolver resolver) {
        return Settings.Global.getInt(
                resolver,
                GLOBAL_CALL_TWINNING_STATE_KEY,
                STATE_OFF
        ) == STATE_ON;
    }

    public static final void setCallTwinningState(
            Context context,
            int state,
            String operator,
            String voicemailNumber) {

        Settings.Global.putInt(
                context.getContentResolver(),
                GLOBAL_CALL_TWINNING_STATE_KEY,
                state
        );

        // Legacy setting
        Settings.Global.putInt(
                context.getContentResolver(),
                "twinning_state",
                state
        );

        Settings.Global.putString(
                context.getContentResolver(),
                Constants.GLOBAL_OPERATOR_KEY,
                operator
        );

        Boolean enableHfp = null;

        if (state == Constants.STATE_ON) {
            // Always disable HFP when in twinning mode.
            enableHfp = false;
        } else {
            // Restore original user setting if previously set to true.
            int userSetting = SettingsContract.getIntValueForKey(context.getContentResolver(),
                    SettingsContract.BLUETOOTH_URI, SettingsContract.KEY_USER_HFP_CLIENT_SETTING,
                    SettingsContract.HFP_CLIENT_DISABLED);

            if (userSetting == SettingsContract.HFP_CLIENT_ENABLED) {
                enableHfp = true;
            }
        }

        if (enableHfp != null) {
            context.sendBroadcast(SettingsIntents.getEnableHFPIntent(enableHfp, false));
        }

        Intent setVoicemailIntent;
        if (voicemailNumber != null) {
            setVoicemailIntent = new Intent(SetNumberService.ACTION_SET_VOICEMAIL_NUMBER);
            setVoicemailIntent.putExtra(SetNumberService.EXTRA_NEW_NUMBER, voicemailNumber);
            setVoicemailIntent.putExtra(SetNumberService.EXTRA_IS_OVERRIDE, true);
        } else {
            setVoicemailIntent = new Intent(SetNumberService.ACTION_RESTORE_VOICEMAIL_NUMBER);
        }
        setVoicemailIntent.setClass(context.getApplicationContext(), SetNumberService.class);
        context.getApplicationContext().startService(setVoicemailIntent);
    }

    public static final boolean isTextTwinningEnabled(Context context) {
        return Settings.Global.getInt(
                context.getContentResolver(),
                GLOBAL_TEXT_TWINNING_STATE_KEY,
                STATE_OFF
        ) == STATE_ON;
    }

    public static final void setTextTwinningState(Context context, int state) {
        Settings.Global.putInt(
                context.getContentResolver(),
                GLOBAL_TEXT_TWINNING_STATE_KEY,
                state
        );
    }

    public static final boolean isTextBridgingEnabled(Context context) {
        return Settings.Global.getInt(
                context.getContentResolver(),
                GLOBAL_TEXT_BRIDGING_STATE_KEY,
                STATE_ON
        ) == STATE_ON;
    }

    public static final void setTextBridgingState(Context context, int state) {
        Settings.Global.putInt(
                context.getContentResolver(),
                GLOBAL_TEXT_BRIDGING_STATE_KEY,
                state
        );
    }

    public static final boolean getBooleanProperty(Context context, final String property) {
        final int subId = SubscriptionManager.getDefaultSubscriptionId();
        return SubscriptionManager.getBooleanSubscriptionProperty(subId, property, true, context);
    }

    public static final void setBooleanProperty(Context context, final String property, boolean value) {
        final int subId = SubscriptionManager.getDefaultSubscriptionId();
        final String strValue = value ? "1" : "0";
        SubscriptionManager.setSubscriptionProperty(subId, property, strValue);
        Utils.sendBroadcast(context, subId);
    }

    public static final int getIntegerProperty(Context context, final String property) {
        final int subId = SubscriptionManager.getDefaultSubscriptionId();
        return SubscriptionManager.getIntegerSubscriptionProperty(subId, property, 0, context);
    }

    public static final void setIntegerProperty(Context context, final String property, int value) {
        final int subId = SubscriptionManager.getDefaultSubscriptionId();
        final String strValue = String.valueOf(value);
        SubscriptionManager.setSubscriptionProperty(subId, property, strValue);
        Utils.sendBroadcast(context, subId);
    }

    private static final void sendBroadcast(Context context, int subId) {
       final Intent intent = new Intent(ACTION_ENABLE_CHANNELS);
       intent.setComponent(SettingsIntents.CELL_BROADCAST_CONFIG_SERVICE_COMPONENT);
       intent.putExtra(PhoneConstants.SUBSCRIPTION_KEY, subId);
       context.startService(intent);
   }
}
