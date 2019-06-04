package com.google.android.clockwork.settings.gestures;

import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import com.google.android.clockwork.settings.utils.FeatureManager;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowPackageManager;
import org.robolectric.util.FragmentTestUtil;

/** Test gesture setting fragment */
@RunWith(ClockworkRobolectricTestRunner.class)
public class GesturesSettingsFragmentTest {
    private static final String FEATURE_CN_GOOGLE = "cn.google";
    private static final String KEY_PREF_MORE_TIPS = "pref_moreTips";

    private Context mContext;
    private FeatureManager mFeatureManager;
    private GesturesSettingsFragment mFragment;
    private final ShadowPackageManager mPackageManager =
        shadowOf(RuntimeEnvironment.application.getPackageManager());

    @Before
    public void setup() {
        mContext = ShadowApplication.getInstance().getApplicationContext();
        mFeatureManager = new FeatureManager();
        FeatureManager.INSTANCE.setTestInstance(mFeatureManager);
        mFragment = new GesturesSettingsFragment();
    }

    @After
    public void tearDown() {
        mPackageManager.setSystemFeature(FEATURE_CN_GOOGLE, false);
        FeatureManager.INSTANCE.clearTestInstance();
    }

    @Test
    @Ignore
    public void rowEditionHasAllPrefs() {
        mPackageManager.setSystemFeature(FEATURE_CN_GOOGLE, false);
        mFeatureManager.initialize(mContext);

        FragmentTestUtil.startFragment(mFragment);
        Assert.assertNotNull(mFragment.findPreference(KEY_PREF_MORE_TIPS));
    }

    @Test
    @Ignore
    public void localEditionHidesPrefs() {
        mPackageManager.setSystemFeature(FEATURE_CN_GOOGLE, true);
        mFeatureManager.initialize(mContext);

        FragmentTestUtil.startFragment(mFragment);
        Assert.assertNull(mFragment.findPreference(KEY_PREF_MORE_TIPS));
    }
}
