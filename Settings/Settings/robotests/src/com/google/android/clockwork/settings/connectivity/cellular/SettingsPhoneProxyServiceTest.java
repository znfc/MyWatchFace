package com.google.android.clockwork.settings.connectivity.cellular;

import static com.android.internal.telephony.PhoneConstants.PHONE_TYPE_GSM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Intent;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.Phone;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;
import org.robolectric.Robolectric;

/**
 * Tests clockwork phone service
 *
 *{@link com.google.android.clockwork.settings.connectivity.cellular.SettingsPhoneProxyService}.
 */
@RunWith(ClockworkRobolectricTestRunner.class)
@Config(sdk = 28)
public class SettingsPhoneProxyServiceTest {
    private static final int PHONE_ID = 1;
    private static final int PREFERRED_NETWORK_TYPE_ID0 = 123;
    private static final int PREFERRED_NETWORK_TYPE_ID1 = 456;

    private static final int SUBSCRIPTION_ID0 = 789;
    private static final int SUBSCRIPTION_ID1 = 1011;

    private static final int NETWORK_SELECTION_AUTOMATIC = 0;
    private static final int NETWORK_SELECTION_MANUAL = 1;

    private static final String PHONE_NAME = "ShadowGsmCdmaPhone";
    private static final String LINE1NUMBERALPHA = "Test Number 1";
    private static final String LINE1NUMBER = "650-555-1212";
    private static final String LINE2NUMBERALPHA = "Test Number 2";
    private static final String LINE2NUMBER = "415-555-1212";

    private GsmCdmaPhone mGsmCdmaPhone;
    private ISettingsPhoneProxy.Stub mStub;

    @Before
    public void setUp() {
        ServiceController<TestPhoneProxyService> serviceController
                = Robolectric.buildService(TestPhoneProxyService.class).create().bind();

        mGsmCdmaPhone = ShadowGsmCdmaPhone.createInstance(PHONE_ID, PHONE_NAME);

        TestPhoneProxyService phoneProxyService = serviceController.get();
        phoneProxyService.setPhone(mGsmCdmaPhone);

        IBinder binder = phoneProxyService.onBind(new Intent());
        mStub = (ISettingsPhoneProxy.Stub) binder;
    }

    @Test
    public void testGlobalConstantsTest() {
        assertEquals(SettingsPhoneProxyService.NT_MODE_CDMA, Phone.NT_MODE_CDMA);
        assertEquals(SettingsPhoneProxyService.NT_MODE_CDMA_NO_EVDO, Phone.NT_MODE_CDMA_NO_EVDO);
        assertEquals(SettingsPhoneProxyService.NT_MODE_EVDO_NO_CDMA, Phone.NT_MODE_EVDO_NO_CDMA);
        assertEquals(SettingsPhoneProxyService.NT_MODE_GLOBAL, Phone.NT_MODE_GLOBAL);
        assertEquals(SettingsPhoneProxyService.NT_MODE_LTE_CDMA_AND_EVDO,
                     Phone.NT_MODE_LTE_CDMA_AND_EVDO);
        assertEquals(SettingsPhoneProxyService.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA,
                Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA);
        assertEquals(SettingsPhoneProxyService.NT_MODE_LTE_ONLY, Phone.NT_MODE_LTE_ONLY);
    }

    @Test
    public void testRadioPowerTest() {
        try {
            mStub.setRadioPower(false);
            assertFalse(ShadowGsmCdmaPhone.shadowOf(mGsmCdmaPhone).getRadioPower());
            mStub.setRadioPower(true);
            assertTrue(ShadowGsmCdmaPhone.shadowOf(mGsmCdmaPhone).getRadioPower());
            mStub.setRadioPower(false);
            assertFalse(ShadowGsmCdmaPhone.shadowOf(mGsmCdmaPhone).getRadioPower());
        } catch (RemoteException e) {
            fail();
        }
    }

    @Test
    public void testDataEnabledTest() {
        try {
            mStub.setDataEnabled(false);
            assertFalse(mStub.getDataEnabled());
            mStub.setDataEnabled(true);
            assertTrue(mStub.getDataEnabled());
            mStub.setDataEnabled(false);
            assertFalse(mStub.getDataEnabled());
        } catch (RemoteException e) {
            fail();
        }
    }

