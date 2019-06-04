package com.google.android.clockwork.settings.connectivity.cellular;

import static com.android.internal.telephony.PhoneConstants.PHONE_TYPE_GSM;

import android.os.Message;
import com.android.internal.telephony.GsmCdmaPhone;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;

/**
 * Shadow for {@link com.android.internal.telephony.GsmCdmaPhone}.
 */
@Implements(GsmCdmaPhone.class)
public class ShadowGsmCdmaPhone extends ShadowPhone {
    private static int LTE_ON_CDMA_FALSE = 0;
    private static int LTE_ON_CDMA_TRUE = 1;
    private int mPhoneId;
    private int mLteEnabled;
    private String mName;
    private boolean mRadioPower;
    private boolean mDataEnabled;
    private boolean mDataRoamingEnabled;
    private String mLine1NumberAlphaTag;
    private String mLine1Number;
    private String mVoicemailNumberAlphaTag;
    private String mVoicemailNumber;
    private int mPhoneType;

    static GsmCdmaPhone createInstance(int phoneId, String name) {
        GsmCdmaPhone gsmCdmaPhone = Shadow.newInstanceOf(GsmCdmaPhone.class);
        shadowOf(gsmCdmaPhone).mPhoneId = phoneId;
        shadowOf(gsmCdmaPhone).mName = name;
        shadowOf(gsmCdmaPhone).mPhoneType = PHONE_TYPE_GSM;
        shadowOf(gsmCdmaPhone).mLteEnabled = LTE_ON_CDMA_TRUE;
        return gsmCdmaPhone;
    }

    public static ShadowGsmCdmaPhone shadowOf(GsmCdmaPhone phone) {
        return (ShadowGsmCdmaPhone) Shadow.extract(phone);
    }

    @Implementation
    public int getPhoneType() {
        return mPhoneType;
    }

    @Implementation
    public int getSubId() {
        return super.getSubId();
    }

    @Implementation
    public int getPhoneId() {
        return mPhoneId;
    }

    @Implementation
    public void setRadioPower(boolean power) {
        mRadioPower = power;
    }

    @Implementation
    public String getLine1Number() {
        return mLine1Number;
    }

    @Implementation
    public boolean setLine1Number(String alphaTag, String number, Message onComplete) {
        mLine1NumberAlphaTag = alphaTag;
        mLine1Number = number;
        return true;
    }

    @Implementation
    public String getLine1AlphaTag() {
        return mLine1NumberAlphaTag;
    }

    @Implementation
    public String getVoiceMailNumber() {
        return mVoicemailNumber;
    }

    @Implementation
    public void setVoiceMailNumber(String alphaTag, String voicemailNumber, Message onComplete) {
        mVoicemailNumberAlphaTag = alphaTag;
        mVoicemailNumber = voicemailNumber;
    }

    @Implementation
    public String getVoiceMailAlphaTag() {
        return mVoicemailNumberAlphaTag;
    }

    @Implementation
    public boolean getDataRoamingEnabled() {
        return mDataRoamingEnabled;
    }

    @Implementation
    public void setDataRoamingEnabled(boolean enable) {
        mDataRoamingEnabled = enable;
    }

    @Implementation
    public boolean getDataEnabled() {
        return mDataEnabled;
    }

    @Implementation
    public boolean isUserDataEnabled() {
        return mDataEnabled;
    }

    @Implementation
    public void setDataEnabled(boolean enable) {
        mDataEnabled = enable;
    }

    @Implementation
    public void setUserDataEnabled(boolean enable) {
        mDataEnabled = enable;
    }

    @Implementation
    public int getLteOnCdmaMode() {
        return mLteEnabled;
    }


    /**
     * Certain state is accessed not via the API but via other Android
     * mechanisms.
     *
     * These methods are non-telephony APIs that are used to access
     * the shadow variables for testing purposes only.
     */
    public boolean getRadioPower() {
        return mRadioPower;
    }

    public void setSubId(int subId) {
        super.setSubId(subId);
    }
}
