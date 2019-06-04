package com.google.android.clockwork.settings.personal.fitness.models;

import static android.content.pm.PackageManager.MATCH_DEFAULT_ONLY;
import static com.google.android.clockwork.settings.SettingsIntents.EXTRA_COMPONENT_NAME;
import static com.google.android.clockwork.settings.SettingsIntents.EXTRA_ORIGINAL_INTENT;
import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.FIT_REALTIME_ACTIVITY;
import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.INTENTS;
import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.RUNNING_KEY;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Parcelable;
import com.google.android.apps.wearable.resolver.ResolverActivity;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.robolectric.RuntimeEnvironment;

@RunWith(ClockworkRobolectricTestRunner.class)
public class ExerciseAssociationModelTest {

    @Mock PackageManager mPackageManager;
    @Captor ArgumentCaptor<Intent> mIntentCaptor;
    @Spy private Context mContext = RuntimeEnvironment.application;
    private ExerciseAssociationModel mModel;
    private String mNoneLabel;
    private Intent mExpectedImplicitIntent = INTENTS.get(RUNNING_KEY);
    private String mWhiteListedName = "white listed";
    private String mUnsupportedName = "unsupported";
    private String[] mAppLabels = new String[] { mWhiteListedName, mUnsupportedName };
    private String[] mAppLabelsWithNone = new String[] {
            "replaced in setUp()",
            mAppLabels[0],
            mAppLabels[1],
    };

    private String mWhiteListedPackage = "com.whitelisted";
    private String mUnsupportedPackage = "com.unsupported";
    private String mWhiteListedActivity = "com.whitelisted.WhiteListedActivity";
    private String mUnsupportedActivity = "com.unsupported.UnsupportedActivity";
    private String mWhiteListedComponent = mWhiteListedPackage + "/" + mWhiteListedActivity;
    private String mUnsupportedComponent = mUnsupportedPackage + "/" + mUnsupportedActivity;
    private ComponentName mExpectedComponent =
            ComponentName.unflattenFromString(mWhiteListedComponent);
    private String[] mComponentNames = new String[] {
            mWhiteListedComponent,
            mUnsupportedComponent,
    };
    private String[] mComponentNamesWithNone = new String[] {
            "replaced in setUp()",
            mComponentNames[0],
            mComponentNames[1],
    };

    private ActivityInfo[] mActivityInfos = new ActivityInfo[] {
            makeActivityInfo(mWhiteListedPackage, mWhiteListedActivity),
            makeActivityInfo(mUnsupportedPackage, mUnsupportedActivity),
    };
    private List<ResolveInfo> mResolveInfos = new ArrayList<>(mAppLabels.length);

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        for (int i = 0; i < mAppLabels.length; i++) {
            mResolveInfos.add(getResolveInfoMock(mAppLabels[i], mActivityInfos[i]));
        }
        PackageWhiteListModel whiteListModel = new PackageWhiteListModel(mWhiteListedPackage);
        mModel = new ExerciseAssociationModel(mContext, whiteListModel, mPackageManager);