    @Test
    public void testDataRoamingTest() {
        try {
            mStub.setDataRoamingEnabled(false);
            assertFalse(mStub.getDataRoamingEnabled());
            mStub.setDataRoamingEnabled(true);
            assertTrue(mStub.getDataRoamingEnabled());
            mStub.setDataRoamingEnabled(false);
            assertFalse(mStub.getDataRoamingEnabled());
        } catch (RemoteException e) {
            fail();
        }
    }

    @Test
    public void testLine1NumberTest() {
        try {
            mStub.setLine1Number(LINE1NUMBERALPHA, LINE1NUMBER, null);
            assertEquals(LINE1NUMBERALPHA, mStub.getLine1AlphaTag());
            assertEquals(LINE1NUMBER, mStub.getLine1Number());
            mStub.setLine1Number(LINE2NUMBERALPHA, LINE2NUMBER, null);
            assertEquals(LINE2NUMBERALPHA, mStub.getLine1AlphaTag());
            assertEquals(LINE2NUMBER, mStub.getLine1Number());
        } catch (RemoteException e) {
            fail();
        }
    }

    @Test
    public void testVoicemailNumberTest() {
        try {
            mStub.setVoiceMailNumber(LINE1NUMBERALPHA, LINE1NUMBER, null);
            assertEquals(LINE1NUMBERALPHA, mStub.getVoiceMailAlphaTag());
            assertEquals(LINE1NUMBER, mStub.getVoiceMailNumber());
            mStub.setVoiceMailNumber(LINE2NUMBERALPHA, LINE2NUMBER, null);
            assertEquals(LINE2NUMBERALPHA, mStub.getVoiceMailAlphaTag());
            assertEquals(LINE2NUMBER, mStub.getVoiceMailNumber());
        } catch (RemoteException e) {
            fail();
        }
    }

    @Test
    public void testSubIdTest() {
        try {
            ShadowGsmCdmaPhone.shadowOf(mGsmCdmaPhone).setSubId(SUBSCRIPTION_ID0);
            assertEquals(SUBSCRIPTION_ID0, mStub.getSubId());
            ShadowGsmCdmaPhone.shadowOf(mGsmCdmaPhone).setSubId(SUBSCRIPTION_ID1);
            assertEquals(SUBSCRIPTION_ID1, mStub.getSubId());
        } catch (RemoteException e) {
            fail();
        }
    }

    @Test
    public void testPhoneTypeTest() {
        try {
            mStub.getPhoneType();
            assertEquals(PHONE_TYPE_GSM, mStub.getPhoneType());
        } catch (RemoteException e) {
            fail();
        }
    }

    @Test
    public void testLteOnCdmaTest() {
        try {
            assertEquals(true, mStub.isLteOnCdma());
        } catch (RemoteException e) {
            fail();
        }
    }

    @Test
    public void testPreferredNetworkTypeTest() {
        try {
            Message msg = new Message();
            mStub.setPreferredNetworkType(PREFERRED_NETWORK_TYPE_ID0, msg);
            ShadowGsmCdmaPhone.shadowOf(mGsmCdmaPhone).getPreferredNetworkType(msg);
            assertEquals(PREFERRED_NETWORK_TYPE_ID0, msg.arg1);
            mStub.setPreferredNetworkType(PREFERRED_NETWORK_TYPE_ID1, msg);
            ShadowGsmCdmaPhone.shadowOf(mGsmCdmaPhone).getPreferredNetworkType(msg);
            assertEquals(PREFERRED_NETWORK_TYPE_ID1, msg.arg1);
        } catch (RemoteException e) {
            fail();
        }
    }

    @Test
    public void testSelectNetworkManuallyTest() {
        try {
            Message msg = new Message();
            mStub.selectNetworkManually(null /* OperatorInfo */, false, msg);
            mStub.getNetworkSelectionMode(msg);
            assertEquals(NETWORK_SELECTION_MANUAL, msg.arg1);
        } catch (RemoteException e) {
            fail();
        }
    }

    @Test
    public void testNetworkSelectionModeAutomaticTest() {
        try {
            Message msg = new Message();
            mStub.setNetworkSelectionModeAutomatic(msg);
            mStub.getNetworkSelectionMode(msg);
            assertEquals(NETWORK_SELECTION_AUTOMATIC, msg.arg1);
        } catch (RemoteException e) {
            fail();
        }
    }

    /** Test service for phone proxy */
    public static class TestPhoneProxyService extends SettingsPhoneProxyService {
        public void setPhone(Phone phone) {
            mPhone = phone;
        }

        @Override
        public Phone getDefaultPhone() {
            return mPhone;
        }
    }
}
