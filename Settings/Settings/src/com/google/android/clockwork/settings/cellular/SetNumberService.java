package com.google.android.clockwork.settings.cellular;

import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.ResultReceiver;
import android.util.Log;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.google.android.clockwork.common.content.CwPrefs;
import com.google.android.clockwork.phone.Utils;


/**
 * Service that sets various cell related values in the system.
 *
 * Must be run in the phone process since it accesses the default global phone structure.
 */
public class SetNumberService extends IntentService {
    private static final String VOICEMAIL_PREFERENCES =
            "com.google.android.clockwork.settings.cellular.voicemail";
    private static final String TAG = SetNumberService.class.getSimpleName();
    private static final String EMPTY_NUMBER = "";

    private static final int NUMBER_UPDATED = 1;

    public static final String ACTION_SET_PHONE_NUMBER_DISPLAY = "action.set_phone_number_display";
    public static final String ACTION_SET_VOICEMAIL_NUMBER = "action.set_voicemail_number";
    public static final String ACTION_RESTORE_VOICEMAIL_NUMBER = "action.restore_voicemail_number";
    public static final String EXTRA_NEW_NUMBER = "new_number";
    public static final String EXTRA_OLD_NUMBER = "old_number";
    public static final String EXTRA_RESULT_RECEIVER = "result_receiver";
    public static final String EXTRA_IS_OVERRIDE = "is_override";

    public static final String KEY_IS_OVERRIDDEN = "cw.voicemail.is_overridden";
    public static final String KEY_OVERRIDDEN_VOICEMAIL_NUMBER = "cw.voicemail.overridden_number";

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case NUMBER_UPDATED: {
                    AsyncResult result = (AsyncResult) msg.obj;
                    ResultReceiver receiver = (ResultReceiver) result.userObj;
                    if (receiver != null) {
                        receiver.send(Activity.RESULT_OK, null);
                    }
                    return;
                }
            }
        }
    };

    public SetNumberService() {
        super(SetNumberService.class.getSimpleName());
    }

    /**
     * Service intent handler for phone data manipulation.
     *
     * Pass in an intent with one of the handled actions
     * to be serviced.  An optional result receiver for
     * notification may be included when the action is complete.
     *
     * Runs on worker thread.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        ResultReceiver receiver =
                intent.getParcelableExtra(EXTRA_RESULT_RECEIVER);
        switch (intent.getAction()) {
            case ACTION_SET_PHONE_NUMBER_DISPLAY:
                setPhoneNumberDisplay(intent.getStringExtra(EXTRA_NEW_NUMBER), receiver);
                break;
            case ACTION_SET_VOICEMAIL_NUMBER:
                setVoicemailNumber(
                        intent.getStringExtra(EXTRA_NEW_NUMBER),
                        receiver,
                        intent.getBooleanExtra(EXTRA_IS_OVERRIDE, false)
                );
                break;
            case ACTION_RESTORE_VOICEMAIL_NUMBER:
                SharedPreferences prefs = CwPrefs.wrap(this, VOICEMAIL_PREFERENCES);
                String overriddenNumber = prefs.getString(KEY_OVERRIDDEN_VOICEMAIL_NUMBER, null);
                if (overriddenNumber != null) {
                    setVoicemailNumber(overriddenNumber, receiver, false);
                }
                break;
            default:
                Log.e(TAG, "Unknown action for set number service");
                break;
        }
    }

    private void setPhoneNumberDisplay(final String number, final ResultReceiver receiver) {
        Phone phone = PhoneFactory.getDefaultPhone();
        Message message = mHandler.obtainMessage(NUMBER_UPDATED);
        message.obj = receiver;
        phone.setLine1Number(phone.getLine1AlphaTag(), number, message);
    }

    private void setVoicemailNumber(final String number, final ResultReceiver receiver,
            boolean isOverride) {
        Phone phone = PhoneFactory.getDefaultPhone();
        SharedPreferences prefs = CwPrefs.wrap(this, VOICEMAIL_PREFERENCES);
        SharedPreferences.Editor editor = prefs.edit();

        if (!isOverride) {
            // The new number is not an override so save it
            editor.putString(KEY_OVERRIDDEN_VOICEMAIL_NUMBER, number);
        } else if (!prefs.getBoolean(KEY_IS_OVERRIDDEN, false)) {
            // The new number is an override and we're not overridden yet, so save
            // the current number
            editor.putString(KEY_OVERRIDDEN_VOICEMAIL_NUMBER, phone.getVoiceMailNumber());
        }

        editor.putBoolean(KEY_IS_OVERRIDDEN, isOverride);
        editor.apply();

        Message message = mHandler.obtainMessage(NUMBER_UPDATED);
        message.obj = receiver;
        phone.setVoiceMailNumber(phone.getVoiceMailAlphaTag(), number, message);
    }

    /**
     * Formats a phone number and will accept null
     *
     * The context must be valid and the string should
     * be a number sequence that may be formatted according
     * to the appropriate phone locale.
     * <p>
     * @param  context  The context of the calling environment
     * @param  number   The cell number to be formatted.  May be null
     * @return          A properly formatted phone number.  Returns an empty
     *                  string if number is null.
     */
    public static String formatNumber(final Context context, final String number) {
        if (number == null) {
            return EMPTY_NUMBER;
        } else {
            return Utils.formatNumber(context, number);
        }
    }
}
