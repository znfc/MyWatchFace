package com.google.android.clockwork.settings.system;

import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.support.wearable.preference.AcceptDenySwitchPreference;
import android.support.wearable.view.AcceptDenyDialog;
import android.util.AttributeSet;
import android.widget.TextView;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.BatterySaverUtil;

/** Preference for powering off the device. */
public class BatterySaverTogglePreference extends AcceptDenySwitchPreference {

    public BatterySaverTogglePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BatterySaverTogglePreference(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        setKey("pref_batterySaver");
        setTitle(R.string.pref_batterySaver);
        setDialogTitle(R.string.pref_batterySaver_dialogTitle);
        setDialogMessage(BatterySaverUtil.getSaverDialogMessage(getContext()));
        setPersistent(false);

        setOnPreferenceChangeListener((p, newVal) -> {
            boolean enable = (Boolean) newVal;
            BatterySaverUtil.startBatterySaver(enable, getContext(), context.getSystemService(
                PowerManager.class));
            ((AcceptDenySwitchPreference) p).setChecked(enable);
            if (enable) {
                getContext().startActivity(
                    new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME));
            }
            return true;
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
