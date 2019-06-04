package com.google.android.clockwork.settings.personal.fitness.models;

import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.EXERCISE_KEYS;

import android.content.res.Resources;
import android.support.annotation.VisibleForTesting;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.host.GKeys;
import com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.ExerciseKey;

/**
 * Gets whether the device supports exercise detection, and if so, which exercises are supported.
 */
public class ExercisesSupportedModel {

    private final String mSupportedExercises;
    private final Resources mResources;

    public ExercisesSupportedModel(Resources mResources) {
        this.mResources = mResources;
        mSupportedExercises = GKeys.EXERCISE_DETECTION_SUPPORTED_EXERCISES.get();
    }

    @VisibleForTesting
    ExercisesSupportedModel(Resources mResources, String supportedExercises) {
        this.mResources = mResources;
        mSupportedExercises = supportedExercises;
    }

    /**
     * Returns {@code true} if this device has hardware support for Exercise Detection.
     */
    public boolean hasHardwareSupport() {
        boolean deviceHasOverlayIndicatingHardwareSupport =
                mResources.getBoolean(R.bool.config_exercise_detection_supported);

        return deviceHasOverlayIndicatingHardwareSupport;
    }

    /**
     * Returns {@code true} if this device has any supported exercises.
     */
    public boolean hasSupportedExercises() {
        long numberOfSupportedExercises = EXERCISE_KEYS.stream()
                .filter(this::isSupported)
                .count();

        return numberOfSupportedExercises > 0;
    }

    /** Returns {@code true} if this device supports exercise detection for {@code exercise}. */
    public boolean isSupported(@ExerciseKey String exercise) {
        return mSupportedExercises.contains(exercise);
    }
}