package com.google.android.clockwork.settings.connectivity.nfc;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

/** Interface to wrap an internal Android ApduServiceInfo, abstracted for testing purposes. */
public interface IApduServiceInfo {
    ComponentName getComponent();

    String getDescription();

    CharSequence loadLabel(PackageManager pm);

    Drawable loadBanner(PackageManager pm);
}
