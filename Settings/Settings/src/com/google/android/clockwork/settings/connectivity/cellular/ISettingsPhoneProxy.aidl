package com.google.android.clockwork.settings.connectivity.cellular;

import android.os.Message;
import com.android.internal.telephony.OperatorInfo;

/**
 * Proxy that runs in the phone process to handle access to
 * phone module that is not presented in the telephony API.
 *
 * Implements a subset of {@link com.android.internal.telephony.PhoneInternalInterface}.
 * In addition to a few other commands
 */
interface ISettingsPhoneProxy {
  // Phone Internal Interface
  void setRadioPower(boolean radioPower);
  boolean getDataEnabled();
  void setDataEnabled(boolean dataEnabled);
  boolean getDataRoamingEnabled();
  void setDataRoamingEnabled(boolean dataRoamingEnabled);
  String getLine1AlphaTag();
  String getLine1Number();
  boolean setLine1Number(String alphaTag, String number, in Message onComplete);
  String getVoiceMailAlphaTag();
  String getVoiceMailNumber();
  void setVoiceMailNumber(String alphaTag, String voicemailNumber, in Message onComplete);
  // Phone global
  int getPhoneId();
  int getPhoneType();
  boolean isLteOnCdma();
  // Subscription information
  int getSubId();
  // Direct to command interface
  void getNetworkSelectionMode(in Message msg);
  void setPreferredNetworkType(int networkType, in Message response);
  void selectNetworkManually(in OperatorInfo network, boolean persistSelection, in Message response);
  void setNetworkSelectionModeAutomatic(in Message response);
}
