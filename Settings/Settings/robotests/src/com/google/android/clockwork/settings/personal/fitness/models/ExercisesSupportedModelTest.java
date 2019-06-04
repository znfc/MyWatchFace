package com.google.android.clockwork.settings.personal.fitness.models;

import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.EXERCISE_KEYS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.content.res.Resources;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(ClockworkRobolectricTestRunner.class)
public class ExercisesSupportedModelTest {

    private final String NONE_SUPPORTED = "";
    private final String ALL_SUPPORTED = EXERCISE_KEYS.stream()
            .map(key -> key + "=on")
            .collect(Collectors.joining(","));

    @Mock Resources mResources;
    private ExercisesSupportedModel mModel;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void testHasHardwareSupport() throws Exception {
        mModel = new ExercisesSupportedModel(mResources, ALL_SUPPORTED);

        when(mResources.getBoolean(R.bool.config_exercise_detection_supported)).thenReturn(true);
        assertTrue(mModel.hasHardwareSupport());

        when(mResources.getBoolean(R.bool.config_exercise_detection_supported)).thenReturn(false);
        assertFalse(mModel.hasHardwareSupport());
    }

    @Test
    public void testHasSupportedExercises() {
        mModel = new ExercisesSupportedModel(mResources, ALL_SUPPORTED);
        when(mResources.getBoolean(R.bool.config_exercise_detection_supported)).thenReturn(true);
        assertTrue(mModel.hasSupportedExercises());

        mModel = new ExercisesSupportedModel(mResources, NONE_SUPPORTED);
        when(mResources.getBoolean(R.bool.config_exercise_detection_supported)).thenReturn(true);
        assertFalse(mModel.hasSupportedExercises());
    }

    @Test
    public void testIsSupported() throws Exception {
        mModel = new ExercisesSupportedModel(mResources, ALL_SUPPORTED);
        EXERCISE_KEYS.stream().forEach(key -> assertTrue(key, mModel.isSupported(key)));

        mModel = new ExercisesSupportedModel(mResources, NONE_SUPPORTED);
        EXERCISE_KEYS.stream().forEach(key -> assertFalse(key, mModel.isSupported(key)));
    }
}