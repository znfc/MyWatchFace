package com.google.android.clockwork.settings.connectivity;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import android.content.Context;
import com.google.android.clockwork.settings.GuardianModeConfig;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.util.ReflectionHelpers;

@RunWith(ClockworkRobolectricTestRunner.class)
public class DefaultPermissionReviewModeUtilsTest {
    private boolean mOriginalShouldWhitelistTestPackage;
    private DefaultPermissionReviewModeUtils mUtils;
    @Mock GuardianModeConfig mockGuardianModeConfig;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        Context context = shadowApplication.getApplicationContext();
        mOriginalShouldWhitelistTestPackage = ReflectionHelpers.getStaticField(
                DefaultPermissionReviewModeUtils.class, "SHOULD_WHITELIST_TEST_PACKAGES");
        mUtils = new DefaultPermissionReviewModeUtils(
                context.getPackageManager(), context.getResources(), mockGuardianModeConfig);
        ReflectionHelpers.setStaticField(
                DefaultPermissionReviewModeUtils.class, "SHOULD_WHITELIST_TEST_PACKAGES", false);
        when(mockGuardianModeConfig.getGuardianModePackage()).thenReturn(null);
    }

    @After
    public void tearDown() {
        // Restore SHOULD_WHITELIST_TEST_PACKAGES original value
        ReflectionHelpers.setStaticField(
                DefaultPermissionReviewModeUtils.class,
                "SHOULD_WHITELIST_TEST_PACKAGES",
                mOriginalShouldWhitelistTestPackage);
    }

    @Test
    public void androidWearIsWhitelistedForCmiit() {
        mUtils.initializeForTest(true /* inPermissionReviewMode */);
        assertTrue(
                mUtils.isPackageWhitelistedForOmittingCmiitDialog(
                        "com.google.android.wearable.app"));
    }

    @Test
    public void gmsCoreIsWhitelistedForCmiit() {
        mUtils.initializeForTest(true /* inPermissionReviewMode */);
        assertTrue(mUtils.isPackageWhitelistedForOmittingCmiitDialog("com.google.android.gms"));
    }

    @Test
    public void setupwizardIsWhitelistedForCmiit() {
        mUtils.initializeForTest(true /* inPermissionReviewMode */);
        assertTrue(mUtils.isPackageWhitelistedForOmittingCmiitDialog("com.google.android.wearable.setupwizard"));
    }

    @Test
    public void voiceSearchIsNotWhitelistedForCmiit() {
        mUtils.initializeForTest(true /* inPermissionReviewMode */);
        assertFalse(
                mUtils.isPackageWhitelistedForOmittingCmiitDialog(
                        "com.mobvoi.ticwear.sidewearvoicesearch"));
    }

    @Test
    public void absentPackageNameNotWhitelistedForCmiit() {
        mUtils.initializeForTest(true /* inPermissionReviewMode */);
        assertFalse(mUtils.isPackageWhitelistedForOmittingCmiitDialog(null));
    }

    @Test
    public void neverWhitelistedInNonPermissionReviewMode() {
        mUtils.initializeForTest(false /* inPermissionReviewMode */);

        assertFalse(
                mUtils.isPackageWhitelistedForOmittingCmiitDialog(
                        "com.google.android.wearable.app"));

        assertFalse(mUtils.isPackageWhitelistedForOmittingCmiitDialog("com.google.android.gms"));

        assertFalse(
                mUtils.isPackageWhitelistedForOmittingCmiitDialog(
                        "com.mobvoi.ticwear.sidewearvoicesearch"));

        assertFalse(mUtils.isPackageWhitelistedForOmittingCmiitDialog(null));
    }

    @Test
    public void guardianModeIsWhitelistedForCmiit() {
        final String GUARDIAN_MODE_PACKAGE = "com.google.guardianmode";
        mUtils.initializeForTest(true /* inPermissionReviewMode */);

        assertFalse(mUtils.isPackageWhitelistedForOmittingCmiitDialog(GUARDIAN_MODE_PACKAGE));

        when(mockGuardianModeConfig.getGuardianModePackage()).thenReturn(GUARDIAN_MODE_PACKAGE);

        assertTrue(mUtils.isPackageWhitelistedForOmittingCmiitDialog(GUARDIAN_MODE_PACKAGE));
    }

    @Test
    public void wifiUtilIsWhitelistedForCmiit() {
        ReflectionHelpers.setStaticField(
                DefaultPermissionReviewModeUtils.class, "SHOULD_WHITELIST_TEST_PACKAGES", true);
        mUtils.initializeForTest(true /* inPermissionReviewMode */);
        assertTrue(
                mUtils.isPackageWhitelistedForOmittingCmiitDialog(
                        "com.android.tradefed.utils.wifi"));
    }

    @Test
    public void wifiUtilIsNotWhitelistedForCmiitIfNotShouldWhitelistTestPackage() {
        ReflectionHelpers.setStaticField(
                DefaultPermissionReviewModeUtils.class, "SHOULD_WHITELIST_TEST_PACKAGES", false);
        mUtils.initializeForTest(true /* inPermissionReviewMode */);
        assertFalse(
                mUtils.isPackageWhitelistedForOmittingCmiitDialog(
                        "com.android.tradefed.utils.wifi"));
    }

    @Test
    public void bluetoothTestsIsWhitelistedForCmiit() {
        ReflectionHelpers.setStaticField(
                DefaultPermissionReviewModeUtils.class, "SHOULD_WHITELIST_TEST_PACKAGES", true);
        mUtils.initializeForTest(true /* inPermissionReviewMode */);
        assertTrue(
                mUtils.isPackageWhitelistedForOmittingCmiitDialog(
                        "com.android.bluetooth.tests"));
    }

    @Test
    public void bluetoothTestsIsNotWhitelistedForCmiitIfNotShouldWhitelistTestPackage() {
        ReflectionHelpers.setStaticField(
                DefaultPermissionReviewModeUtils.class, "SHOULD_WHITELIST_TEST_PACKAGES", false);
        mUtils.initializeForTest(true /* inPermissionReviewMode */);
        assertFalse(
                mUtils.isPackageWhitelistedForOmittingCmiitDialog(
                        "com.android.bluetooth.tests"));
    }

    @Test
    public void stabilityTestIsWhitelistedForCmiit() {
        ReflectionHelpers.setStaticField(
                DefaultPermissionReviewModeUtils.class, "SHOULD_WHITELIST_TEST_PACKAGES", true);
        mUtils.initializeForTest(true /* inPermissionReviewMode */);
        assertTrue(
                mUtils.isPackageWhitelistedForOmittingCmiitDialog(
                        "com.google.android.clockwork.stabilitytests"));
    }

    @Test
    public void stabilityTestIsNotWhitelistedForCmiitIfNotShouldWhitelistTestPackage() {
        ReflectionHelpers.setStaticField(
                DefaultPermissionReviewModeUtils.class, "SHOULD_WHITELIST_TEST_PACKAGES", false);
        mUtils.initializeForTest(true /* inPermissionReviewMode */);
        assertFalse(
                mUtils.isPackageWhitelistedForOmittingCmiitDialog(
                        "com.google.android.clockwork.stabilitytests"));
    }

    @Test
    public void unregisteredTestPackageIsNotWhitelistedForCmiit() {
        ReflectionHelpers.setStaticField(
                DefaultPermissionReviewModeUtils.class, "SHOULD_WHITELIST_TEST_PACKAGES", true);
        mUtils.initializeForTest(true /* inPermissionReviewMode */);
        assertFalse(mUtils.isPackageWhitelistedForOmittingCmiitDialog("com.android.test.package"));
    }
}