        mNoneLabel = mContext.getResources().getString(R.string.exerciseDetection_none);
        mComponentNamesWithNone[0] = mNoneLabel;
        mAppLabelsWithNone[0] = mNoneLabel;
    }

    @Test
    public void testGetNoneLabel() throws Exception {
        assertEquals(mNoneLabel, mModel.getNoneLabel());
    }

    @Test
    public void testHasDefaultApp() throws Exception {
        when(mPackageManager.resolveActivity(INTENTS.get(RUNNING_KEY), MATCH_DEFAULT_ONLY))
                .thenReturn(mResolveInfos.get(0));

        assertTrue(mModel.hasDefaultApp(RUNNING_KEY));
    }

    @Test
    public void testHasDefaultApp_none() throws Exception {
        when(mPackageManager.resolveActivity(INTENTS.get(RUNNING_KEY), MATCH_DEFAULT_ONLY))
                .thenReturn(null);

        assertFalse(mModel.hasDefaultApp(RUNNING_KEY));
    }

    @Test
    public void testHasDefaultApp_systemResolver() throws Exception {
        ComponentName resolver = new ComponentName(mContext, ResolverActivity.class);
        ActivityInfo activityInfo =
                makeActivityInfo(resolver.getPackageName(), resolver.getClassName());
        ResolveInfo info = getResolveInfoMock("System Resolver", activityInfo);
        when(mPackageManager.resolveActivity(INTENTS.get(RUNNING_KEY), MATCH_DEFAULT_ONLY))
                .thenReturn(info);

        assertFalse(mModel.hasDefaultApp(RUNNING_KEY));
    }

    @Test
    public void testSetDefaultApp_validComponentName() throws Exception {
        mModel.setDefaultApp(RUNNING_KEY, mExpectedComponent.flattenToString());

        verify(mContext).startService(mIntentCaptor.capture());

        Intent intent = mIntentCaptor.getValue();
        Parcelable observedComponent = intent.getParcelableExtra(EXTRA_COMPONENT_NAME);
        Parcelable observedImplicitIntent = intent.getParcelableExtra(EXTRA_ORIGINAL_INTENT);

        assertEquals(mExpectedComponent, observedComponent);
        assertEquals(mExpectedImplicitIntent, observedImplicitIntent);
    }

    @Test
    public void testSetDefaultApp_InvalidComponentName() throws Exception {
        // Make sure we don't crash or try to set a default app if the flattened component name is
        // invalid. A flattened component name is invalid if any of the following are true:
        // - it is null
        // - it doesn't contain a "/"
        // - it contains a "/" but it is at the end of the String.
        mModel.setDefaultApp(RUNNING_KEY, null /* componentName */);
        mModel.setDefaultApp(RUNNING_KEY, "" /* componentName */);
        mModel.setDefaultApp(RUNNING_KEY, "com.test" /* componentName */);
        mModel.setDefaultApp(RUNNING_KEY, "com.test/" /* componentName */);

        verify(mContext, never()).startService(any(Intent.class));
    }

    @Test
    public void testGetAppLabels_multipleApps() throws Exception {
        String[] expectedAppLabels = new String[] { mNoneLabel, mWhiteListedName };
        when(mPackageManager.queryIntentActivities(INTENTS.get(RUNNING_KEY), MATCH_DEFAULT_ONLY))
                .thenReturn(mResolveInfos);

        CharSequence[] observedAppLabels = mModel.getWhiteListedAppLabelsWithNoneFirst(RUNNING_KEY);

        assertArrayEquals(expectedAppLabels, observedAppLabels);
    }

    @Test
    public void testGetAppLabels_noApps() throws Exception {
        String[] expectedAppLabels = new String[] { mNoneLabel };
        when(mPackageManager.queryIntentActivities(INTENTS.get(RUNNING_KEY), MATCH_DEFAULT_ONLY))
                .thenReturn(Collections.emptyList());

        CharSequence[] observedAppLabels = mModel.getWhiteListedAppLabelsWithNoneFirst(RUNNING_KEY);

        assertArrayEquals(expectedAppLabels, observedAppLabels);
    }

    @Test
    public void testGetFlattenedComponentNamesWithNoneFirst_multipleApps() throws Exception {
        String[] expectedComponentNames = new String[] { mNoneLabel, mWhiteListedComponent };
        when(mPackageManager.queryIntentActivities(INTENTS.get(RUNNING_KEY), MATCH_DEFAULT_ONLY))
                .thenReturn(mResolveInfos);

        CharSequence[] observedComponentNames =
                mModel.getWhiteListedFlattenedComponentNamesWithNoneFirst(RUNNING_KEY);

        assertArrayEquals(expectedComponentNames, observedComponentNames);
    }

    @Test
    public void testGetFlattenedComponentNamesWithNoneFirst_noApps() throws Exception {
        String[] expectedComponentNames = new String[] {mNoneLabel};
        when(mPackageManager.queryIntentActivities(INTENTS.get(RUNNING_KEY), MATCH_DEFAULT_ONLY))
                .thenReturn(Collections.emptyList());

        CharSequence[] observedComponentNames =
                mModel.getWhiteListedFlattenedComponentNamesWithNoneFirst(RUNNING_KEY);

        assertArrayEquals(expectedComponentNames, observedComponentNames);
    }

    @Test
    public void testGetFlattenedComponentNames_multipleApps() throws Exception {
        String[] expectedComponentNames = new String[] { mWhiteListedComponent };
        when(mPackageManager.queryIntentActivities(INTENTS.get(RUNNING_KEY), MATCH_DEFAULT_ONLY))
                .thenReturn(mResolveInfos);

        CharSequence[] observedComponentNames =
                mModel.getWhiteListedFlattenedComponentNames(RUNNING_KEY);

        assertArrayEquals(expectedComponentNames, observedComponentNames);
    }

    @Test
    public void testGetFlattenedComponentNames_noApps() throws Exception {
        when(mPackageManager.queryIntentActivities(INTENTS.get(RUNNING_KEY), MATCH_DEFAULT_ONLY))
                .thenReturn(Collections.emptyList());

        CharSequence[] observedComponentNames =
                mModel.getWhiteListedFlattenedComponentNames(RUNNING_KEY);

        assertEquals(0, observedComponentNames.length);
    }

    @Test
    public void testGetAppLabelFromComponentName_validInput() throws Exception {
        String packageName = "com.test";
        String activityName = "com.test.SomeActivity";
        String flattenedPackage = packageName + "/" + activityName;
        String appLabel = "Acme Fitness";
        ResolveInfo mockResolveInfo =
                getResolveInfoMock(appLabel, makeActivityInfo(packageName, activityName));

        //noinspection WrongConstant
        when(mPackageManager.resolveActivity(mIntentCaptor.capture(), eq(MATCH_DEFAULT_ONLY)))
                .thenReturn(mockResolveInfo);

        String observedAppLabel = mModel.getAppLabelFromComponentName(flattenedPackage);
        ComponentName observedComponent = mIntentCaptor.getValue().getComponent();

        assertEquals(appLabel, observedAppLabel);
        assertEquals(flattenedPackage, observedComponent.flattenToString());
    }

    @Test
    public void testGetAppLabelFromComponentName_invalidInput() throws Exception {
        assertEquals(mNoneLabel, mModel.getAppLabelFromComponentName(""));
        assertEquals(mNoneLabel, mModel.getAppLabelFromComponentName("com.test/"));
        assertEquals(mNoneLabel,
                mModel.getAppLabelFromComponentName("com.test:com.test.Something"));

        //noinspection WrongConstant
        when(mPackageManager.resolveActivity(anyObject(), anyInt())).thenReturn(null);
        assertEquals(mNoneLabel, mModel.getAppLabelFromComponentName("com.test/com.test.Valid"));
    }

    @Test
    public void isComponentResolvable_found() throws Exception {
        ResolveInfo whiteListedResolveInfoMock = getResolveInfoMock(mWhiteListedName,
                makeActivityInfo(mWhiteListedPackage, mWhiteListedActivity));
        //noinspection WrongConstant - eq(MATCH_DEFAULT_ONLY) isn't recognized.
        when(mPackageManager.resolveActivity(mIntentCaptor.capture(), eq(MATCH_DEFAULT_ONLY)))
                .thenReturn(whiteListedResolveInfoMock);

        boolean observedValue = mModel.isComponentResolvable(mWhiteListedComponent);
        String observedComponent = mIntentCaptor.getValue().getComponent().flattenToString();

        assertTrue(observedValue);
        assertEquals(mWhiteListedComponent, observedComponent);
    }

    @Test
    public void isComponentResolvable_notWhiteListed() throws Exception {
        ResolveInfo unsupportedResolveInfoMock = getResolveInfoMock(mUnsupportedName,
                makeActivityInfo(mUnsupportedPackage, mUnsupportedActivity));
        //noinspection WrongConstant - eq(MATCH_DEFAULT_ONLY) isn't recognized.
        when(mPackageManager.resolveActivity(mIntentCaptor.capture(), eq(MATCH_DEFAULT_ONLY)))
                .thenReturn(unsupportedResolveInfoMock);

        boolean observedValue = mModel.isComponentResolvable(mUnsupportedComponent);
        String observedComponent = mIntentCaptor.getValue().getComponent().flattenToString();

        assertFalse(observedValue);
        assertEquals(mUnsupportedComponent, observedComponent);
    }

    @Test
    public void isComponentResolvable_notFound() throws Exception {
        //noinspection WrongConstant - eq(MATCH_DEFAULT_ONLY) isn't recognized.
        when(mPackageManager.resolveActivity(mIntentCaptor.capture(), eq(MATCH_DEFAULT_ONLY)))
                .thenReturn(null);

        assertFalse(mModel.isComponentResolvable(FIT_REALTIME_ACTIVITY));
    }

    @Test
    public void testGetPreferredAppLabelForExercise_validActivity() throws Exception {
        ResolveInfo whiteListedResolveInfoMock = getResolveInfoMock(mWhiteListedName,
                makeActivityInfo(mWhiteListedPackage, mWhiteListedActivity));
        when(mPackageManager.resolveActivity(INTENTS.get(RUNNING_KEY), MATCH_DEFAULT_ONLY))
                .thenReturn(whiteListedResolveInfoMock);

        String observedAppLabel = mModel.getPreferredAppLabelForExercise(RUNNING_KEY);

        assertEquals(mAppLabels[0], observedAppLabel);
    }

    @Test
    public void testGetPreferredAppLabelForExercise_notWhiteListed() throws Exception {
        ResolveInfo unsupportedResolveInfoMock = getResolveInfoMock(mUnsupportedName,
                makeActivityInfo(mUnsupportedPackage, mUnsupportedActivity));
        when(mPackageManager.resolveActivity(INTENTS.get(RUNNING_KEY), MATCH_DEFAULT_ONLY))
                .thenReturn(unsupportedResolveInfoMock);

        String observedAppLabel = mModel.getPreferredAppLabelForExercise(RUNNING_KEY);

        assertEquals("", observedAppLabel);
    }

    @Test
    public void testGetPreferredAppLabelForExercise_invalidActivity() throws Exception {
        ResolveInfo info = mResolveInfos.get(1);
        when(mPackageManager.resolveActivity(INTENTS.get(RUNNING_KEY), MATCH_DEFAULT_ONLY))
                .thenReturn(info);

        info.activityInfo.name = null;
        assertEquals("", mModel.getPreferredAppLabelForExercise(RUNNING_KEY));

        info.activityInfo.packageName = null;
        assertEquals("", mModel.getPreferredAppLabelForExercise(RUNNING_KEY));

        info.activityInfo = null;
        assertEquals("", mModel.getPreferredAppLabelForExercise(RUNNING_KEY));

        when(mPackageManager.resolveActivity(INTENTS.get(RUNNING_KEY), MATCH_DEFAULT_ONLY))
                .thenReturn(null);
        assertEquals("", mModel.getPreferredAppLabelForExercise(RUNNING_KEY));
    }

    @Test
    public void testGetPreferredAppLabelForExercise_resolverActivity() throws Exception {
        // Note, when an implicit intent doesn't have an associated activity, package manager will
        // return that it's associated with the system resolver activity.
        ComponentName resolver = new ComponentName(mContext, ResolverActivity.class);
        ActivityInfo activityInfo =
                makeActivityInfo(resolver.getPackageName(), resolver.getClassName());
        ResolveInfo info = getResolveInfoMock("System Resolver", activityInfo);

        when(mPackageManager.resolveActivity(INTENTS.get(RUNNING_KEY), MATCH_DEFAULT_ONLY))
                .thenReturn(info);
        assertEquals("", mModel.getPreferredAppLabelForExercise(RUNNING_KEY));
    }

    @Test
    public void testGetPreferredAppComponent() throws Exception {
        ResolveInfo resolveInfoMock = getResolveInfoMock(mWhiteListedName,
                makeActivityInfo(mWhiteListedPackage, mWhiteListedActivity));
        when(mPackageManager.resolveActivity(INTENTS.get(RUNNING_KEY), MATCH_DEFAULT_ONLY))
                .thenReturn(resolveInfoMock);

        String observedComponent = mModel.getPreferredAppComponent(RUNNING_KEY);

        assertEquals(mWhiteListedComponent, observedComponent);
    }

    @Test
    public void testGetPreferredAppComponent_notWhiteListed() throws Exception {
        ResolveInfo resolveInfoMock = getResolveInfoMock(mUnsupportedName,
                makeActivityInfo(mUnsupportedPackage, mUnsupportedActivity));
        when(mPackageManager.resolveActivity(INTENTS.get(RUNNING_KEY), MATCH_DEFAULT_ONLY))
                .thenReturn(resolveInfoMock);

        String observedComponent = mModel.getPreferredAppComponent(RUNNING_KEY);

        assertEquals("", observedComponent);
    }

    @Test
    public void testGetPreferredAppComponent_invalid() throws Exception {
        ResolveInfo info = mResolveInfos.get(1);
        when(mPackageManager.resolveActivity(INTENTS.get(RUNNING_KEY), MATCH_DEFAULT_ONLY))
                .thenReturn(info);

        info.activityInfo.name = null;
        assertEquals("", mModel.getPreferredAppComponent(RUNNING_KEY));

        info.activityInfo.packageName = null;
        assertEquals("", mModel.getPreferredAppComponent(RUNNING_KEY));

        info.activityInfo = null;
        assertEquals("", mModel.getPreferredAppComponent(RUNNING_KEY));

        when(mPackageManager.resolveActivity(INTENTS.get(RUNNING_KEY), MATCH_DEFAULT_ONLY))
                .thenReturn(null);
        assertEquals("", mModel.getPreferredAppComponent(RUNNING_KEY));
    }

    @Test
    public void testGetPreferredAppComponent_resolver() throws Exception {
        // Note, when an implicit intent doesn't have an associated activity, package manager will
        // return that it's associated with the system resolver activity.
        ComponentName resolver = new ComponentName(mContext, ResolverActivity.class);
        ActivityInfo activityInfo =
                makeActivityInfo(resolver.getPackageName(), resolver.getClassName());
        ResolveInfo info = getResolveInfoMock("System Resolver", activityInfo);

        when(mPackageManager.resolveActivity(INTENTS.get(RUNNING_KEY), MATCH_DEFAULT_ONLY))
                .thenReturn(info);
        assertEquals("", mModel.getPreferredAppComponent(RUNNING_KEY));
    }

    private ResolveInfo getResolveInfoMock(String label, ActivityInfo activityInfo) {
        ResolveInfo resolveInfo = mock(ResolveInfo.class);
        when(resolveInfo.loadLabel(mPackageManager)).thenReturn(label);
        resolveInfo.activityInfo = activityInfo;
        return resolveInfo;
    }

    private static ActivityInfo makeActivityInfo(String packageName, String activityName) {
        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = packageName;
        activityInfo.name = activityName;
        return activityInfo;
    }
}
