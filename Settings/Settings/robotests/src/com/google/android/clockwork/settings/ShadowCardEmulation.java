package com.google.android.clockwork.settings;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

import android.nfc.NfcAdapter;
import android.nfc.cardemulation.CardEmulation;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(CardEmulation.class)
public class ShadowCardEmulation {

  private static final CardEmulation sMock = mock(CardEmulation.class);

  @Implementation
  public static synchronized CardEmulation getInstance(NfcAdapter adapter) {
    return sMock;
  }

  public static CardEmulation getMock() {
    return sMock;
  }

  public static void resetMock() {
    reset(sMock);
  }

}