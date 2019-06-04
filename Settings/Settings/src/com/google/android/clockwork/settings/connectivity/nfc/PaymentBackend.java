package com.google.android.clockwork.settings.connectivity.nfc;

import static com.google.common.base.Preconditions.checkNotNull;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.nfc.cardemulation.ApduServiceInfo;
import android.nfc.cardemulation.CardEmulation;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.google.android.clockwork.settings.connectivity.nfc.IApduServiceInfo;
import com.google.android.gsf.GservicesValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps the backend of the Tap and Pay functionality, providing helper functions to do things like
 * set/get the default Tap and Pay app, and list all the available Tap and Pay apps.
 *
 * Large chunks of this functionality have been taken from Android's PaymentBackend.
 */
public class PaymentBackend {
    public static final String TAG = "PaymentBackend";

    public static class PaymentAppInfo {
        CharSequence caption;
        Drawable banner;
        boolean isDefault;
        public ComponentName componentName;
    }

    public interface Listener {
        void onDefaultAppChanged();
    }

    /** Provides a list of payment services, abstracted for testing purposes. */
    public interface CardEmuProvider {
        @Nullable List<IApduServiceInfo> getServices();
    }

    /** System implementation providing payment services from the CardEmulation. */
    public static class DefaultCardEmuProvider implements CardEmuProvider {
        private final CardEmulation mCardEmuManager;

        public DefaultCardEmuProvider(CardEmulation cardEmuManager) {
            mCardEmuManager = checkNotNull(cardEmuManager);
        }

        @Nullable
        public List<IApduServiceInfo> getServices() {
            List<ApduServiceInfo> serviceInfos =
                    mCardEmuManager.getServices(CardEmulation.CATEGORY_PAYMENT);
            List<IApduServiceInfo> outInfos = new ArrayList<>();
            for (ApduServiceInfo service : serviceInfos) {
                outInfos.add(new DefaultApduServiceInfo(service));
            }
            return outInfos;
        }
    }

    @VisibleForTesting
    static final String GSERVICES_FLAG_SHOW_ANDROID_PAY_SETTINGS =
            "google_wallet:show_android_pay_settings";

    private static final String ANDROID_PAY_COMPONENT_NAME =
            "com.google.android.gms/com.google.android.gms.tapandpay.hce.service.TpHceService";

    private final PackageManager mPackageManager;
    private final ContentResolver mContentResolver;
    private final CardEmuProvider mCardEmuProvider;
    private final List<Listener> mListeners;
    private final boolean mShouldShowAndroidPay;

    public PaymentBackend(
            PackageManager packageManager,
            ContentResolver contentResolver,
            CardEmuProvider cardEmuProvider) {
        mPackageManager = checkNotNull(packageManager);
        mContentResolver = checkNotNull(contentResolver);
        mCardEmuProvider = checkNotNull(cardEmuProvider);

        mListeners = new ArrayList<Listener>();

        // Cache the Android Pay setting, since the GservicesValue retrieval could be expensive,
        // so we don't really want to do it every loop iteration in getPaymentAppInfos().
        GservicesValue<Boolean> gservicesValue =
                GservicesValue.value(GSERVICES_FLAG_SHOW_ANDROID_PAY_SETTINGS, false);
        mShouldShowAndroidPay = gservicesValue.get();
    }

    public void addListener(Listener newListener) {
        mListeners.add(newListener);
    }

    /*
     * Returns a list of the available apps in the system that can be used for Tap and Pay.
     */
    public List<PaymentAppInfo> getPaymentAppInfos() {
        List<IApduServiceInfo> serviceInfos = mCardEmuProvider.getServices();

        List<PaymentAppInfo> appInfos = new ArrayList<PaymentAppInfo>();

        if (serviceInfos == null) {
            return appInfos;
        }

        ComponentName defaultApp = getDefaultPaymentComponentName();

        for (IApduServiceInfo service : serviceInfos) {
            if (serviceIsAvailable(service)) {
                PaymentAppInfo appInfo = new PaymentAppInfo();
                appInfo.banner = service.loadBanner(mPackageManager);
                appInfo.caption = service.getDescription();
                if (appInfo.caption == null) {
                    appInfo.caption = service.loadLabel(mPackageManager);
                }
                appInfo.isDefault = service.getComponent().equals(defaultApp);
                appInfo.componentName = service.getComponent();
                appInfos.add(appInfo);
            }
        }

        return appInfos;
    }

    /*
     * Returns whether the given service is available. This is currently only relevant to the
     * Android Pay service, which is disabled in countries where it is not supported.
     */
    private boolean serviceIsAvailable(@Nullable IApduServiceInfo service) {
        if (service == null || service.getComponent() == null) {
            return false;
        }

        return componentIsAvailable(service.getComponent().flattenToString());
    }

    /*
     * Returns whether the given component is available. This is currently only relevant to the
     * Android Pay service, which is disabled in countries where it is not supported.
     */
    private boolean componentIsAvailable(@Nullable String componentName) {
        if (componentName == null) {
            return false;
        }

        if (mShouldShowAndroidPay) {
            return true;
        }

        return !ANDROID_PAY_COMPONENT_NAME.equals(componentName);
    }

    /*
     * Returns the ComponentName for the Tap and Pay app that is currently selected to use.
     */
    ComponentName getDefaultPaymentComponentName() {
        String componentString = Settings.Secure.getString(mContentResolver,
                Settings.Secure.NFC_PAYMENT_DEFAULT_COMPONENT);
        if (componentString != null && componentIsAvailable(componentString)) {
            return ComponentName.unflattenFromString(componentString);
        } else {
            return null;
        }
    }

    /*
     * Returns the user-readable name of the Tap and Pay app that is currently selected to use.
     */
    CharSequence getDefaultPaymentCaption() {
        List<PaymentAppInfo> appInfos = getPaymentAppInfos();
        if (appInfos == null) {
            return null;
        }

        for (PaymentAppInfo appInfo : appInfos) {
            if (appInfo.isDefault) {
                return appInfo.caption;
            }
        }

        return null;
    }

    /*
     * Sets the Tap and Pay app to use.
     */
    public void setDefaultPaymentApp(ComponentName app) {
        Settings.Secure.putString(mContentResolver,
                Settings.Secure.NFC_PAYMENT_DEFAULT_COMPONENT,
                app != null ? app.flattenToString() : null);
        for (Listener listener : mListeners) {
            listener.onDefaultAppChanged();
        }
    }
}
