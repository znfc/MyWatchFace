package com.google.android.clockwork.settings.testing;

import static org.junit.Assert.assertEquals;
import static org.robolectric.Shadows.shadowOf;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowPackageManager;

/** Helper methods for testing Voice Integration on LE devices */
public class VoiceProvidersLeUtils {

    public static void setUpDisabledPackage(String packageName) {
        setUpPackage(packageName, "", /*enabled=*/ false);
    }

    public static void setUpEnabledPackage(String packageName) {
        setUpPackage(packageName, "", /*enabled=*/ true);
    }

    public static void setUpDisabledPackage(String packageName, String label) {
        setUpPackage(packageName, label, /*enabled=*/ false);
    }

    public static void setUpEnabledPackage(String packageName, String label) {
        setUpPackage(packageName, label, /*enabled=*/ true);
    }

    private static void setUpPackage(String packageName, String label, boolean enabled) {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = packageName;
        packageInfo.applicationInfo = new ApplicationInfo();
        packageInfo.applicationInfo.packageName = packageName;
        packageInfo.applicationInfo.name = label;
        packageInfo.applicationInfo.enabled = enabled;
        ShadowPackageManager shadowPackageManager =
                shadowOf(RuntimeEnvironment.application.getPackageManager());
        shadowPackageManager.addPackage(packageInfo);
    }

    public static void assertPackageEnabled(String packageName, int enabledState) {
        assertEquals(
                enabledState,
                RuntimeEnvironment.application
                        .getPackageManager()
                        .getApplicationEnabledSetting(packageName));
    }
}
