package com.google.android.clockwork.settings.system;

import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.util.AttributeSet;

import com.google.android.apps.wearable.settings.R;

/** Preference for factory resetting the device. */
public class FactoryResetPreference extends Preference {

    public FactoryResetPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FactoryResetPreference(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        setKey("pref_factoryReset");
        setTitle(R.string.pref_factoryReset);
        setIcon(R.drawable.ic_cc_settings_factory_reset);
        setIntent(new Intent(getContext(), FactoryResetDialogActivity.class));
        setPersistent(false);
    }
}
