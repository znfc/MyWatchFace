package com.google.android.clockwork.settings.apps;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import android.content.pm.ApplicationInfo;
import android.graphics.drawable.ColorDrawable;
import android.preference.PreferenceScreen;

import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.Session;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

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

import java.io.File;
import java.util.ArrayList;

@Ignore("b/37678947")
@RunWith(ClockworkRobolectricTestRunner.class)
public class AppStorageSettingsFragmentTest {

    private static final String PACKAGE_PREFIX = "package_";
    private static final long KB = 1024;
    private static final long MB = 1024 * KB;

    private AppStorageSettingsFragment mFragment;
    private PreferenceScreen mPrefScreen;

    @Mock
    private ApplicationsState mMockState;
    @Mock
    private ApplicationsState.Session mMockSession;

    @Mock
    private AppEntry mMockAppEntry1;
    @Mock
    private AppEntry mMockAppEntry2;
    @Mock
    private AppEntry mMockAppEntry3;
    @Mock
    private AppEntry mMockAppEntry4;

    @Mock
    private File mMockApk1;
    @Mock
    private File mMockApk2;
    @Mock
    private File mMockApk3;
    @Mock
    private File mMockApk4;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        setupAppEntries(mMockAppEntry1, mMockApk1,1);
        setupAppEntries(mMockAppEntry2, mMockApk2,2);
        setupAppEntries(mMockAppEntry3, mMockApk3,3);
        setupAppEntries(mMockAppEntry4, mMockApk4,4);

        mFragment = new AppStorageSettingsFragment();
        mFragment.setStateAndSession(mMockState, mMockSession);
        FragmentTestUtil.startFragment(mFragment);
        mPrefScreen = mFragment.getPreferenceScreen();
    }

    @Test
    public void testShowLoadedApps_SortDataSize() {
        ArrayList<AppEntry> apps = new ArrayList<>();
        apps.add(mMockAppEntry1);
        apps.add(mMockAppEntry2);
        apps.add(mMockAppEntry3);
        apps.add(mMockAppEntry4);

        mFragment.mCurrentSort = true;
        mFragment.showLoadedApps(mPrefScreen, apps);

        Assert.assertEquals("Wrong number of preferences", 5, mPrefScreen.getPreferenceCount());
        Assert.assertEquals("Title does not match", PACKAGE_PREFIX + 1,
                mPrefScreen.getPreference(4).getTitle());
        Assert.assertEquals("Title does not match", PACKAGE_PREFIX + 2,
                mPrefScreen.getPreference(3).getTitle());
        Assert.assertEquals("Title does not match", PACKAGE_PREFIX + 3,
                mPrefScreen.getPreference(2).getTitle());
        Assert.assertEquals("Title does not match", PACKAGE_PREFIX + 4,
                mPrefScreen.getPreference(1).getTitle());
    }

    @Test
    public void testShowLoadedApps_SortAppSize() {
        ArrayList<AppEntry> apps = new ArrayList<>();
        apps.add(mMockAppEntry1);
        apps.add(mMockAppEntry2);
        apps.add(mMockAppEntry3);
        apps.add(mMockAppEntry4);

        mFragment.mCurrentSort = false;
        mFragment.showLoadedApps(mPrefScreen, apps);

        Assert.assertEquals("Wrong number of preferences", 5, mPrefScreen.getPreferenceCount());
        Assert.assertEquals("Title does not match", PACKAGE_PREFIX + 1,
                mPrefScreen.getPreference(1).getTitle());
        Assert.assertEquals("Title does not match", PACKAGE_PREFIX + 2,
                mPrefScreen.getPreference(2).getTitle());
        Assert.assertEquals("Title does not match", PACKAGE_PREFIX + 3,
                mPrefScreen.getPreference(3).getTitle());
        Assert.assertEquals("Title does not match", PACKAGE_PREFIX + 4,
                mPrefScreen.getPreference(4).getTitle());
    }

    private void setupAppEntries(AppEntry appEntry, File apk, int index) {
        appEntry.icon = new ColorDrawable();
        appEntry.info = new ApplicationInfo();
        appEntry.info.packageName = PACKAGE_PREFIX + index;
        appEntry.label = PACKAGE_PREFIX + index;
        appEntry.cacheSize = index * KB;
        appEntry.dataSize = index * 10 * KB;
        ReflectionHelpers.setField(appEntry, "apkFile", apk);
        when(apk.length()).thenReturn((5 - index) * MB);
    }
}
