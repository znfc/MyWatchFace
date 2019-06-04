package com.google.android.clockwork.settings.personal.fitness;

import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.BIKING_KEY;
import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.RUNNING_KEY;
import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.WALKING_KEY;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.android.clockwork.settings.personal.fitness.ExerciseDetectionSettingsPresenter.ExerciseDetectionSettingsView;
import com.google.android.clockwork.settings.personal.fitness.ExerciseDetectionSettingsPresenter.SettingsEntry;
import com.google.android.clockwork.settings.personal.fitness.models.ExerciseAssociationModel;
import com.google.android.clockwork.settings.personal.fitness.models.ExercisesEnabledModel;
import com.google.android.clockwork.settings.personal.fitness.models.ExercisesSupportedModel;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

@RunWith(ClockworkRobolectricTestRunner.class)
public class ExerciseDetectionSettingsPresenterTest {

    private static final String NONE = "none";
    private static final String COMPONENT = "component";
    private static final String NAME = "name";
    private static final CharSequence[] APP_LABELS_WITH_NONE_FIRST = new String[] {
            NONE,
            NAME,
    };

    @Mock ExerciseDetectionSettingsView mView;
    @Mock ExercisesEnabledModel mEnabledModel;
    @Mock ExercisesSupportedModel mSupportedModel;
    @Mock ExerciseAssociationModel mAssociationModel;
    @Captor ArgumentCaptor<SettingsEntry> mSettingsEntryCaptor;

    private ExerciseDetectionSettingsPresenter mPresenter;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        mPresenter = new ExerciseDetectionSettingsPresenter(
                mView, mEnabledModel, mSupportedModel, mAssociationModel);

        when(mSupportedModel.isSupported(WALKING_KEY)).thenReturn(false);
        when(mSupportedModel.isSupported(RUNNING_KEY)).thenReturn(true);
        when(mSupportedModel.isSupported(BIKING_KEY)).thenReturn(true);
        when(mAssociationModel.getNoneLabel()).thenReturn(NONE);
        when(mAssociationModel.getPreferredAppComponent(RUNNING_KEY)).thenReturn(COMPONENT);
        when(mAssociationModel.getAppLabelFromComponentName(COMPONENT)).thenReturn(NAME);
    }

    @Test
    public void testInit() throws Exception {
        mPresenter.init();

        verify(mView, only()).removePreference(WALKING_KEY);
    }

    @Test
    public void testUpdatePreferences() throws Exception {
        List<String> supportedExercises = ImmutableList.of(RUNNING_KEY, BIKING_KEY);
        List<String> unsupportedExercises = ImmutableList.of(WALKING_KEY);

        mPresenter.updatePreferences();

        verify(mView, times(2)).updatePreference(mSettingsEntryCaptor.capture());
        List<SettingsEntry> capturedSettingsEntries = mSettingsEntryCaptor.getAllValues();

        // Only contains supported.
        assertTrue(capturedSettingsEntries.stream()
                .map(settingsEntry -> settingsEntry.exercise)
                .allMatch(supportedExercises::contains));

        // Doesn't contain any unsupported.
        assertTrue(capturedSettingsEntries.stream()
                .map(settingsEntry -> settingsEntry.exercise)
                .noneMatch(unsupportedExercises::contains));
    }

    @Test
    public void testOnSelectionChanged_selectedAnApp() throws Exception {
        mPresenter.onSelectionChanged(RUNNING_KEY, COMPONENT);

        verify(mAssociationModel).setDefaultApp(RUNNING_KEY, COMPONENT);
        verify(mEnabledModel).setIsDetectionEnabled(RUNNING_KEY, true);
        verify(mView).setPreferenceSummary(RUNNING_KEY, NAME);
    }

    @Test
    public void testOnSelectionChanged_selectedNone() throws Exception {
        mPresenter.onSelectionChanged(RUNNING_KEY, NONE);

        verify(mEnabledModel, only()).setIsDetectionEnabled(RUNNING_KEY, false);
        verify(mView).setPreferenceSummary(RUNNING_KEY, NONE);
    }

    @Test
    public void testGetSelectedAppName_enabledExercise() throws Exception {
        when(mEnabledModel.isDetectionEnabled(RUNNING_KEY)).thenReturn(true);
        when(mAssociationModel.getPreferredAppLabelForExercise(RUNNING_KEY))
                .thenReturn(NAME);

        String observedLabel = mPresenter.getGetSelectedAppName(RUNNING_KEY);

        assertEquals(NAME, observedLabel);
    }

    @Test
    public void testGetSelectedAppName_disabledExercise() throws Exception {
        when(mEnabledModel.isDetectionEnabled(RUNNING_KEY)).thenReturn(false);

        String observedLabel = mPresenter.getGetSelectedAppName(RUNNING_KEY);

        verify(mAssociationModel, never()).getPreferredAppLabelForExercise(RUNNING_KEY);
        assertEquals(NONE, observedLabel);
    }

    @Test
    public void testGetAppNames() throws Exception {
        when(mAssociationModel.getWhiteListedAppLabelsWithNoneFirst(RUNNING_KEY))
                .thenReturn(APP_LABELS_WITH_NONE_FIRST);

        CharSequence[] observedAppLabels = mPresenter.getAppNames(RUNNING_KEY);

        assertArrayEquals(APP_LABELS_WITH_NONE_FIRST, observedAppLabels);
    }

    @Test
    public void testGetSelectedComponent_enabled() throws Exception {
        when(mEnabledModel.isDetectionEnabled(RUNNING_KEY)).thenReturn(true);
        when(mAssociationModel.getPreferredAppComponent(RUNNING_KEY)).thenReturn(COMPONENT);

        String observedComponent = mPresenter.getSelectedComponent(RUNNING_KEY);

        assertEquals(COMPONENT, observedComponent);
    }

    @Test
    public void testGetSelectedComponent_disabled() throws Exception {
        when(mEnabledModel.isDetectionEnabled(RUNNING_KEY)).thenReturn(false);
        when(mAssociationModel.getPreferredAppComponent(RUNNING_KEY)).thenReturn(COMPONENT);

        String observedComponent = mPresenter.getSelectedComponent(RUNNING_KEY);

        //noinspection WrongConstant - anyString() isn't an @ExerciseKey.
        verify(mAssociationModel, never()).getPreferredAppComponent(anyString());
        assertEquals(NONE, observedComponent);
    }
}