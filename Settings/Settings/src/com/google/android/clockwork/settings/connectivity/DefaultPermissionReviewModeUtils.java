package com.google.android.clockwork.settings.connectivity;

import static com.google.common.base.Preconditions.checkNotNull;

import android.annotation.Nullable;
import android.annotation.StringRes;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.VisibleForTesting;
import android.text.Html;
import android.text.TextUtils;

import com.google.android.clockwork.common.suppliers.LazyContextSupplier;
import com.google.android.clockwork.common.suppliers.LazyContextSupplier.InstanceCreator;
import com.google.android.clockwork.settings.GuardianModeConfig;

import java.util.Arrays;
import java.util.List;

/* Util functions used for premission review mode. */
public class DefaultPermissionReviewModeUtils implements PermissionReviewModeUtils {
    private static final String TAG = "PermissionReviewModeUtils";
    private static final boolean SHOULD_WHITELIST_TEST_PACKAGES =
            Build.TYPE.equals("userdebug") || Build.TYPE.equals("eng");

    private static final List<String> PACKAGE_WHITELIST_FOR_CMIIT =
            Arrays.asList(
                    // For iOS connection management in GMS Core.
                    "com.google.android.gms",
                    // For iOS WiFi reconnection service in Android Wear app.
                    "com.google.android.wearable.app",
                    // For retail service bring-up, see b/113365809.
                    "com.google.android.wearable.setupwizard");
    private static final List<String> TEST_PACKAGE_WHITELIST_FOR_CMIIT =
            Arrays.asList(
                    // For WifiUtil in Tradefederation
                    "com.android.tradefed.utils.wifi",
                    // BluetoothTests APK used in Wear automation tests.
                    "com.android.bluetooth.tests",
                    // Stability test APK
                    "com.google.android.clockwork.stabilitytests");

    private final PackageManager mPackageManager;
    private final Resources mResources;
    private final GuardianModeConfig mGuardianModeConfig;
    private boolean mPermissionReviewModeEnabled;

    public static final LazyContextSupplier<PermissionReviewModeUtils> INSTANCE =
            new LazyContextSupplier<>(
                    new InstanceCreator<PermissionReviewModeUtils>() {
                        @Override
                        public PermissionReviewModeUtils createNewInstance(Context appContext) {
                            DefaultPermissionReviewModeUtils instance =
                                    new DefaultPermissionReviewModeUtils(
                                            appContext.getPackageManager(),
                                            appContext.getResources(),
                                            GuardianModeConfig.INSTANCE.get(appContext));
                            instance.initialize();
                            return instance;
                        }
                    },
                    TAG);

    @VisibleForTesting
    DefaultPermissionReviewModeUtils(
            PackageManager packageManager,
            Resources resources,
            GuardianModeConfig guardianModeConfig) {
        mPackageManager = checkNotNull(packageManager);
        mResources = checkNotNull(resources);
        mGuardianModeConfig = checkNotNull(guardianModeConfig);
    }

    @VisibleForTesting
    void initializeForTest(boolean enabled) {
        mPermissionReviewModeEnabled = enabled;
    }

    private void initialize() {
        mPermissionReviewModeEnabled = mPackageManager.isPermissionReviewModeEnabled();
    }

    /** Returns the app's name according to its package name if exists, otherwise returns null. */
    @Nullable
    public CharSequence getAppLabelFromPackage(@Nullable String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return null;
        }

        try {
            ApplicationInfo applicationInfo = mPackageManager.getApplicationInfo(packageName, 0);
            return applicationInfo.loadSafeLabel(mPackageManager);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    /**
     * If appLabel is not empty, returns formatted title using string resource resId as template,
     * otherwise returns string from defaultResId.
     */
    public CharSequence getConsentDialogTitle(
            CharSequence appLabel, @StringRes int resId, @StringRes int defaultResId) {
        return TextUtils.isEmpty(appLabel)
                ? mResources.getString(defaultResId)
                : Html.fromHtml(mResources.getString(resId, appLabel), 0);
    }

    /** Returns true is permission review mode is enabled, false otherwise. */
    public boolean isPermissionReviewModeEnabled() {
        return mPermissionReviewModeEnabled;
    }

    /** Returns whether directly accepting BT/WiFi enabling requests based on package name. */
    public boolean isPackageWhitelistedForOmittingCmiitDialog(@Nullable String packageName) {
        final String guardianModePackage = mGuardianModeConfig.getGuardianModePackage();
        return mPermissionReviewModeEnabled
                && (PACKAGE_WHITELIST_FOR_CMIIT.contains(packageName)
                        || inTestModeAndIsTestPackage(packageName)
                        || (!TextUtils.isEmpty(guardianModePackage)
                                && TextUtils.equals(packageName, guardianModePackage)));
    }

    private boolean inTestModeAndIsTestPackage(@Nullable String packageName) {
        return SHOULD_WHITELIST_TEST_PACKAGES
                && TEST_PACKAGE_WHITELIST_FOR_CMIIT.contains(packageName);
    }
}
