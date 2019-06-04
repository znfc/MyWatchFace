package com.google.android.clockwork.settings.personal.fitness.models;

import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.DISABLED;
import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.ENABLED;
import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.EXERCISE_KEYS;
import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.RUNNING_KEY;
import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.UNSET;
import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.WALKING_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

import android.content.ContentResolver;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import java.util.Locale;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(ClockworkRobolectricTestRunner.class)
public class ExercisesEnabledModelTest {

    final String NO_SUPPORTED_EXERCISES = "";

    final String JUST_RUNNING_SUPPORTED = RUNNING_KEY + "=" + ENABLED;

    final String TRAILING_COMMA = JUST_RUNNING_SUPPORTED + ",";

    final String NO_DEFAULT = EXERCISE_KEYS.stream()
            .collect(Collectors.joining(","));

    final String ENABLED_BY_DEFAULT = EXERCISE_KEYS.stream()
            .map(key -> String.format(Locale.US, "%s=%d", key, ENABLED))
            .collect(Collectors.joining(","));

    final String DISABLED_BY_DEFAULT = EXERCISE_KEYS.stream()
            .map(key -> String.format(Locale.US, "%s=%d", key, DISABLED))
            .collect(Collectors.joining(","));

    final String UNSET_BY_DEFAULT = EXERCISE_KEYS.stream()
            .map(key -> String.format(Locale.US, "%s=%d", key, UNSET))
            .collect(Collectors.joining(","));

    @Mock ContentResolver mContentResolver;
    ExercisesEnabledModel mModel;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        mModel = new ExercisesEnabledModel(mContentResolver, ENABLED_BY_DEFAULT);
    }

    @Test
    public void testGetDefault_enabledByDefault() throws Exception {
        mModel = new ExercisesEnabledModel(mContentResolver, ENABLED_BY_DEFAULT);
        assertTrue(mModel.isEnabledByDefault(RUNNING_KEY));
    }

    @Test
    public void testGetDefault_absenceOfDefaultShouldBeEnabledByDefault() throws Exception {
        // This might happen to dogfooders, since previously defaults were not embedded in the gkeys
        // String. Make sure that the previous behavior still works (enable by default). This will
        // also catch the situation where the gkey is not correctly formed.
        mModel = new ExercisesEnabledModel(mContentResolver, NO_DEFAULT);
        assertTrue(mModel.isEnabledByDefault(RUNNING_KEY));
    }

    @Test
    public void testGetDefault_unsetByDefaultShouldBeConsideredEnabledByDefault() throws Exception {
        // To avoid confusion, things shouldn't be unset by default. However, if they are, then
        // it should be considered enabled, since the FitnessSettingsChecker will set it to enabled.
        mModel = new ExercisesEnabledModel(mContentResolver, UNSET_BY_DEFAULT);
        assertTrue(mModel.isEnabledByDefault(RUNNING_KEY));
    }

    @Test
    public void testGetDefault_disabledByDefault() throws Exception {
        mModel = new ExercisesEnabledModel(mContentResolver, DISABLED_BY_DEFAULT);
        assertFalse(mModel.isEnabledByDefault(RUNNING_KEY));
    }

    @Test
    public void testGetDefault_noSupportedExercises() throws Exception {
        mModel = new ExercisesEnabledModel(mContentResolver, NO_SUPPORTED_EXERCISES);
        assertFalse(mModel.isEnabledByDefault(RUNNING_KEY));
    }

    @Test
    public void testGetDefault_trailingCommaDoesNotThrowException() throws Exception {
        mModel = new ExercisesEnabledModel(mContentResolver, TRAILING_COMMA);
    }

    @Test
    public void testGetDefault_unsupportedNotConsideredEnabled() throws Exception {
        mModel = new ExercisesEnabledModel(mContentResolver, JUST_RUNNING_SUPPORTED);
        assertFalse(mModel.isEnabledByDefault(WALKING_KEY));
        assertTrue(mModel.isEnabledByDefault(RUNNING_KEY));
    }

    @Test
    public void testIsDetectionEnabled() throws Exception {
        mModel.setSetting(RUNNING_KEY, UNSET);
        assertFalse(mModel.isDetectionEnabled(RUNNING_KEY));

        mModel.setSetting(RUNNING_KEY, DISABLED);
        assertFalse(mModel.isDetectionEnabled(RUNNING_KEY));

        mModel.setSetting(RUNNING_KEY, ENABLED);
        assertTrue(mModel.isDetectionEnabled(RUNNING_KEY));
    }

    @Test
    public void testSetIsDetectionEnabled() throws Exception {
        mModel.setSetting(RUNNING_KEY, UNSET);

        mModel.setIsDetectionEnabled(RUNNING_KEY, true);
        assertEquals(ENABLED, mModel.getSetting(RUNNING_KEY));

        mModel.setIsDetectionEnabled(RUNNING_KEY, false);
        assertEquals(DISABLED, mModel.getSetting(RUNNING_KEY));
    }

    @Test
    public void testUnset() throws Exception {
        mModel.setSetting(RUNNING_KEY, ENABLED);

        mModel.unset(RUNNING_KEY);
        assertEquals(UNSET, mModel.getSetting(RUNNING_KEY));
    }

    @Test
    public void testIsSet() throws Exception {
        mModel.setSetting(RUNNING_KEY, ENABLED);
        assertTrue(mModel.isSet(RUNNING_KEY));

        mModel.setSetting(RUNNING_KEY, DISABLED);
        assertTrue(mModel.isSet(RUNNING_KEY));

        mModel.setSetting(RUNNING_KEY, UNSET);
        assertFalse(mModel.isSet(RUNNING_KEY));
    }
}