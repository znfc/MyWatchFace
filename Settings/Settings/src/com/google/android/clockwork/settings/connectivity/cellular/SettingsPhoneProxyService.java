package com.google.android.clockwork.settings.connectivity.cellular;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Message;

import android.support.annotation.VisibleForTesting;

import com.android.internal.telephony.OperatorInfo;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;

/**
 * Proxy that runs in the phone process to handle access to
 * phone module that is not presented in the telephony API.
 *
 * The idea is that any access to internal phone state by
 * settings would go through here, except for the intent
 * services that are one way.
 */
public class SettingsPhoneProxyService extends Service {
    protected Phone mPhone;

    public static final int NT_MODE_CDMA = Phone.NT_MODE_CDMA;
    public static final int NT_MODE_CDMA_NO_EVDO = Phone.NT_MODE_CDMA_NO_EVDO;
    public static final int NT_MODE_EVDO_NO_CDMA = Phone.NT_MODE_EVDO_NO_CDMA;
    public static final int NT_MODE_GLOBAL = Phone.NT_MODE_GLOBAL;
    public static final int NT_MODE_LTE_CDMA_AND_EVDO = Phone.NT_MODE_LTE_CDMA_AND_EVDO;
    public static final int NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA = Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA;
    public static final int NT_MODE_LTE_ONLY = Phone.NT_MODE_LTE_ONLY;

    public static final int NETWORK_SELECTION_MODE_AUTOMATIC = 0;
    public static final int NETWORK_SELECTION_MODE_MANUAL = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        mPhone = getDefaultPhone();
    }

    @VisibleForTesting
    /* package */ Phone getDefaultPhone() {
        return PhoneFactory.getDefaultPhone();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Return the interface
        return mBinder;
    }

    private final ISettingsPhoneProxy.Stub mBinder = new ISettingsPhoneProxy.Stub() {
        public void setRadioPower(boolean radioPower) {
            mPhone.setRadioPower(radioPower);
        }

        public boolean getDataEnabled() {
            return mPhone.isUserDataEnabled();
        }

        public void setDataEnabled(boolean dataEnabled) {
            mPhone.setUserDataEnabled(dataEnabled);
        }

        public boolean getDataRoamingEnabled() {
            return mPhone.getDataRoamingEnabled();
        }

        public void setDataRoamingEnabled(boolean dataRoamingEnabled) {
            mPhone.setDataRoamingEnabled(dataRoamingEnabled);
        }

        public String getLine1AlphaTag() {
            return mPhone.getLine1AlphaTag();
        }

        public String getLine1Number() {
            return mPhone.getLine1Number();
        }

        public boolean setLine1Number(String alphaTag, String number, Message onComplete) {
            return mPhone.setLine1Number(alphaTag, number, onComplete);
        }

        public String getVoiceMailAlphaTag() {
            return mPhone.getVoiceMailAlphaTag();
        }

        public String getVoiceMailNumber() {
            return mPhone.getVoiceMailNumber();
        }

        public void setVoiceMailNumber(String alphaTag, String voicemailNumber,
                Message onComplete) {
            mPhone.setVoiceMailNumber(alphaTag, voicemailNumber, onComplete);
        }

        public int getPhoneId() {
            return mPhone.getPhoneId();
        }

        public int getPhoneType() {
            return mPhone.getPhoneType();
        }

        public boolean isLteOnCdma() {
            return mPhone.getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE;
        }

        public int getSubId() {
            return mPhone.getSubId();
        }

        public void getNetworkSelectionMode(Message msg) {
            mPhone.getNetworkSelectionMode(msg);
        }

        public void setPreferredNetworkType(int networkType, Message response) {
            mPhone.setPreferredNetworkType(networkType, response);
        }

         public void selectNetworkManually(OperatorInfo network, boolean persistSelection,
                Message response) {
            mPhone.selectNetworkManually(network, persistSelection, response);
        }

        public void setNetworkSelectionModeAutomatic(Message response) {
            mPhone.setNetworkSelectionModeAutomatic(response);
        }
    };
};
