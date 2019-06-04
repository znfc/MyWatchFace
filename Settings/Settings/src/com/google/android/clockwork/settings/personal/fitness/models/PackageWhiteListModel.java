package com.google.android.clockwork.settings.personal.fitness.models;

import android.support.annotation.VisibleForTesting;
import com.google.android.clockwork.host.GKeys;
import java.util.Arrays;

/**
 * Gets whether particular packages or components are white listed and should be allowed to handle
 * Exercise Detection events.
 */
public class PackageWhiteListModel {

    private final String mSupportedPackages;

    public PackageWhiteListModel() {
        mSupportedPackages = GKeys.EXERCISE_DETECTION_WHITE_LIST.get();
    }

    @VisibleForTesting
    PackageWhiteListModel(String supportedPackages) {
        mSupportedPackages = supportedPackages;
    }

    /**
     * Returns {@code true} if the package is white listed. {@code packageOrComponent} may either be
     * a flattened component name or a package name.
     */
    public boolean isWhiteListed(CharSequence packageOrComponent) {
        // If the list is empty, consider that to mean all packages should be white listed.
        if ("".equals(mSupportedPackages)) {
            return true;
        }
        if (packageOrComponent == null) {
            return false;
        }

        packageOrComponent = getPackage((String) packageOrComponent);
        return Arrays.stream(mSupportedPackages.split(","))
                .anyMatch(packageOrComponent::equals);
    }

    @VisibleForTesting
    static String getPackage(String componentOrPackage) {
        int endIndex = componentOrPackage.indexOf("/");
        return endIndex < 0
                ? componentOrPackage
                : componentOrPackage.substring(0, endIndex);
    }
}
