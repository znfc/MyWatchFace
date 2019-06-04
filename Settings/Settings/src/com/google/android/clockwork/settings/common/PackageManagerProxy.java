package com.google.android.clockwork.settings.common;

import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

/**
 * A proxy class for PackageManager that enables shadowing of methods in PackageManager that aren't
 * yet supported by shadows in Robolectric, simply by holding a reference to an actual instance of
 * and intercepting calls to {@link PackageManager}.
 */
public class PackageManagerProxy {

  private PackageManager mPackageManager;

  public PackageManagerProxy(PackageManager packageManager) {
    mPackageManager = packageManager;
  }

  public int getPackageUidAsUser(String packageName, int userHandle) throws NameNotFoundException {
    return mPackageManager.getPackageUidAsUser(packageName, userHandle);
  }

  public boolean hasSystemFeature(String name) {
    return mPackageManager.hasSystemFeature(name);
  }
}