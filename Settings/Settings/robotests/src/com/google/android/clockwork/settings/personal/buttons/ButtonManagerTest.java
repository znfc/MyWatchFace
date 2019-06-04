package com.google.android.clockwork.settings.personal.buttons;

import static android.view.KeyEvent.KEYCODE_1;
import static android.view.KeyEvent.KEYCODE_STEM_1;
import static android.view.KeyEvent.KEYCODE_STEM_2;
import static android.view.KeyEvent.KEYCODE_STEM_3;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.DefaultSettingsContentResolver;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.provider.SettingsProvider;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;

import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

@RunWith(ClockworkRobolectricTestRunner.class)
public class ButtonManagerTest {
    private ComponentName mButton1Component =
            new ComponentName("com.test.pkg", "com.test.button1");
    private ComponentName mButton2Component =
            new ComponentName("com.test.pkg", "com.test.button2");
    private ComponentName mButton3Component =
            new ComponentName("com.test.pkg", "com.test.button3");
    private ComponentName mButtonUnknownComponent =
            new ComponentName("com.test.pkg", "com.test.buttonUnknown");

    private ButtonManager mButtonManager;
    private SettingsProvider mSettingsProvider;
    private ContentResolver mContentResolver;

    @Mock private Resources mMockResources;
    @Mock private PackageManager mMockPackageManager;

    @Before
    public void setup() {
        initMocks(this);

        Context context = RuntimeEnvironment.application;
        mSettingsProvider = new SettingsProvider();
        ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.authority = SettingsContract.SETTINGS_AUTHORITY;
        mSettingsProvider.attachInfoForTesting(context, providerInfo);

        mContentResolver = context.getContentResolver();

        mButtonManager = new ButtonManager(context, mContentResolver, mMockResources,
                mMockPackageManager);
    }

    @Test
    public void testDefaultValues_Type() {
        // Check the default values for type
        Assert.assertEquals(Constants.STEM_TYPE_APP_LAUNCH, getButtonTypeFromPref(KEYCODE_STEM_1));
        Assert.assertEquals(Constants.STEM_TYPE_APP_LAUNCH, getButtonTypeFromPref(KEYCODE_STEM_2));
        Assert.assertEquals(Constants.STEM_TYPE_APP_LAUNCH, getButtonTypeFromPref(KEYCODE_STEM_3));
        try {
            Assert.assertEquals(Constants.STEM_TYPE_APP_LAUNCH,
                    getButtonTypeFromPref(KEYCODE_1));
            Assert.assertFalse("Exception should be thrown for unknown keyCode: "
                            + KEYCODE_1, true);
        } catch (IllegalArgumentException e ) {
            // expected.
        }
    }

    @Test
    public void testDefaultValues_Data() {
        // Check the default values for type
        Assert.assertNull(getButtonDataFromPref(KEYCODE_STEM_1));
        Assert.assertNull(getButtonDataFromPref(KEYCODE_STEM_2));
        Assert.assertNull(getButtonDataFromPref(KEYCODE_STEM_3));
        try {
            Assert.assertNull(getButtonDataFromPref(KEYCODE_1));
            Assert.assertFalse("Exception should be thrown for key: " + KEYCODE_1,
                    true);
        } catch (IllegalArgumentException e ) {
            // expected.
        }
    }

    @Test
    public void testSaveButtonSettings() {
        Assert.assertEquals(Constants.STEM_TYPE_APP_LAUNCH, getButtonTypeFromPref(KEYCODE_STEM_1));
        mButtonManager.saveButtonSettings(KEYCODE_STEM_1, Constants.STEM_TYPE_CONTACT_LAUNCH,
                "data");
        Assert.assertEquals(Constants.STEM_TYPE_CONTACT_LAUNCH,
                getButtonTypeFromPref(KEYCODE_STEM_1));
        Assert.assertEquals("data",
                getButtonDataFromPref(KEYCODE_STEM_1));
    }

    @Test
    public void testIntentForButton_fallbackCase() {
        // GIVEN
        ComponentName expectedComponent = new ComponentName(
                "com.google.android.apps.wearable.settings",
                "com.google.android.clockwork.settings.MainSettingsActivity");

        // WHEN
        Intent intent = mButtonManager.getIntentForButton(KEYCODE_STEM_1, null, null);

        // THEN
        Assert.assertNotNull(intent);
        Assert.assertEquals("com.google.android.clockwork.settings.BUTTON_SETTINGS",
                intent.getAction());
        Assert.assertEquals(expectedComponent, intent.getComponent());
    }

    @Test
    public void testIntentForButton_savedIntentCase() {
        setupDefaultIntent(KEYCODE_STEM_1, true);

        // WHEN
        Intent intent = mButtonManager.getIntentForButton(KEYCODE_STEM_1, null, null);

        // THEN
        Assert.assertNotNull(intent);
        Assert.assertEquals(mButton1Component, intent.getComponent());
    }

