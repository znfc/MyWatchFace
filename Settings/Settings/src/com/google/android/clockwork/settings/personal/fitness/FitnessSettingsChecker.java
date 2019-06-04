package com.google.android.clockwork.settings.personal.fitness;

import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.EXERCISE_KEYS;
import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.FIT_REALTIME_ACTIVITY;

import android.content.Context;
import com.google.android.clockwork.settings.personal.fitness.models.ExerciseAssociationModel;
import com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.ExerciseKey;
import com.google.android.clockwork.settings.personal.fitness.models.ExercisesEnabledModel;
import com.google.android.clockwork.settings.personal.fitness.models.ExercisesSupportedModel;
import com.google.android.clockwork.settings.personal.fitness.models.PackageWhiteListModel;

/**
 * Checks fitness related Settings to ensure they are self consistent and that defaults are set.
 */
public class FitnessSettingsChecker {

    private final ExercisesEnabledModel mEnabledModel;
    private final ExercisesSupportedModel mSupportedModel;
    private final ExerciseAssociationModel mAssociationModel;

    public static FitnessSettingsChecker getInstance(Context context) {
        PackageWhiteListModel whiteListModel = new PackageWhiteListModel();
        ExercisesEnabledModel enabledModel =
                new ExercisesEnabledModel(context.getContentResolver());
        ExercisesSupportedModel supportedModel =
                new ExercisesSupportedModel(context.getResources());
        ExerciseAssociationModel associationModel =
                new ExerciseAssociationModel(context, whiteListModel, context.getPackageManager());

        return new FitnessSettingsChecker(enabledModel, supportedModel, associationModel);
    }

    FitnessSettingsChecker(ExercisesEnabledModel enabledModel,
            ExercisesSupportedModel supportedModel, ExerciseAssociationModel associationModel) {
        this.mEnabledModel = enabledModel;
        this.mSupportedModel = supportedModel;
        this.mAssociationModel = associationModel;
    }

    /**
     * Verifies that all settings are consistent. Unsupported activities will be unset (and
     * disabled), while supported activities that are unset will be set to their default value.
     */
    public FitnessSettingsChecker verifyConsistency() {
        if (!mSupportedModel.hasHardwareSupport()) {
            return this;
        }

        for (@ExerciseKey String exercise : EXERCISE_KEYS) {
            if (!mSupportedModel.isSupported(exercise)) {
                mEnabledModel.unset(exercise);
            } else if (!mEnabledModel.isSet(exercise)) {
                boolean isEnabledByDefault = mEnabledModel.isEnabledByDefault(exercise);
                mEnabledModel.setIsDetectionEnabled(exercise, isEnabledByDefault);
            }
        }

        return this;
    }

    /**
     * If detection is enabled but it isn't associated with anything, this associates it with Fit.
     */
    public FitnessSettingsChecker verifyDefaultAssociations() {
        if (!mSupportedModel.hasHardwareSupport()
                || !mAssociationModel.isComponentResolvable(FIT_REALTIME_ACTIVITY)) {
            return this;
        }

        EXERCISE_KEYS.stream()
                .filter(mEnabledModel::isDetectionEnabled)
                .filter(exercise -> !mAssociationModel.hasDefaultApp(exercise))
                .forEach(exercise ->
                        mAssociationModel.setDefaultApp(exercise, FIT_REALTIME_ACTIVITY));

        return this;
    }
}
