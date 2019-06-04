package com.google.android.clockwork.settings.connectivity.nfc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.nfc.cardemulation.AidGroup;
import android.nfc.cardemulation.CardEmulation;
import android.nfc.NfcAdapter;
import android.provider.Settings;
import android.support.annotation.Nullable;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import com.google.android.clockwork.robolectric.shadows.ShadowGservices;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;


@RunWith(ClockworkRobolectricTestRunner.class)
public class PaymentBackendTest {

    @Mock private ContentResolver mContentResolver;
    @Mock private PackageManager mPackageManager;

    @Before
    public void setUp() {
        initMocks(this);
        ShadowGservices.reset();
    }

    @Test
    public void noTapAndPayPackages() {
        PaymentBackend testBackend = new PaymentBackend(
                mPackageManager,
                mContentResolver,
                new FakeCardEmuProvider(false, false));

        assertEquals(0, testBackend.getPaymentAppInfos().size());
    }

    @Test
    public void onlyAndroidPayDisabled() throws Exception {
        ShadowGservices.override(PaymentBackend.GSERVICES_FLAG_SHOW_ANDROID_PAY_SETTINGS, false);

        PaymentBackend testBackend = new PaymentBackend(
                mPackageManager,
                mContentResolver,
                new FakeCardEmuProvider(true, false));

        assertEquals(0, testBackend.getPaymentAppInfos().size());
    }

    @Test
    public void onlyAndroidPayEnabled() throws Exception {
        ShadowGservices.override(PaymentBackend.GSERVICES_FLAG_SHOW_ANDROID_PAY_SETTINGS, true);

        PaymentBackend testBackend = new PaymentBackend(
                mPackageManager,
                mContentResolver,
                new FakeCardEmuProvider(true, false));

        List<PaymentBackend.PaymentAppInfo> paymentAppInfos = testBackend.getPaymentAppInfos();
        assertEquals(1, paymentAppInfos.size());
        assertTrue(paymentAppInfos.get(0).componentName.equals(FakeCardEmuProvider.ANDROID_PAY_COMPONENT_NAME));
    }

    @Test
    public void androidPayDisabledButOthersEnabled() throws Exception {
        ShadowGservices.override(PaymentBackend.GSERVICES_FLAG_SHOW_ANDROID_PAY_SETTINGS, false);

        PaymentBackend testBackend = new PaymentBackend(
                mPackageManager,
                mContentResolver,
                new FakeCardEmuProvider(true, true));

        List<PaymentBackend.PaymentAppInfo> paymentAppInfos = testBackend.getPaymentAppInfos();
        assertEquals(1, paymentAppInfos.size());
        assertTrue(paymentAppInfos.get(0).componentName.equals(FakeCardEmuProvider.GENERIC_PAY_COMPONENT_NAME));
    }

    @Test
    public void defaultIsNullWhenAndroidPayDisabled() throws Exception {
        ShadowGservices.override(PaymentBackend.GSERVICES_FLAG_SHOW_ANDROID_PAY_SETTINGS, false);

        PaymentBackend testBackend = new PaymentBackend(
                mPackageManager,
                mContentResolver,
                new FakeCardEmuProvider(true, false));
        testBackend.setDefaultPaymentApp(FakeCardEmuProvider.ANDROID_PAY_COMPONENT_NAME);

        assertNull(testBackend.getDefaultPaymentComponentName());
    }

    @Test
    public void defaultIsSetProperly() throws Exception {
        ShadowGservices.override(PaymentBackend.GSERVICES_FLAG_SHOW_ANDROID_PAY_SETTINGS, false);

        PaymentBackend testBackend = new PaymentBackend(
                mPackageManager,
                mContentResolver,
                new FakeCardEmuProvider(true, true));
        testBackend.setDefaultPaymentApp(FakeCardEmuProvider.GENERIC_PAY_COMPONENT_NAME);

        assertEquals(FakeCardEmuProvider.GENERIC_PAY_COMPONENT_NAME,
                testBackend.getDefaultPaymentComponentName());
    }

    @Test
    public void androidPayAndOthersEnabled() throws Exception {
        ShadowGservices.override(PaymentBackend.GSERVICES_FLAG_SHOW_ANDROID_PAY_SETTINGS, true);

        PaymentBackend testBackend = new PaymentBackend(
                mPackageManager,
                mContentResolver,
                new FakeCardEmuProvider(true, true));

        assertEquals(2, testBackend.getPaymentAppInfos().size());
    }

    private static class FakeCardEmuProvider implements PaymentBackend.CardEmuProvider{

        List<IApduServiceInfo> mServices = new ArrayList<>();

        public static final ComponentName ANDROID_PAY_COMPONENT_NAME = new ComponentName(
                            "com.google.android.gms",
                            "com.google.android.gms.tapandpay.hce.service.TpHceService");

        public static final ComponentName GENERIC_PAY_COMPONENT_NAME = new ComponentName(
                            "com.google.android.world.peace",
                            "com.google.android.world.peace.tapandpay.RandomServiceName");

        FakeCardEmuProvider(boolean showAndroidPay, boolean showGenericPay) {
            if (showAndroidPay) {
                mServices.add(getAndroidPayApduServiceInfo());
            }
            if (showGenericPay) {
                mServices.add(getGenericApduServiceInfo());
            }
        }

        @Nullable
        public List<IApduServiceInfo> getServices() {
            return mServices;
        }

        private IApduServiceInfo getAndroidPayApduServiceInfo() {
            return new TestApduServiceInfo(
                    ANDROID_PAY_COMPONENT_NAME,
                    "Android Pay",
                    "Android Pay",
                    null);
        }

        private IApduServiceInfo getGenericApduServiceInfo() {
            return new TestApduServiceInfo(
                    GENERIC_PAY_COMPONENT_NAME,
                    "Pennywise Pay",
                    "Pennywise Pay",
                    null);
        }

    }
}
