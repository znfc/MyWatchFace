package com.google.android.clockwork.settings;

import android.app.ActivityManager;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.wearable.view.WearableDialogActivity;
import android.view.View;

import com.google.android.apps.wearable.settings.R;

/**
 * Confirmation dialog shows phone away error.
 */
public class PhoneAwayErrorDialog extends WearableDialogActivity {

    @Override
    public CharSequence getPositiveButtonText() {
        return getString(R.string.generic_ok);
    }

    @Override
    public Drawable getPositiveButtonDrawable() {
        return getDrawable(R.drawable.action_accept);
    }

    @Override
    public void onPositiveButtonClick() {
        super.onPositiveButtonClick();
        finish();
    }

    @Override
    public CharSequence getAlertTitle() {
        return getString(R.string.phone_away_error_dialog_title);
    }

    @Override
    public CharSequence getMessage() {
        return getString(R.string.phone_away_error_dialog_subtitle);
    }
}
