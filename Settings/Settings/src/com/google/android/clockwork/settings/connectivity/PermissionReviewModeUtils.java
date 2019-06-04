package com.google.android.clockwork.settings.connectivity;

import android.annotation.Nullable;
import android.annotation.StringRes;

/* Util functions used for permission review mode. */
public interface PermissionReviewModeUtils {
    /** Returns the app's name according to its package name if exists, otherwise returns null. */
    @Nullable
    CharSequence getAppLabelFromPackage(@Nullable String packageName);

    /**
     * If appLabel is not empty, returns formatted title using string resource resId as template,
     * otherwise returns string from defaultResId.
     */
    CharSequence getConsentDialogTitle(
            CharSequence appLabel, @StringRes int resId, @StringRes int defaultResId);

    /** Returns true is permission review mode is enabled, false otherwise. */
    boolean isPermissionReviewModeEnabled();

    /** Returns whether directly accepting BT/WiFi enabling requests based on package name. */
    boolean isPackageWhitelistedForOmittingCmiitDialog(@Nullable String packageName);
}
