package com.google.android.clockwork.settings.connectivity.bluetooth;

import android.os.Bundle;
import android.provider.Settings;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;

import com.google.android.apps.wearable.settings.R;

/**
 * Pairing UI for the {@link BluetoothDevice.PAIRING_VARIANT_CONFIRM_PASSKEY} pairing variant.
 *
 * <p>It asks the user to confirm that the passkey shown by the remote device matches the passkey
 * shown by this device.
 */
public class BluetoothConfirmPasskeyActivity extends WearableActivity {
    public static final String EXTRA_PAIRING_KEY = "pairing_key";
    private static final String TAG = "BluetoothConfirmPasskey";

    private BluetoothPairingCancellationReceiver mCancellationReceiver;

    /**
     * Specifies whether the activity should cancel pairing once it finishes.
     *
     * <p>Pairing must be cancelled if it didn't finish successfully and if it wasn't cancelled by
     * the remote device.
     */
    private boolean mCancelOnExit = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final BluetoothPairingDialog diag = new BluetoothPairingDialog(this);
        if (!diag.setDevice(getIntent())) {
            finish();
            return;
        }

        String pairingKey = getIntent().getStringExtra(EXTRA_PAIRING_KEY);
        if (pairingKey == null) {
            Log.e(TAG, "Pairing key cannot be null.");
            finish();
            return;
        }

        mCancellationReceiver = new BluetoothPairingCancellationReceiver(this, () -> {
            mCancelOnExit = false;
        });
        mCancellationReceiver.register();

        diag.setDevice(getIntent());
        diag.setPairingMessage(getString(R.string.bluetooth_confirm_code), pairingKey);
        diag.setOnDismissListener((dialog) -> {
            if (mCancelOnExit) {
                diag.getDevice().setPairingConfirmation(false);
                mCancelOnExit = false;
            }
            finish();
        });

        if (Settings.System.getInt(getContentResolver(), Settings.System.SETUP_WIZARD_HAS_RUN, 0)
                == 0) {
            // if in setup, don't ahve any buttons and auto-confirm
            diag.getDevice().setPairingConfirmation(true);
            mCancelOnExit = false;
        } else {
            diag.setPositiveButton((dialog, which) -> {
                diag.getDevice().setPairingConfirmation(true);
                mCancelOnExit = false;
            });
            diag.setNegativeButton((dialog, which) -> { /* do nothing, just show button */ });
        }

        diag.show();
    }

    @Override
    protected void onDestroy() {
        if (mCancellationReceiver != null) {
            mCancellationReceiver.unregister();
        }
        super.onDestroy();
    }
}
