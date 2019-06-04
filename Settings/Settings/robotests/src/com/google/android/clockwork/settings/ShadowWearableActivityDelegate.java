package com.google.android.clockwork.settings;

import android.app.Activity;
import android.support.wearable.activity.WearableActivityDelegate;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(WearableActivityDelegate.class)
public class ShadowWearableActivityDelegate {
  @Implementation
  public void initAmbientSupport(Activity activity) {}
}