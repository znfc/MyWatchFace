package com.google.android.clockwork.settings.apps;

import static com.google.android.clockwork.settings.apps.AppInfoBase.ARG_PACKAGE_NAME;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.Utils;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.FragmentTestUtil;
import org.robolectric.util.ReflectionHelpers;

import java.util.List;

@Ignore("b/37678947")
@RunWith(ClockworkRobolectricTestRunner.class)
public class AppDetailsFragmentTest {
    private static final String PACKAGE_1 = "package.1";
    private static final List<Integer> EXPECTED_ENTRIES = ImmutableList.of(
        R.string.app_label_force_stop, R.string.permissions_setting,
        R.string.app_advanced_settings_title, R.string.pref_app_about);
    private static final List<String> EXPECTED_ENTRIES_TITLE = ImmutableList.of(
        "Force Stop?", "Permissions", "Advanced", "App Info");
    private static final String KEY_PREF_FORCE_STOP = "pref_force_stop";

    private AppDetailsFragment mFragment;
    private PreferenceScreen mPrefScreen;
    @Mock private ApplicationsState mMockState;
    @Mock private ApplicationsState.Session mMockSession;
    @Mock private ApplicationsState.AppEntry mMockAppEntry;
    @Mock private PackageManager mMockPackageManager;

    /** Fake version of the fragment which includes getContext(), added in SDK24. */
    private class FakeAppDetailsFragment extends AppDetailsFragment {
        @Override
        public Context getContext() {
            return RuntimeEnvironment.application;
        }
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(mMockState.newSession(any())).thenReturn(mMockSession);
        when(mMockState.getEntry(eq(PACKAGE_1), anyInt())).thenReturn(mMockAppEntry);

        mMockAppEntry.info = new ApplicationInfo();
        mMockAppEntry.info.packageName = PACKAGE_1;

        mFragment = new FakeAppDetailsFragment();
        mFragment.setStateAndSession(mMockState, mMockSession);
        mFragment.setArguments(new Bundle());
        mFragment.getArguments().putString(ARG_PACKAGE_NAME, PACKAGE_1);
        FragmentTestUtil.startFragment(mFragment);
        mPrefScreen = mFragment.getPreferenceScreen();
    }

    @Test
    public void testUninstall_RegularApp() {
        mMockAppEntry.info.flags = 0; // Not a system app.
        mMockAppEntry.info.enabled = false;
        mFragment.refreshPrefs(mMockAppEntry);
        Assert.assertEquals(EXPECTED_ENTRIES.size() + 1, mPrefScreen.getPreferenceCount());
        Assert.assertEquals("First entry should be \"Uninstall\"", R.string.app_label_uninstall,
                mPrefScreen.getPreference(0).getTitleRes());
        checkCommonPreferences();
    }

    @Test
    public void testUninstall_SystemAppNotUpdated_Disabled() {
        mMockAppEntry.info.flags = ApplicationInfo.FLAG_SYSTEM;
        mMockAppEntry.info.enabled = false;
        mFragment.refreshPrefs(mMockAppEntry);
        Assert.assertEquals(EXPECTED_ENTRIES.size() + 1, mPrefScreen.getPreferenceCount());
        Assert.assertEquals("First entry should be \"Enable\"", R.string.app_label_enable,
                mPrefScreen.getPreference(0).getTitleRes());
        checkCommonPreferences();
    }

    @Test
    public void testUninstall_SystemAppNotUpdated_Enabled() {
        mMockAppEntry.info.flags = ApplicationInfo.FLAG_SYSTEM;
        mMockAppEntry.info.enabled = true;
        mFragment.refreshPrefs(mMockAppEntry);
        Assert.assertEquals(EXPECTED_ENTRIES.size() + 1, mPrefScreen.getPreferenceCount());
        Assert.assertEquals("First entry should be \"Disable\"", R.string.app_label_disable,
                mPrefScreen.getPreference(0).getTitleRes());
        checkCommonPreferences();
    }

    @Test
    public void testUninstall_SystemAppUpdated() {
        mMockAppEntry.info.flags =
                ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;
        mMockAppEntry.info.enabled = false;
        mFragment.refreshPrefs(mMockAppEntry);
        Assert.assertEquals(EXPECTED_ENTRIES.size() + 1, mPrefScreen.getPreferenceCount());
        Assert.assertEquals("First entry should be \"Remove Upgrades\"",
                R.string.app_label_remove_upgrades, mPrefScreen.getPreference(0).getTitleRes());
        checkCommonPreferences();
    }

    @Test
    public void testStopped() {
        mMockAppEntry.info.flags = ApplicationInfo.FLAG_STOPPED;
        mMockAppEntry.info.enabled = false;
        mFragment.refreshPrefs(mMockAppEntry);

        // The removal from the preference list is delegated to the displaying activity.
        // We just check that the value has been set correctly.
        Assert.assertFalse("The \"Force Stop\" preference should be disabled.",
            mPrefScreen.findPreference(KEY_PREF_FORCE_STOP).isEnabled());
    }

    @Test
    public void testDisableAllowed_homeApp() {
        ArrayList<ResolveInfo> homeActivities = new ArrayList<>();
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = new ActivityInfo();
        resolveInfo.activityInfo.packageName = PACKAGE_1;
        homeActivities.add(resolveInfo);

        Assert.assertFalse("Disable should be disallowed for Home app.",
            AppDetailsFragment.isDisableAllowed(
                RuntimeEnvironment.application,
                PACKAGE_1,
                new PackageInfo(),
                homeActivities));
    }

    @Test
    public void testDisableAllowed_systemApp() {
        // Pretend this is a system app
        ReflectionHelpers.setStaticField(Utils.class, "sSharedSystemSharedLibPackageName",
                PACKAGE_1);
        ReflectionHelpers.setStaticField(Utils.class, "sServicesSystemSharedLibPackageName",
            PACKAGE_1);

        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = PACKAGE_1;
        Assert.assertFalse("Disable should be disallowed for Home app.",
            AppDetailsFragment.isDisableAllowed(
                RuntimeEnvironment.application,
                PACKAGE_1,
                packageInfo,
                null));
    }

    private void checkCommonPreferences() {
        for (int i = 1; i < mPrefScreen.getPreferenceCount(); i++) {
            Preference preference = mPrefScreen.getPreference(i);
            Assert.assertEquals("Entry " + i + " should be \"" + EXPECTED_ENTRIES_TITLE.get(i - 1)
                + "\" but was " + preference.getTitle(),
                EXPECTED_ENTRIES.get(i - 1), (Integer) preference.getTitleRes());
        }
    }
}
