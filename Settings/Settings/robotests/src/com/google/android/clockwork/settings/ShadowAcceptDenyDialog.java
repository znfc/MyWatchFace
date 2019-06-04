package com.google.android.clockwork.settings;

import android.annotation.Nullable;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.StyleRes;
import android.support.annotation.VisibleForTesting;
import android.support.wearable.view.AcceptDenyDialog;
import java.util.Map;
import java.util.HashMap;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowDialog;

@Implements(AcceptDenyDialog.class)
public class ShadowAcceptDenyDialog extends ShadowDialog {
    private static Map<Context, ShadowAcceptDenyDialog> sLatestDialog = new HashMap();
    private Context mContext;
    private CharSequence mTitle;
    private DialogInterface.OnCancelListener mCancelListener;
    private DialogInterface.OnClickListener mPositiveButtonListener;
    private DialogInterface.OnClickListener mNegativeButtonListener;

    @Implementation
    public void __constructor__(Context context) {
        mContext = context;
    }

    @Implementation
    public void __constructor__(Context context, @StyleRes int themeResId) {
        mContext = context;
    }

    @Implementation
    public void setTitle(@Nullable CharSequence title) {
        mTitle = title;
    }

    @Implementation
    public void setOnCancelListener(@Nullable DialogInterface.OnCancelListener listener) {
        mCancelListener = listener;
    }

    @Implementation
    public void setPositiveButton(DialogInterface.OnClickListener listener) {
        mPositiveButtonListener = listener;
    }

    @Implementation
    public void setNegativeButton(DialogInterface.OnClickListener listener) {
        mNegativeButtonListener = listener;
    }

    @Implementation
    public void show() {
        sLatestDialog.put(mContext, this);
    }

    @VisibleForTesting
    public static ShadowAcceptDenyDialog getLatestAcceptDenyDialog(Context context) {
        return sLatestDialog.get(context);
    }

    @VisibleForTesting
    public DialogInterface.OnCancelListener getOnCancelListener() {
        return mCancelListener;
    }

    @VisibleForTesting
    public DialogInterface.OnClickListener getPositiveButton() {
        return mPositiveButtonListener;
    }

    @VisibleForTesting
    public DialogInterface.OnClickListener getNegativeButton() {
        return mNegativeButtonListener;
    }
}
