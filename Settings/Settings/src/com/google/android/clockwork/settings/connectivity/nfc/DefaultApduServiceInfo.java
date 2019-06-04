package com.google.android.clockwork.settings.connectivity.nfc;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.nfc.cardemulation.ApduServiceInfo;

/** Default implementation of an ApduServiceInfo wrapper, using the internal Android class. */
public class DefaultApduServiceInfo implements IApduServiceInfo {
    private final ApduServiceInfo mApduServiceInfo;

    DefaultApduServiceInfo(ApduServiceInfo serviceInfo) {
        mApduServiceInfo = serviceInfo;
    }

    public ComponentName getComponent() {
        return mApduServiceInfo.getComponent();
    }

    public String getDescription() {
        return mApduServiceInfo.getDescription();
    }

    public CharSequence loadLabel(PackageManager pm) {
        return mApduServiceInfo.loadLabel(pm);
    }

    public Drawable loadBanner(PackageManager pm) {
        return mApduServiceInfo.loadBanner(pm);
    }
}
