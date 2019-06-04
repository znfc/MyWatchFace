package com.google.android.clockwork.settings.connectivity.cellular;

import static com.android.internal.telephony.PhoneConstants.LTE_ON_CDMA_TRUE;

import android.os.Message;
import com.android.internal.telephony.OperatorInfo;
import com.android.internal.telephony.Phone;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * Shadow for {@link com.android.internal.telephony.Phone}.
 */
@Implements(Phone.class)
public class ShadowPhone {
    private static final int NETWORK_SELECTION_MODE_AUTOMATIC = 0;
    private static final int NETWORK_SELECTION_MODE_MANUAL = 1;

    private int mSubId;
    private int mPreferredNetworkType;
    private int mLteOnCdmaMode = LTE_ON_CDMA_TRUE;
    private int mNetworkSelectionMode;

    @Implementation
    public int getSubId() {
        return mSubId;
    }

    @Implementation
    public int getLteOnCdmaMode() {
        return mLteOnCdmaMode;
    }

    @Implementation
    public void getNetworkSelectionMode(Message msg) {
        if (msg != null) {
            msg.arg1 = mNetworkSelectionMode;
        }
    }

    @Implementation
    public void selectNetworkManually(OperatorInfo network, boolean persistSelection,
               Message response) {
        mNetworkSelectionMode = NETWORK_SELECTION_MODE_MANUAL;
        if (response != null) {
            response.arg1 = mNetworkSelectionMode;
        }
    }

    @Implementation
    public void setNetworkSelectionModeAutomatic(Message msg) {
        mNetworkSelectionMode = NETWORK_SELECTION_MODE_AUTOMATIC;
        if (msg != null) {
            msg.arg1 = mNetworkSelectionMode;
        }
    }

    /**
     * Non android accessors
     */
    public void setSubId(int subId) {
        mSubId = subId;
    }

    public void getPreferredNetworkType(Message response) {
        if (response != null) {
            response.arg1 = mPreferredNetworkType;
        }
    }

    public void setPreferredNetworkType(int networkType, Message response) {
        mPreferredNetworkType = networkType;
        if (response != null) {
            response.arg1 = networkType;
        }
    }
}
