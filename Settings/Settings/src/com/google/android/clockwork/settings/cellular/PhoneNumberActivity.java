package com.google.android.clockwork.settings.cellular;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.AcceptDenyDialog;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.telephony.DialpadHost;
import com.google.android.clockwork.telephony.fragments.DialpadFragment;

/**
 * Activity to allow user to set phone number manually.
 */
public class PhoneNumberActivity extends WearableActivity implements DialpadHost {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.voicemail_number_activity);
        DialpadFragment fragment = new DialpadFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(DialpadFragment.EXTRA_ICON_RES_ID, R.drawable.ic_check_white);
        bundle.putBoolean(DialpadFragment.EXTRA_ALLOW_EMPTY_NUMBER, true);
        fragment.setArguments(bundle);

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.add(R.id.dialpad_container, fragment);
        transaction.commit();
    }

    @Override
    public void onDialPadEnd(int callId, String newNumber) {
        final String oldNumber = getIntent().getStringExtra(SetNumberService.EXTRA_OLD_NUMBER);
        verifyPhoneNumberChanged(newNumber, oldNumber);
    }

    @Override
    public void playDTMF(int callId, char key) {
    }

    /**
     * Confirm or deny the user input of a potential new number
     * <p>
     * @param  newNumber  The new cell number to verify with user.  May be null
     * @param  oldNumber  The previous number to replace.  May be null
     */
    private void verifyPhoneNumberChanged(final String newNumber, final String oldNumber) {
        final AcceptDenyDialog d = new AcceptDenyDialog(PhoneNumberActivity.this);
        d.setTitle(R.string.pref_phoneNumber);
        d.setMessage(getApplicationContext().getString(R.string.settings_update_phone_number,
                SetNumberService.formatNumber(PhoneNumberActivity.this, oldNumber),
                SetNumberService.formatNumber(PhoneNumberActivity.this, newNumber)));
        d.setPositiveButton((dialog, which) -> {
            dialog.dismiss();
            Parcelable p = getIntent().getParcelableExtra(SetNumberService.EXTRA_RESULT_RECEIVER);
            Intent intent = new Intent(SetNumberService.ACTION_SET_PHONE_NUMBER_DISPLAY)
                .putExtra(SetNumberService.EXTRA_NEW_NUMBER, newNumber)
                .putExtra(SetNumberService.EXTRA_RESULT_RECEIVER, p);
            intent.setClass(getApplicationContext(), SetNumberService.class);
            getApplicationContext().startService(intent);
            setResult(Activity.RESULT_OK, intent);
            finish();
        });
        d.setNegativeButton((dialog, which) -> {
            dialog.dismiss();
            cancelAndCleanUp();
            finish();
        });
        d.show();
    }

    private void cancelAndCleanUp() {
        ResultReceiver receiver = getIntent().getParcelableExtra(
                SetNumberService.EXTRA_RESULT_RECEIVER);
        if (receiver != null) {
            receiver.send(Activity.RESULT_CANCELED, null);
        }
        setResult(Activity.RESULT_CANCELED, null);
    }
}
