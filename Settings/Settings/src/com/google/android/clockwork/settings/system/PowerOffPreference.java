package com.google.android.clockwork.settings.system;

import android.app.ActivityManager;
import android.content.Context;
import android.os.IBinder;
import android.os.IPowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.support.wearable.preference.AcceptDenyDialogPreference;
import android.support.wearable.view.AcceptDenyDialog;
import android.util.AttributeSet;

import android.widget.TextView;
import com.google.android.apps.wearable.settings.R;

/** Preference for powering off the device. */
public class PowerOffPreference extends AcceptDenyDialogPreference {
    private static final String SHUTDOWN_REASON_SETUP_TIMEOUT = "setupautoshutdowntimeout";

    public PowerOffPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PowerOffPreference(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        setKey("pref_powerOff");
        setTitle(R.string.pref_powerOff);
        setIcon(R.drawable.ic_cc_settings_power_off);
        setDialogTitle(R.string.pref_powerOff);
        setDialogMessage(R.string.are_you_sure);
        setPersistent(false);

        setOnDialogClosedListener((positiveResult) -> {
            // don't reboot if monkey, it'll break monkey test
            if (positiveResult && !ActivityManager.isUserAMonkey()) {
                IBinder b = ServiceManager.getService("power");
                IPowerManager service = IPowerManager.Stub.asInterface(b);
                try {
                    service.shutdown(false, SHUTDOWN_REASON_SETUP_TIMEOUT, false);
                } catch (RemoteException e) {
                    // Couldn't do anything.
                }
            }
        });
    }

    @Override
    protected void onPrepareDialog(AcceptDenyDialog dialog) {
        super.onPrepareDialog(dialog);
        ((TextView) dialog.findViewById(android.R.id.title))
            .setTextAppearance(R.style.WearText_Subhead);
        ((TextView) dialog.findViewById(android.R.id.message))
            .setTextAppearance(R.style.WearText_Body1);
    }
}
