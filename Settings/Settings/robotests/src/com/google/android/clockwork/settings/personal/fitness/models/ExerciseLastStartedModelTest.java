package com.google.android.clockwork.settings.personal.fitness.models;

import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.BIKING_KEY;
import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.EXERCISE_DETECTION_URI;
import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.INTENTS;
import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.RUNNING_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import com.google.android.clockwork.settings.FakeSettingsContentResolver;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.shadows.ShadowApplication;

/** Tests for {@link ExerciseLastStartedModel}. */
@RunWith(ClockworkRobolectricTestRunner.class)
public class ExerciseLastStartedModelTest {

    private static final String EXPECTED_COMPONENT = "com.google.android/.ExpectedActivity";

    private Intent mImplicitBikingIntent;
    private Intent mExplicitBikingIntent;
    private FakeSettingsContentResolver mSettings;
    private ExerciseLastStartedModel mModel;

    @Before
    public void setUp() throws Exception {
        Context context = ShadowApplication.getInstance().getApplicationContext();
        mSettings = new FakeSettingsContentResolver();

        mImplicitBikingIntent = INTENTS.get(BIKING_KEY);
        mExplicitBikingIntent = INTENTS.get(BIKING_KEY)
                .setComponent(ComponentName.unflattenFromString(EXPECTED_COMPONENT));

        mModel = new ExerciseLastStartedModel(mSettings, context);
    }

    @Test
    public void returnsEmptyStringWhenNothingStartedForExerciseType() {
        mSettings.putStringValueForKey(EXERCISE_DETECTION_URI, RUNNING_KEY, EXPECTED_COMPONENT);

        String lastedStarted = mModel.getLastStarted(BIKING_KEY);

        assertEquals("", lastedStarted);
    }

    @Test
    public void getLastStartedReturnsComponentWhenItExistsForExerciseType() {
        mSettings.putStringValueForKey(EXERCISE_DETECTION_URI, BIKING_KEY, EXPECTED_COMPONENT);

        String lastedStarted = mModel.getLastStarted(BIKING_KEY);

        assertEquals(EXPECTED_COMPONENT, lastedStarted);
    }

    @Test
    public void setLastStartedNormalInput() {
        mModel.setLastStarted(BIKING_KEY, EXPECTED_COMPONENT);

        String observedComponent =
                mSettings.getStringValueForKey(EXERCISE_DETECTION_URI, BIKING_KEY, null);

        assertEquals(EXPECTED_COMPONENT, observedComponent);
    }

    @Test
    public void setLastStartedNullInputRemovesPref() {
        mSettings.putStringValueForKey(EXERCISE_DETECTION_URI, BIKING_KEY, EXPECTED_COMPONENT);

        mModel.setLastStarted(BIKING_KEY, null);

        String observedComponent =
                mSettings.getStringValueForKey(EXERCISE_DETECTION_URI, BIKING_KEY, "not_found");
        assertEquals("not_found", observedComponent);
    }

    @Test
    public void setLastStartedAndGetLastStartedEndToEnd() {
        mModel.setLastStarted(BIKING_KEY, EXPECTED_COMPONENT);
        String observedComponent = mModel.getLastStarted(BIKING_KEY);

        assertEquals(EXPECTED_COMPONENT, observedComponent);
    }

    @Test
    public void startFiresExplicitIntentForActivity() {
        assertTrue(mModel.start(BIKING_KEY, EXPECTED_COMPONENT, mImplicitBikingIntent));

        Intent observedIntent = ShadowApplication.getInstance().getNextStartedActivity();

        assertEquals(EXPECTED_COMPONENT, observedIntent.getComponent().flattenToShortString());
        assertEquals(mImplicitBikingIntent.getAction(), observedIntent.getAction());
        assertEquals(mImplicitBikingIntent.getType(), observedIntent.getType());
    }

    @Test
    public void startSetsLastStarted() {
        assertTrue(mModel.start(BIKING_KEY, EXPECTED_COMPONENT, mImplicitBikingIntent));

        String observedComponent =
                mSettings.getStringValueForKey(EXERCISE_DETECTION_URI, BIKING_KEY, null);
        assertEquals(EXPECTED_COMPONENT, observedComponent);
    }

    @Test
    public void startDoesNotSetLastStartedWhenComponentNameInvalid() {
        assertFalse(mModel.start(BIKING_KEY, "", mImplicitBikingIntent));
        assertFalse(mModel.start(BIKING_KEY, "invalid", mImplicitBikingIntent));

        String observedComponent =
                mSettings.getStringValueForKey(EXERCISE_DETECTION_URI, BIKING_KEY, "not_found");
        assertEquals("not_found", observedComponent);
    }

    @Test
    public void startDoesNotSetLastStartedWhenStartActivityThrowsException() {
        Context mockContext = mock(Context.class);
        doThrow(new ActivityNotFoundException()).when(mockContext).startActivity(any(Intent.class));
        mModel = new ExerciseLastStartedModel(mSettings, mockContext);

        assertFalse(mModel.start(BIKING_KEY, EXPECTED_COMPONENT, mImplicitBikingIntent));

        String observedComponent =
                mSettings.getStringValueForKey(EXERCISE_DETECTION_URI, BIKING_KEY, "not_found");
        assertEquals("not_found", observedComponent);
    }
}