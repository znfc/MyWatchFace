package com.google.android.clockwork.settings.connectivity.bluetooth;

import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;

/**
 * Asks the user whether to pair with a given remote Bluetooth device.
 *
 * <p>It is shown when an incoming Bluetooth pairing request is being processed.
 */
public class BluetoothPairingConsentActivity extends WearableActivity {
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

        mCancellationReceiver = new BluetoothPairingCancellationReceiver(this, () -> {
            mCancelOnExit = false;
        });
        mCancellationReceiver.register();

        diag.setOnDismissListener((dialog) -> {
            if (mCancelOnExit) {
                diag.getDevice().setPairingConfirmation(false);
                mCancelOnExit = false;
            }
            finish();
        });
        diag.setPositiveButton((dialog, which) -> {
            diag.getDevice().setPairingConfirmation(true);
            mCancelOnExit = false;
        });
        diag.setNegativeButton((dialog, which) -> { /* do nothing, just show button */ });
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
