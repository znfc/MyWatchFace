package com.google.android.clockwork.settings;

import static com.google.android.clockwork.settings.testing.VoiceProvidersLeUtils.assertPackageEnabled;
import static com.google.android.clockwork.settings.testing.VoiceProvidersLeUtils.setUpDisabledPackage;
import static com.google.android.clockwork.settings.testing.VoiceProvidersLeUtils.setUpEnabledPackage;
import static org.junit.Assert.assertEquals;
import static org.robolectric.Shadows.shadowOf;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowPackageManager;

/** Test retail service mode operation */
@RunWith(ClockworkRobolectricTestRunner.class)
public class RetailServiceTest {

    private static final String VOICE_PROVIDER_PACKAGE1 = "package.one";
    private static final String VOICE_PROVIDER_PACKAGE2 = "package.two";
    private static final String VOICE_PROVIDER_PACKAGE3 = "package.three";

    private static final String ACTIVITY_NAME1 = VOICE_PROVIDER_PACKAGE1 + ".Activity1";
    private static final String ACTIVITY_NAME2 = VOICE_PROVIDER_PACKAGE1 + ".Activity2";

    private static final String VOICE_ASSIST_ACTION = "android.intent.action.VOICE_ASSIST";

    private PackageManager mPackageManager;

    @Before
    public void setup() {
        mPackageManager = RuntimeEnvironment.application.getPackageManager();
    }

    @Test
    public void testDisableAlternativeVoiceProvidersLe() {
        // package1 not installed
        setUpDisabledPackage(VOICE_PROVIDER_PACKAGE2);
        setUpEnabledPackage(VOICE_PROVIDER_PACKAGE3);

        ShadowPackageManager shadowPackageManager = shadowOf(mPackageManager);

        RetailService.disableAlternativeVoiceProvidersLe(
                mPackageManager,
                new String[] {
                    VOICE_PROVIDER_PACKAGE1, VOICE_PROVIDER_PACKAGE2, VOICE_PROVIDER_PACKAGE3
                });

        assertPackageEnabled(
                VOICE_PROVIDER_PACKAGE2, PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
        assertPackageEnabled(
                VOICE_PROVIDER_PACKAGE3, PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
    }

    @Test
    public void testDisableActivitiesForRetailModeLe() {
        ShadowPackageManager shadowPackageManager = shadowOf(mPackageManager);
        setUpActivity(
                shadowPackageManager,
                VOICE_PROVIDER_PACKAGE1,
                ACTIVITY_NAME1,
                /*priority=*/ -1);
        setUpActivity(
                shadowPackageManager,
                VOICE_PROVIDER_PACKAGE2,
                ACTIVITY_NAME2,
                /*priority=*/ -2);
        ComponentName component1 = new ComponentName(VOICE_PROVIDER_PACKAGE1, ACTIVITY_NAME1);
        ComponentName component2 = new ComponentName(VOICE_PROVIDER_PACKAGE2, ACTIVITY_NAME2);
        mPackageManager.setComponentEnabledSetting(
                component1, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, /*flags=*/ 0);
        mPackageManager.setComponentEnabledSetting(
                component2, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, /*flags=*/ 0);

        RetailService.disableActivitiesForRetailModeLe(mPackageManager);

        assertEquals(
                mPackageManager.getComponentEnabledSetting(component1),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
        assertEquals(
                mPackageManager.getComponentEnabledSetting(component2),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
    }

    private void setUpActivity(
            ShadowPackageManager robolectricPackageManager,
            String packageName,
            String activityName,
            int priority) {
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = new ActivityInfo();
        resolveInfo.activityInfo.packageName = packageName;
        resolveInfo.activityInfo.name = activityName;
        resolveInfo.priority = priority;
        Intent assistIntent = new Intent(VOICE_ASSIST_ACTION);
        robolectricPackageManager.addResolveInfoForIntent(assistIntent, resolveInfo);
    }
}
