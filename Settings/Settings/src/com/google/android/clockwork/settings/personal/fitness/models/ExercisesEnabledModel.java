package com.google.android.clockwork.settings.personal.fitness.models;

import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.DISABLED;
import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.ENABLED;
import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.EXERCISE_KEYS;
import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.UNSET;

import android.content.ContentResolver;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import com.google.android.clockwork.host.GKeys;
import com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.EnabledStatus;
import com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.ExerciseKey;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Gets and sets whether Exercise Detection should be enabled for particular exercises.
 *
 * <p>For supported exercises, these may be modified by the user and may be in one of three states:
 * <ol>
 *     <li> {@code ENABLED}: Exercise Detection should be actively running for the exercise.
 *     <li> {@code DISABLED}: Exercise Detection should not be actively running for the exercise.
 *     <li> {@code UNSET}: Exercise Detection is either unsupported on the device or has not yet
 *          been set for the first time.
 * </ol>
 */
public class ExercisesEnabledModel {

    private final ContentResolver mContentResolver;
    private final Map<String, Integer> mDefaultEnabledStatus = new HashMap<>(EXERCISE_KEYS.size());
    private final String mSupportedExercises;

    public ExercisesEnabledModel(ContentResolver contentResolver) {
        this(contentResolver, GKeys.EXERCISE_DETECTION_SUPPORTED_EXERCISES.get());
    }

    @VisibleForTesting
    ExercisesEnabledModel(ContentResolver contentResolver, String supportedExercises) {
        mContentResolver = contentResolver;
        mSupportedExercises = supportedExercises;
        parseSupportedExercises(supportedExercises);
    }

    private void parseSupportedExercises(String supportedExercises) {
        Arrays.stream(supportedExercises.split(","))
                .map(exercise -> exercise.split("="))
                .forEach(keyValue -> mDefaultEnabledStatus.put(
                        keyValue[0],
                        keyValue.length > 1 ? Integer.parseInt(keyValue[1]) : ENABLED));
    }

    /** Returns true if {@code exercise} should be enabled by default. */
    public boolean isEnabledByDefault(@ExerciseKey String exercise) {
        return mDefaultEnabledStatus.containsKey(exercise)
                && DISABLED != mDefaultEnabledStatus.getOrDefault(exercise, UNSET);
    }

    /** Returns true if Exercise Detection is enabled for {@code exercise}. */
    public boolean isDetectionEnabled(@ExerciseKey String exercise) {
        return ENABLED == getSetting(exercise);
    }

    /** Sets whether Exercise Detection is enabled for {@code exercise}. */
    public void setIsDetectionEnabled(@ExerciseKey String exercise, boolean isEnabled) {
        setSetting(exercise, isEnabled ? ENABLED : DISABLED);
    }

    /**
     * Unsets Exercise Detection for the given {@code exercise}. Note that Fit Platform will
     * consider unset to be disabled.
     */
    public void unset(@ExerciseKey String exercise) {
        setSetting(exercise, UNSET);
    }

    /** Returns true if Exercise Detection is enabled for {@code exercise}. */
    public boolean isSet(@ExerciseKey String exercise) {
        return UNSET != getSetting(exercise);
    }

    @VisibleForTesting
    @EnabledStatus
    int getSetting(@ExerciseKey String exercise) {
        //noinspection WrongConstant - linter isn't aware the setting contains an @EnabledStatus.
        return Settings.Global.getInt(mContentResolver, exercise, UNSET);
    }

    @VisibleForTesting
    void setSetting(@ExerciseKey String exercise, @EnabledStatus int setting) {
        Settings.Global.putInt(mContentResolver, exercise, setting);
    }

}
