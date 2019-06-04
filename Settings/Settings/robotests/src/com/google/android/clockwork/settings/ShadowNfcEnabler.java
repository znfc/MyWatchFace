package com.google.android.clockwork.settings;

import com.google.android.clockwork.settings.connectivity.nfc.NfcEnabler;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import android.preference.Preference;

@Implements(NfcEnabler.class)
public class ShadowNfcEnabler {
  @Implementation
  public boolean isNfcEnabled() {
    return true;
  }

  @Implementation
  public void resume() {}

  @Implementation
  public void pause() {}

  @Implementation
  public boolean onPreferenceChange(Preference preference, Object value) {
    return false;
  }
}