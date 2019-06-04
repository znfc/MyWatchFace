package com.google.android.clockwork.settings.connectivity.nfc;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

/** Test implementation that just returns the constructor's parameters. */
public class TestApduServiceInfo implements IApduServiceInfo {

    private final ComponentName mComponentName;
    private final String mDescription;
    private final CharSequence mLabel;
    private final Drawable mBanner;

    TestApduServiceInfo(
            ComponentName componentName,
            String description,
            CharSequence label,
            Drawable banner) {
        mComponentName = componentName;
        mDescription = description;
        mLabel = label;
        mBanner = banner;
    }

    public ComponentName getComponent() {
        return mComponentName;
    }

    public String getDescription() {
        return mDescription;
    }

    public CharSequence loadLabel(PackageManager pm) {
        return mLabel;
    }

    public Drawable loadBanner(PackageManager pm) {
        return mBanner;
    }
}
