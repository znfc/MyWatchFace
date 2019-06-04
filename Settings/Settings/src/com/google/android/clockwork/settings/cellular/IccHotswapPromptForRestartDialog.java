package com.google.android.clockwork.settings.cellular;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.wearable.view.WearableDialogActivity;

import com.android.internal.telephony.uicc.UiccCard;
import com.google.android.apps.wearable.settings.R;

/**
 * A dialog to inform user to restart the device because it doesn't support ICC hotswap.
 *
 * Start in UiccCard.prompotForRestart()
 */
public class IccHotswapPromptForRestartDialog extends WearableDialogActivity {

    private boolean mIsAdded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIsAdded = getIntent().getBooleanExtra(UiccCard.EXTRA_ICC_CARD_ADDED, false);
    }

    @Override
    public CharSequence getPositiveButtonText() {
        return getString(R.string.action_restart);
    }

    @Override
    public Drawable getPositiveButtonDrawable() {
        return getDrawable(R.drawable.action_retry);
    }

    @Override
    public void onPositiveButtonClick() {
        super.onPositiveButtonClick();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        pm.reboot("SIM is added/removed.");
    }

    @Override
    public CharSequence getAlertTitle() {
        return getString(mIsAdded ? com.android.internal.R.string.sim_added_title
                : com.android.internal.R.string.sim_removed_title);
    }

    @Override
    public CharSequence getMessage() {
        return getString(mIsAdded ? com.android.internal.R.string.sim_added_message
                : com.android.internal.R.string.sim_removed_message);
    }
}
