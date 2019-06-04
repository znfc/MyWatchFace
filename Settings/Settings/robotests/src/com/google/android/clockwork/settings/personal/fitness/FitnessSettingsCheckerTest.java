package com.google.android.clockwork.settings.personal.fitness;

import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.BIKING_KEY;
import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.FIT_REALTIME_ACTIVITY;
import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.RUNNING_KEY;
import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.WALKING_KEY;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.clockwork.settings.personal.fitness.models.ExerciseAssociationModel;
import com.google.android.clockwork.settings.personal.fitness.models.ExercisesEnabledModel;
import com.google.android.clockwork.settings.personal.fitness.models.ExercisesSupportedModel;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(ClockworkRobolectricTestRunner.class)
public class FitnessSettingsCheckerTest {

    @Mock ExercisesEnabledModel mEnabledModel;
    @Mock ExercisesSupportedModel mSupportedModel;
    @Mock ExerciseAssociationModel mAssociationModel;
    private FitnessSettingsChecker mChecker;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        when(mSupportedModel.hasHardwareSupport()).thenReturn(true);
        when(mSupportedModel.isSupported(WALKING_KEY)).thenReturn(false);
        when(mSupportedModel.isSupported(RUNNING_KEY)).thenReturn(true);
        when(mSupportedModel.isSupported(BIKING_KEY)).thenReturn(true);
        when(mAssociationModel.isComponentResolvable(FIT_REALTIME_ACTIVITY)).thenReturn(true);

        mChecker = new FitnessSettingsChecker(mEnabledModel, mSupportedModel, mAssociationModel);
    }

    @Test
    public void testVerifyConsistency_NoOpOnUnsupportedDevice() throws Exception {
        when(mSupportedModel.hasHardwareSupport()).thenReturn(false);

        mChecker.verifyConsistency();

        verifyZeroInteractions(mEnabledModel);
    }

    @Test
    public void testVerifyConsistency_unsupportedAreUnset() throws Exception {
        mChecker.verifyConsistency();

        verify(mEnabledModel).unset(WALKING_KEY);
        verify(mEnabledModel, never()).unset(RUNNING_KEY);
        verify(mEnabledModel, never()).unset(BIKING_KEY);
    }

    @Test
    @SuppressWarnings("WrongConstant") // eq() isn't an ExerciseKey.
    public void testVerifyConsistency_unsupportedNotEnabled() throws Exception {
        when(mSupportedModel.isSupported(WALKING_KEY)).thenReturn(false);
        when(mEnabledModel.isSet(WALKING_KEY)).thenReturn(false);
        when(mEnabledModel.isEnabledByDefault(WALKING_KEY)).thenReturn(true);

        mChecker.verifyConsistency();

        verify(mEnabledModel, never()).setIsDetectionEnabled(eq(WALKING_KEY), anyBoolean());
    }

    @Test
    public void testVerifyConsistency_supportedUnsetEnabledByDefault() throws Exception {
        when(mSupportedModel.isSupported(WALKING_KEY)).thenReturn(true);
        when(mEnabledModel.isSet(WALKING_KEY)).thenReturn(false);
        when(mEnabledModel.isEnabledByDefault(WALKING_KEY)).thenReturn(true);

        mChecker.verifyConsistency();

        verify(mEnabledModel).setIsDetectionEnabled(WALKING_KEY, true);
    }

    @Test
    public void testVerifyConsistency_supportedUnsetDisabledByDefault() throws Exception {
        when(mSupportedModel.isSupported(WALKING_KEY)).thenReturn(true);
        when(mEnabledModel.isSet(WALKING_KEY)).thenReturn(false);
        when(mEnabledModel.isEnabledByDefault(WALKING_KEY)).thenReturn(false);

        mChecker.verifyConsistency();

        verify(mEnabledModel).setIsDetectionEnabled(WALKING_KEY, false);
    }

    @Test
    public void testVerifyDefaultAssociations_noDefaultApp() throws Exception {
        when(mEnabledModel.isDetectionEnabled(RUNNING_KEY)).thenReturn(true);
        when(mAssociationModel.hasDefaultApp(RUNNING_KEY)).thenReturn(false);

        mChecker.verifyDefaultAssociations();

        verify(mAssociationModel).setDefaultApp(RUNNING_KEY, FIT_REALTIME_ACTIVITY);
    }

    @Test
    public void testVerifyDefaultAssociations_noOpOnUnsupportedDevice() throws Exception {
        when(mSupportedModel.hasHardwareSupport()).thenReturn(false);
        when(mEnabledModel.isDetectionEnabled(RUNNING_KEY)).thenReturn(true);
        when(mAssociationModel.hasDefaultApp(RUNNING_KEY)).thenReturn(false);

        mChecker.verifyDefaultAssociations();

        verifyZeroInteractions(mAssociationModel);
    }

    @Test
    @SuppressWarnings("WrongConstant") // eq() isn't an ExerciseKey.
    public void testVerifyDefaultAssociations_hasDefaultApp() throws Exception {
        when(mEnabledModel.isDetectionEnabled(RUNNING_KEY)).thenReturn(true);
        when(mAssociationModel.hasDefaultApp(RUNNING_KEY)).thenReturn(true);

        mChecker.verifyDefaultAssociations();

        verify(mAssociationModel, never()).setDefaultApp(eq(RUNNING_KEY), anyString());
    }

    @Test
    @SuppressWarnings("WrongConstant") // eq() isn't an ExerciseKey.
    public void testVerifyDefaultAssociations_detectionDisabled() throws Exception {
        when(mEnabledModel.isDetectionEnabled(RUNNING_KEY)).thenReturn(false);
        when(mAssociationModel.hasDefaultApp(RUNNING_KEY)).thenReturn(true);

        mChecker.verifyDefaultAssociations();

        verify(mAssociationModel, never()).setDefaultApp(eq(RUNNING_KEY), anyString());
    }

    @Test
    @SuppressWarnings("WrongConstant") // anyString() isn't an ExerciseKey.
    public void testVerifyDefaultAssociations_noOpWhenFitNotAvailable() throws Exception {
        when(mAssociationModel.isComponentResolvable(FIT_REALTIME_ACTIVITY)).thenReturn(false);
        when(mEnabledModel.isDetectionEnabled(RUNNING_KEY)).thenReturn(true);
        when(mAssociationModel.hasDefaultApp(RUNNING_KEY)).thenReturn(false);

        mChecker.verifyDefaultAssociations();

        verify(mAssociationModel, never()).setDefaultApp(anyString(), anyString());
    }
}