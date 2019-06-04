package com.google.android.clockwork.settings.connectivity.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

/**
 * Listens to pairing requests and launches activities to handle them based on the pairing variant.
 */
public class BluetoothPairingRequestReceiver extends BroadcastReceiver {

    private static final String TAG = BluetoothPairingRequestReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        final boolean setupWizardCompleted = Settings.System.getInt(
                context.getContentResolver(), Settings.System.SETUP_WIZARD_HAS_RUN, 0) == 1;

        if (!setupWizardCompleted) {
            Log.w(TAG, "Ignore external pairing requests while setup wizard is active");
            return;
        }

        int type = intent.getIntExtra(
                BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.ERROR);
        BluetoothDevice deviceToPair = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        Intent pairingActivityIntent = null;
        int pairingKeyInt = 0;
        String pairingKey;
        switch(type) {
            case BluetoothDevice.PAIRING_VARIANT_PIN:
                pairingActivityIntent = new Intent(context, BluetoothEnterPinActivity.class);
                break;
            case BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION:
                pairingActivityIntent = new Intent(context, BluetoothConfirmPasskeyActivity.class);
                pairingKeyInt = intent.getIntExtra(
                        BluetoothDevice.EXTRA_PAIRING_KEY, BluetoothDevice.ERROR);
                pairingKey = String.format("%06d", pairingKeyInt);
                pairingActivityIntent.putExtra(
                        BluetoothConfirmPasskeyActivity.EXTRA_PAIRING_KEY, pairingKey);
                break;
            case BluetoothDevice.PAIRING_VARIANT_CONSENT:
                pairingActivityIntent = new Intent(context, BluetoothPairingConsentActivity.class);
                break;
            case BluetoothDevice.PAIRING_VARIANT_DISPLAY_PASSKEY:
                pairingActivityIntent =
                        new Intent(context, BluetoothDisplayPasskeyActivity.class);
                pairingKeyInt = intent.getIntExtra(
                        BluetoothDevice.EXTRA_PAIRING_KEY, BluetoothDevice.ERROR);
                pairingKey = String.format("%06d", pairingKeyInt);
                pairingActivityIntent.putExtra(BluetoothDisplayPasskeyActivity.EXTRA_PAIRING_KEY,
                        pairingKey);
                pairingActivityIntent.putExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, type);
                break;
            case BluetoothDevice.PAIRING_VARIANT_DISPLAY_PIN:
                pairingActivityIntent =
                        new Intent(context, BluetoothDisplayPasskeyActivity.class);
                pairingKeyInt = intent.getIntExtra(
                        BluetoothDevice.EXTRA_PAIRING_KEY, BluetoothDevice.ERROR);
                pairingKey = String.format("%04d", pairingKeyInt);
                pairingActivityIntent.putExtra(BluetoothDisplayPasskeyActivity.EXTRA_PAIRING_KEY,
                        pairingKey);
                pairingActivityIntent.putExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, type);
                break;
            default:
                Log.w(TAG, "Unsupported pairing variant " + type);
                return;
        }
        if (pairingKeyInt == BluetoothDevice.ERROR) {
            Log.e(TAG, "Pairing key is missing.");
            return;
        }
        if (pairingActivityIntent != null) {
            pairingActivityIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, deviceToPair);
            pairingActivityIntent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(pairingActivityIntent);
        }
    }

}
