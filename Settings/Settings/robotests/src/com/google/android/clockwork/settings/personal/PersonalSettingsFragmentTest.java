package com.google.android.clockwork.settings.personal;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.Shadows.shadowOf;

import android.content.ContentProvider;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.HotwordConfig;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.SmartReplyConfig;
import com.google.android.clockwork.settings.utils.FeatureManager;
import com.google.android.clockwork.settings.utils.SettingsCursor;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowContentResolver;
import org.robolectric.util.FragmentTestUtil;
import java.util.Locale;

/** Test personal settings fragment */
@RunWith(ClockworkRobolectricTestRunner.class)
public class PersonalSettingsFragmentTest {
    private static final String FEATURE_CN_GOOGLE = "cn.google";
    private static final String KEY_PREF_DEVICE_ADMIN = "pref_deviceAdministration";
    private static final String KEY_PREF_YOLO = "pref_yolo";
    private static final String KEY_PREF_ACCOUNTS = "pref_accounts";

    private Context mContext;
    private PersonalSettingsFragment mFragment;

    @Mock private HotwordConfig mHotwordConfig;
    @Mock private Resources mResources;
    @Mock private SwitchPreference mTestPreference;
    @Mock private SmartReplyConfig mSmartReplyConfig;
    @Mock private PreferenceScreen mPreferenceScreen;
    @Mock private ContentProvider contentProvider;
    @Mock private FeatureManager featureManager;

    @Before
    public void setup() {
        initMocks(this);

        ShadowContentResolver.registerProviderInternal(
                SettingsContract.SETTINGS_AUTHORITY, contentProvider);
        Cursor cursor =
                new SettingsCursor(
                        SettingsContract.KEY_RETAIL_MODE,
                        String.valueOf(SettingsContract.RETAIL_MODE_CONSUMER));
        when(contentProvider.query(SettingsContract.RETAIL_MODE_URI, null, null, null, null))
                .thenReturn(cursor);

        mContext = ShadowApplication.getInstance().getApplicationContext();
        FeatureManager.INSTANCE.setTestInstance(featureManager);
        mFragment = new PersonalSettingsFragment();
    }

    @After
    public void tearDown() {
        FeatureManager.INSTANCE.clearTestInstance();
    }

    @Test
    public void initSmartReplyPref_doNothingForNullPref() {
        // No exception thrown out.
        mFragment.initSmartReplyPref(null, false /* isLeDevice */,
                mSmartReplyConfig, Locale.ENGLISH, mPreferenceScreen);
    }

    @Test
    public void initSmartReplyPref_showInLeOnChinese() {
        mFragment.initSmartReplyPref(mTestPreference, true /* isLeDevice */,
                mSmartReplyConfig, Locale.SIMPLIFIED_CHINESE, mPreferenceScreen);

        verifyZeroInteractions(mPreferenceScreen);
    }

    @Test
    public void initSmartReplyPref_hideInLeOnNonChinese() {
        mFragment.initSmartReplyPref(mTestPreference, true /* isLeDevice */,
                mSmartReplyConfig, Locale.ENGLISH, mPreferenceScreen);

        verify(mPreferenceScreen).removePreference(mTestPreference);
    }

    @Test
    public void initSmartReplyPref_showInRoWOnEnglish() {
        mFragment.initSmartReplyPref(mTestPreference, false /* isLeDevice */,
                mSmartReplyConfig, Locale.ENGLISH, mPreferenceScreen);

        verifyZeroInteractions(mPreferenceScreen);
    }

    @Test
    public void initSmartReplyPref_hideInRoWOnNonEnglish() {
        mFragment.initSmartReplyPref(mTestPreference, false /* isLeDevice */,
                mSmartReplyConfig, Locale.SIMPLIFIED_CHINESE, mPreferenceScreen);

        verify(mPreferenceScreen).removePreference(mTestPreference);
    }

    @Test
    public void initButtonPref_doNothingForNullPref() {
        // No exception thrown out.
        mFragment.initButtonPref(null);
    }

    @Test
    public void initExerciseDetectionPref_doNothingForNullPref() {
        // No exception thrown out.
        mFragment.initExerciseDetectionPref(null);
    }

    @Test
    public void initDeviceAdminPref_doNothingForNullPref() {
        // No exception thrown out.
        mFragment.initDeviceAdminPref(null);
    }

    @Test
    public void initYoloPref_doNothingForNullPref() {
        // No exception thrown out.
        mFragment.initYoloPref(null);
    }

    @Test
    public void initHotwordDetectionPrefStateOn() {
        when(mHotwordConfig.isHotwordDetectionEnabled()).thenReturn(true);

        // No exception thrown.
        mFragment.initHotwordDetection(
                mTestPreference,
                mHotwordConfig,
                false,
                mResources);
        verify(mTestPreference).setChecked(eq(true));
    }

    @Test
    public void initHotwordDetectionPrefStateOff() {
        when(mHotwordConfig.isHotwordDetectionEnabled()).thenReturn(false);

        // No exception thrown.
        mFragment.initHotwordDetection(
                mTestPreference,
                mHotwordConfig,
                false,
                mResources);
        verify(mTestPreference).setChecked(eq(false));
    }

    @Test
    public void initHotwordDetectionPrefText() {
        // No exception thrown.
        mFragment.initHotwordDetection(
                mTestPreference,
                Mockito.mock(HotwordConfig.class),
                false,
                mResources);
        verifyZeroInteractions(mResources);
    }

    @Test
    public void initHotwordDetectionPrefTextLe() {
        // No exception thrown.
        mFragment.initHotwordDetection(
                mTestPreference,
                Mockito.mock(HotwordConfig.class),
                true,
                mResources);
        verify(mResources).getText(eq(R.string.pref_hotwordDetectionLe));
    }

    @Test
    public void rowEditionHasAllPrefs() {
        when(featureManager.isLocalEditionDevice()).thenReturn(false);

        FragmentTestUtil.startFragment(mFragment);
        Assert.assertNotNull(mFragment.findPreference(KEY_PREF_DEVICE_ADMIN));
        Assert.assertNotNull(mFragment.findPreference(KEY_PREF_ACCOUNTS));
    }

    @Test
    public void localEditionHidesPrefs() {
        when(featureManager.isLocalEditionDevice()).thenReturn(true);

        FragmentTestUtil.startFragment(mFragment);
        Assert.assertNull(mFragment.findPreference(KEY_PREF_DEVICE_ADMIN));
        Assert.assertNull(mFragment.findPreference(KEY_PREF_YOLO));
        Assert.assertNull(mFragment.findPreference(KEY_PREF_ACCOUNTS));
    }
}
