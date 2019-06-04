package com.google.android.clockwork.settings.personal.fitness;

import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.BIKING_KEY;
import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.FIT_REALTIME_ACTIVITY;
import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.INTENTS;
import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.WALKING_KEY;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.content.Intent;
import com.google.android.clockwork.settings.personal.fitness.models.ExerciseAssociationModel;
import com.google.android.clockwork.settings.personal.fitness.models.ExerciseLastStartedModel;
import com.google.android.clockwork.settings.personal.fitness.models.ExercisesEnabledModel;
import com.google.android.clockwork.settings.personal.fitness.models.ExercisesSupportedModel;
import com.google.android.clockwork.settings.personal.fitness.models.RunningAppsModel;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

/** Tests for {@link ExerciseDetectionRelayPresenter}. */
@RunWith(ClockworkRobolectricTestRunner.class)
@SuppressWarnings("WrongConstant") // anyString() is not an @ExerciseKey
public class ExerciseDetectionRelayPresenterTest {

    private static final String EXPECTED_COMPONENT = "com.google.android/.ExpectedActivity";

    private Intent mImplicitBikingIntent;
    private ExerciseDetectionRelayPresenter mPresenter;

    @Mock private ExerciseLastStartedModel mLastedStartedModel;
    @Mock private ExerciseAssociationModel mAssociationModel;
    @Mock private ExercisesEnabledModel mEnabledModel;
    @Mock private ExercisesSupportedModel mSupportedModel;
    @Mock private RunningAppsModel mRunningAppsModel;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        mImplicitBikingIntent = INTENTS.get(BIKING_KEY);
        mPresenter = new ExerciseDetectionRelayPresenter(
                mLastedStartedModel,
                mAssociationModel,
                mEnabledModel,
                mSupportedModel,
                mRunningAppsModel);
        when(mSupportedModel.hasHardwareSupport()).thenReturn(true);
        when(mSupportedModel.isSupported(BIKING_KEY)).thenReturn(true);
        when(mSupportedModel.isSupported(WALKING_KEY)).thenReturn(false);
        when(mRunningAppsModel.isOtherFitnessAppRunning(anyString(), any(CharSequence[].class)))
                .thenReturn(false);
    }

    @Test
    public void testLastIsInstalledAndDefault() throws Exception {
        when(mLastedStartedModel.getLastStarted(BIKING_KEY)).thenReturn(EXPECTED_COMPONENT);
        when(mAssociationModel.getPreferredAppComponent(BIKING_KEY)).thenReturn(EXPECTED_COMPONENT);
        when(mAssociationModel.isComponentResolvable(EXPECTED_COMPONENT)).thenReturn(true);
        when(mLastedStartedModel.start(BIKING_KEY, EXPECTED_COMPONENT, mImplicitBikingIntent))
                .thenReturn(true);

        mPresenter.onRelayRequest(BIKING_KEY, mImplicitBikingIntent);

        verify(mLastedStartedModel).start(BIKING_KEY, EXPECTED_COMPONENT, mImplicitBikingIntent);
    }

    @Test
    public void testFailsToStartActivity() throws Exception {
        when(mLastedStartedModel.getLastStarted(BIKING_KEY)).thenReturn(EXPECTED_COMPONENT);
        when(mAssociationModel.getPreferredAppComponent(BIKING_KEY)).thenReturn(EXPECTED_COMPONENT);
        when(mAssociationModel.isComponentResolvable(EXPECTED_COMPONENT)).thenReturn(true);
        when(mLastedStartedModel.start(BIKING_KEY, EXPECTED_COMPONENT, mImplicitBikingIntent))
                .thenReturn(false);

        mPresenter.onRelayRequest(BIKING_KEY, mImplicitBikingIntent);

        verify(mLastedStartedModel).start(BIKING_KEY, EXPECTED_COMPONENT, mImplicitBikingIntent);
        verify(mEnabledModel).setIsDetectionEnabled(BIKING_KEY, false);
    }

    @Test
    public void testLastIsInstalledButNoDefault() throws Exception {
        when(mLastedStartedModel.getLastStarted(BIKING_KEY)).thenReturn(EXPECTED_COMPONENT);
        when(mAssociationModel.getPreferredAppComponent(BIKING_KEY)).thenReturn("");
        when(mAssociationModel.isComponentResolvable(EXPECTED_COMPONENT)).thenReturn(true);
        when(mLastedStartedModel.start(BIKING_KEY, EXPECTED_COMPONENT, mImplicitBikingIntent))
                .thenReturn(true);

        mPresenter.onRelayRequest(BIKING_KEY, mImplicitBikingIntent);

        verify(mLastedStartedModel).start(BIKING_KEY, EXPECTED_COMPONENT, mImplicitBikingIntent);
        verify(mAssociationModel).setDefaultApp(BIKING_KEY, EXPECTED_COMPONENT);
    }

    @Test
    public void testLastIsInstalledButDiffersFromDefault() throws Exception {
        when(mLastedStartedModel.getLastStarted(BIKING_KEY)).thenReturn("old");
        when(mAssociationModel.getPreferredAppComponent(BIKING_KEY)).thenReturn("new");
        when(mAssociationModel.isComponentResolvable(EXPECTED_COMPONENT)).thenReturn(true);
        when(mLastedStartedModel.start(BIKING_KEY, "new", mImplicitBikingIntent))
                .thenReturn(true);

        mPresenter.onRelayRequest(BIKING_KEY, mImplicitBikingIntent);

        verify(mLastedStartedModel).start(BIKING_KEY, "new", mImplicitBikingIntent);
        verify(mAssociationModel).setDefaultApp(BIKING_KEY, "new");
    }

    @Test
    public void testLastNotInstalledButOnlyOneAppInstalled() throws Exception {
        when(mLastedStartedModel.getLastStarted(BIKING_KEY)).thenReturn("old");
        when(mAssociationModel.getPreferredAppComponent(BIKING_KEY)).thenReturn("");
        when(mAssociationModel.isComponentResolvable("old")).thenReturn(false);
        when(mAssociationModel.getWhiteListedFlattenedComponentNames(BIKING_KEY))
                .thenReturn(new CharSequence[] {
                        EXPECTED_COMPONENT
                });
        when(mLastedStartedModel.start(BIKING_KEY, EXPECTED_COMPONENT, mImplicitBikingIntent))
                .thenReturn(true);

        mPresenter.onRelayRequest(BIKING_KEY, mImplicitBikingIntent);

        verify(mLastedStartedModel).start(BIKING_KEY, EXPECTED_COMPONENT, mImplicitBikingIntent);
        verify(mAssociationModel).setDefaultApp(BIKING_KEY, EXPECTED_COMPONENT);
    }

    @Test
    public void testLastNotInstalledAndMultipleAppsAreButFitIsOneOfThem() throws Exception {
        when(mLastedStartedModel.getLastStarted(BIKING_KEY)).thenReturn("old");
        when(mAssociationModel.getPreferredAppComponent(BIKING_KEY)).thenReturn("");
        when(mAssociationModel.isComponentResolvable("old")).thenReturn(false);
        when(mAssociationModel.getWhiteListedFlattenedComponentNames(BIKING_KEY))
                .thenReturn(new CharSequence[] {
                        "notfit1",
                        FIT_REALTIME_ACTIVITY,
                        "notfit3",
                });
        when(mLastedStartedModel.start(BIKING_KEY, FIT_REALTIME_ACTIVITY, mImplicitBikingIntent))
                .thenReturn(true);

        mPresenter.onRelayRequest(BIKING_KEY, mImplicitBikingIntent);

        verify(mLastedStartedModel).start(BIKING_KEY, FIT_REALTIME_ACTIVITY, mImplicitBikingIntent);
        verify(mAssociationModel).setDefaultApp(BIKING_KEY, FIT_REALTIME_ACTIVITY);
    }

    @Test
    public void testLastNotInstalledAndMultipleAppsAreButFitIsNot() throws Exception {
        when(mLastedStartedModel.getLastStarted(BIKING_KEY)).thenReturn("old");
        when(mAssociationModel.getPreferredAppComponent(BIKING_KEY)).thenReturn("");
        when(mAssociationModel.isComponentResolvable("old")).thenReturn(false);
        when(mAssociationModel.getWhiteListedFlattenedComponentNames(BIKING_KEY))
                .thenReturn(new CharSequence[] {
                        "notfit1",
                        "notfit2",
                        "notfit3",
                });

        mPresenter.onRelayRequest(BIKING_KEY, mImplicitBikingIntent);

        verify(mEnabledModel).setIsDetectionEnabled(BIKING_KEY, false);
        verify(mLastedStartedModel, never()).start(anyString(), anyString(), any(Intent.class));
    }

    @Test
    public void testNoneInstalled() throws Exception {
        when(mLastedStartedModel.getLastStarted(BIKING_KEY)).thenReturn("old");
        when(mAssociationModel.getPreferredAppComponent(BIKING_KEY)).thenReturn("");
        when(mAssociationModel.isComponentResolvable("old")).thenReturn(false);
        when(mAssociationModel.getWhiteListedFlattenedComponentNames(BIKING_KEY))
                .thenReturn(new CharSequence[] { });

        mPresenter.onRelayRequest(BIKING_KEY, mImplicitBikingIntent);

        verify(mEnabledModel).setIsDetectionEnabled(BIKING_KEY, false);
        verify(mLastedStartedModel, never()).start(anyString(), anyString(), any(Intent.class));
    }

    @Test
    public void testDeviceNotSupported() throws Exception {
        when(mSupportedModel.hasHardwareSupport()).thenReturn(false);
        when(mLastedStartedModel.getLastStarted(BIKING_KEY)).thenReturn(EXPECTED_COMPONENT);
        when(mAssociationModel.getPreferredAppComponent(BIKING_KEY)).thenReturn(EXPECTED_COMPONENT);
        when(mAssociationModel.isComponentResolvable(EXPECTED_COMPONENT)).thenReturn(true);
        when(mLastedStartedModel.start(BIKING_KEY, EXPECTED_COMPONENT, mImplicitBikingIntent))
                .thenReturn(true);

        mPresenter.onRelayRequest(BIKING_KEY, mImplicitBikingIntent);

        verify(mEnabledModel).setIsDetectionEnabled(BIKING_KEY, false);
        verify(mLastedStartedModel, never()).start(anyString(), anyString(), any(Intent.class));
    }

    @Test
    public void testExerciseNotSupported() throws Exception {
        when(mLastedStartedModel.getLastStarted(WALKING_KEY)).thenReturn(EXPECTED_COMPONENT);
        when(mAssociationModel.getPreferredAppComponent(WALKING_KEY))
                .thenReturn(EXPECTED_COMPONENT);
        when(mAssociationModel.isComponentResolvable(EXPECTED_COMPONENT)).thenReturn(true);
        when(mLastedStartedModel.start(eq(WALKING_KEY), eq(EXPECTED_COMPONENT), any(Intent.class)))
                .thenReturn(true);

        mPresenter.onRelayRequest(WALKING_KEY, mImplicitBikingIntent);

        verify(mEnabledModel).setIsDetectionEnabled(WALKING_KEY, false);
        verify(mLastedStartedModel, never()).start(anyString(), anyString(), any(Intent.class));
    }

    @Test
    public void testOtherAppInForeground() throws Exception {
        when(mLastedStartedModel.getLastStarted(BIKING_KEY)).thenReturn(EXPECTED_COMPONENT);
        when(mAssociationModel.getPreferredAppComponent(BIKING_KEY)).thenReturn(EXPECTED_COMPONENT);
        when(mAssociationModel.isComponentResolvable(EXPECTED_COMPONENT)).thenReturn(true);
        when(mAssociationModel.getAllFlattenedComponentNames(anyString()))
                .thenReturn(new String[] { EXPECTED_COMPONENT});
        when(mLastedStartedModel.start(BIKING_KEY, EXPECTED_COMPONENT, mImplicitBikingIntent))
                .thenReturn(true);
        when(mRunningAppsModel.isOtherFitnessAppRunning(anyString(), any(CharSequence[].class)))
                .thenReturn(true);

        mPresenter.onRelayRequest(BIKING_KEY, mImplicitBikingIntent);

        verify(mEnabledModel, never()).setIsDetectionEnabled(anyString(), anyBoolean());
        verify(mLastedStartedModel, never()).start(anyString(), anyString(), any(Intent.class));
    }
}