    @Test
    public void testIntentForButton_oldComponentToNewComponentMatches() {
        setupDefaultIntent(KEYCODE_STEM_1, true);
        setupDefaultIntent(KEYCODE_STEM_2, true);

        // WHEN
        Intent intent = mButtonManager.getIntentForButton(
                KEYCODE_STEM_1,
                mButton1Component.flattenToString(),
                mButton2Component.flattenToString());

        // THEN
        Assert.assertNotNull(intent);
        Assert.assertEquals(mButton2Component, intent.getComponent());
    }

    @Test
    public void testIntentForButton_oldComponentToNewComponentDoesntMatch() {
        setupDefaultIntent(KEYCODE_STEM_1, true);
        setupDefaultIntent(KEYCODE_STEM_2, true);
        setupDefaultIntent(KEYCODE_STEM_3, true);

        // WHEN
        Intent intent = mButtonManager.getIntentForButton(
                KEYCODE_STEM_1,
                mButton2Component.flattenToString(),
                mButton3Component.flattenToString());

        // THEN
        Assert.assertNotNull(intent);
        Assert.assertEquals(mButton1Component, intent.getComponent());
    }


    @Test
    public void testIntentForButton_uncallableSavedIntentCase() {
        setupDefaultIntent(KEYCODE_STEM_1, false);

        Intent intent = mButtonManager.getIntentForButton(KEYCODE_STEM_1, null, null);

        ComponentName expectedComponent = new ComponentName(
                "com.google.android.apps.wearable.settings",
                "com.google.android.clockwork.settings.MainSettingsActivity");
        Assert.assertNotNull(intent);
        Assert.assertEquals("com.google.android.clockwork.settings.BUTTON_SETTINGS",
                intent.getAction());
        Assert.assertEquals(expectedComponent, intent.getComponent());
    }

    @Test
    public void testGetFriendlySummary() {
        setupDefaultIntent(KEYCODE_STEM_1, true);

        Assert.assertEquals("activity1", mButtonManager.getFriendlySummary(KEYCODE_STEM_1));
    }

    private int getButtonTypeFromPref(int buttonKeycode) {
        Uri settingsPath = new Uri.Builder().scheme("content")
                .authority(SettingsContract.SETTINGS_AUTHORITY)
                .path(ButtonManager.BUTTON_MANAGER_CONFIG_PATH).build();

        return new DefaultSettingsContentResolver(mContentResolver)
                .getIntValueForKey(
                        settingsPath,
                        ButtonUtils.getStemTypeKey(buttonKeycode),
                        Constants.STEM_TYPE_UNKNOWN);
    }

    private String getButtonDataFromPref(int buttonKeycode) {
        Uri settingsPath = new Uri.Builder().scheme("content")
                .authority(SettingsContract.SETTINGS_AUTHORITY)
                .path(ButtonManager.BUTTON_MANAGER_CONFIG_PATH).build();

        return new DefaultSettingsContentResolver(mContentResolver)
                .getStringValueForKey(
                        settingsPath,
                        ButtonUtils.getStemDataKey(buttonKeycode),
                        null);
    }

    private void setupDefaultIntent(int keyCode, boolean shouldResolve) {
        ComponentName componentName;
        String activityName;
        switch(keyCode) {
            case KEYCODE_STEM_1:
                componentName = mButton1Component;
                activityName = "activity1";
                break;
            case KEYCODE_STEM_2:
                componentName = mButton2Component;
                activityName = "activity2";
                break;
            case KEYCODE_STEM_3:
                componentName = mButton3Component;
                activityName = "activity3";
                break;
            default:
                componentName = mButtonUnknownComponent;
                activityName = "activityUnknown";
                break;
        }
        mButtonManager.saveButtonSettings(keyCode, Constants.STEM_TYPE_APP_LAUNCH,
                componentName.flattenToString());
        if (shouldResolve) {
            ActivityInfo activityInfo = getActivityInfoMock(componentName.getPackageName(),
                    activityName);
            try {
                when(mMockPackageManager.getActivityInfo(eq(componentName), anyInt()))
                        .thenReturn(activityInfo);
            } catch (PackageManager.NameNotFoundException e) {
                // ignore, for compilation
            }
            List<ResolveInfo> resolveInfos = new ArrayList<>(0);
            ResolveInfo resolveInfo = new ResolveInfo();
            resolveInfo.activityInfo = activityInfo;
            resolveInfos.add(resolveInfo);
            when(mMockPackageManager.queryIntentActivities(any(Intent.class), anyInt()))
                    .thenReturn(resolveInfos);
        }
    }

    private ActivityInfo getActivityInfoMock(String packageName, String activityName) {
        ActivityInfo activityInfo = mock(ActivityInfo.class);
        activityInfo.packageName = packageName;
        activityInfo.name = activityName;
        when(activityInfo.loadLabel(mMockPackageManager)).thenReturn(activityName);
        return activityInfo;
    }
}
