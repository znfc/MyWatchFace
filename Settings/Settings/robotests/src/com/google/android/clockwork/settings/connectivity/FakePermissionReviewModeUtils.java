package com.google.android.clockwork.settings.connectivity;

import android.annotation.Nullable;
import android.annotation.StringRes;

import java.util.Arrays;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

/* Util functions used for permission review mode. */
public class FakePermissionReviewModeUtils implements PermissionReviewModeUtils {
    private final boolean mPermissionReviewModeEnabled;
    private final Set<String> mCmiitWhitelistedPackages = new HashSet<>();

    public FakePermissionReviewModeUtils(boolean enabled) {
        mPermissionReviewModeEnabled = enabled;
    }

    @Override
    @Nullable
    public CharSequence getAppLabelFromPackage(@Nullable String packageName) {
        return packageName;
    }

    @Override
    public CharSequence getConsentDialogTitle(
            CharSequence appLabel, @StringRes int resId, @StringRes int defaultResId) {
        return appLabel;
    }

    @Override
    public boolean isPermissionReviewModeEnabled() {
        return mPermissionReviewModeEnabled;
    }

    @Override
    public boolean isPackageWhitelistedForOmittingCmiitDialog(@Nullable String packageName) {
        return mPermissionReviewModeEnabled && mCmiitWhitelistedPackages.contains(packageName);
    }

    public void addCmiitWhitelistedPackage(String packageName) {
        mCmiitWhitelistedPackages.add(packageName);
    }
}
