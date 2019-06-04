package com.google.android.clockwork.settings.keyguard;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.wearable.view.AcceptDenyDialog;

/**
 * Because the UI for entering lock password is an Activity hosting a Dialog, we run into issues
 * where the dialog is dismissed before we can successfully hide a keyboard that was shown since the
 * password view itself is hosted within the dialog and needs to be on the dialog window in order
 * to actually display.
 *
 * This works around that by overriding, dismiss(), which isn't really kosher but otherwise
 * relatively safe.
 */
class KeyboardHidingAcceptDenyDialog extends AcceptDenyDialog {

    private final Handler mHandler = new Handler();
    private final PasswordEntryHelper mHelper;

    KeyboardHidingAcceptDenyDialog(Context context, PasswordEntryHelper helper) {
        super(context);
        mHelper = helper;
    }

    @Override
    public void dismiss() {
        if (Looper.myLooper() == mHandler.getLooper()) {
            mHelper.hideKeyboard();
        } else {
            mHandler.post(mHelper::hideKeyboard);
        }
        super.dismiss();
    }
}
