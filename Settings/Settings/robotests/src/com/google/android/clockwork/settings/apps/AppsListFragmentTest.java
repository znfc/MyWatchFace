package com.google.android.clockwork.settings.apps;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import android.content.pm.ApplicationInfo;
import android.graphics.drawable.ColorDrawable;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.util.FragmentTestUtil;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

@Ignore("b/37678947 and b/31830217")
@RunWith(ClockworkRobolectricTestRunner.class)
public class AppsListFragmentTest {
    private static final String MORE_APPS_PREF = "pref_moreApps";
    private static final String PACKAGE_1 = "package.1";
    private static final String SYS_PACKAGE_1 = "sys.package.1";

    private AppsListFragment mFragment;
    private PreferenceScreen mPrefScreen;

    @Mock private ApplicationsState mMockState;
    @Mock private ApplicationsState.Session mMockSession;
    @Mock private AppEntry mMockAppEntry;
    @Mock private AppEntry mMockSysAppEntry;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        setupAppEntry(mMockAppEntry, PACKAGE_1, false /* isSystem */);
        setupAppEntry(mMockSysAppEntry, SYS_PACKAGE_1, true /* isSystem */);

        mFragment = new AppsListFragment() {
            @Override
            protected int getAppsTitleResId() {
                return 0;
            }

            @Override
            protected int getSystemAppsTitleResId() {
                return 0;
            }
        };
        mFragment.setStateAndSession(mMockState, mMockSession);
        FragmentTestUtil.startFragment(mFragment);
        mPrefScreen = mFragment.getPreferenceScreen();
    }

    @Test
    public void testShowLoadedApps_NullApps() {
        mFragment.mIsSystemAppView = false;
        mFragment.showLoadedApps(mPrefScreen, null);
        Assert.assertEquals("Wrong number of preferences", 1, mPrefScreen.getPreferenceCount());
    }

    @Test
    public void testShowLoadedApps_OneAppSysAppNull() {
        ArrayList<AppEntry> apps = new ArrayList<>();
        apps.add(mMockAppEntry);
        addMorePrefs();

        mFragment.mIsSystemAppView = false;
        mFragment.showLoadedApps(mPrefScreen, apps);

        Assert.assertEquals("Wrong number of preferences", 1, mPrefScreen.getPreferenceCount());
        Assert.assertEquals("Title does not match", PACKAGE_1,
                mPrefScreen.getPreference(0).getTitle());
    }

    @Test
    public void testShowLoadedApps_OneAppSysAppNotNull() {
        ArrayList<AppEntry> apps = new ArrayList<>();
        apps.add(mMockAppEntry);
        apps.add(mMockSysAppEntry);
        addMorePrefs();

        mFragment.mIsSystemAppView = false;
        mFragment.showLoadedApps(mPrefScreen, apps);

        Assert.assertEquals("Wrong number of preferences", 2, mPrefScreen.getPreferenceCount());
        Assert.assertEquals("Title does not match", PACKAGE_1,
                mPrefScreen.getPreference(0).getTitle());
        Assert.assertEquals("Title should be \"System Apps\"",
                R.string.system_apps_settings, mPrefScreen.getPreference(1).getTitleRes());
    }

    @Test
    public void testShowLoadedApps_OneSysAppSystemView() {
        ArrayList<AppEntry> apps = new ArrayList<>();
        apps.add(mMockAppEntry);
        apps.add(mMockSysAppEntry);
        addMorePrefs();

        mFragment.mIsSystemAppView = true;
        mFragment.showLoadedApps(mPrefScreen, apps);

        Assert.assertEquals("Wrong number of preferences", 1, mPrefScreen.getPreferenceCount());
        Assert.assertEquals("Title does not match", SYS_PACKAGE_1,
                mPrefScreen.getPreference(0).getTitle());
    }

    @Test
    public void testShowLoadedApps_NullAppOneSysAppSystemView() {
        ArrayList<AppEntry> apps = new ArrayList<>();
        apps.add(mMockSysAppEntry);
        addMorePrefs();

        mFragment.mIsSystemAppView = true;
        mFragment.showLoadedApps(mPrefScreen, apps);

        Assert.assertEquals("Wrong number of preferences", 1, mPrefScreen.getPreferenceCount());
        Assert.assertEquals("Title does not match", SYS_PACKAGE_1,
                mPrefScreen.getPreference(0).getTitle());
    }

    private void setupAppEntry(AppEntry appEntry, final String packageName, boolean isSystem) {
        appEntry.label = packageName;
        appEntry.icon = new ColorDrawable();
        appEntry.info = new ApplicationInfo();
        appEntry.info.packageName = packageName;
        if (isSystem) {
            appEntry.info.flags |= ApplicationInfo.FLAG_SYSTEM;
        }
    }

    private void addMorePrefs() {
        Preference moreAppsPref = new Preference(mFragment.getActivity());
        moreAppsPref.setKey(MORE_APPS_PREF);
        moreAppsPref.setTitle(R.string.system_apps_settings);
        mPrefScreen.addPreference(moreAppsPref);
    }
}
