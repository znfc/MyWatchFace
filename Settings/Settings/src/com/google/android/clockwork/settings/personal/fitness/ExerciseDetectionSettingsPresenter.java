package com.google.android.clockwork.settings.personal.fitness;

import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.EXERCISE_KEYS;

import android.support.annotation.VisibleForTesting;
import com.google.android.clockwork.settings.personal.fitness.models.ExerciseAssociationModel;
import com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.ExerciseKey;
import com.google.android.clockwork.settings.personal.fitness.models.ExercisesEnabledModel;
import com.google.android.clockwork.settings.personal.fitness.models.ExercisesSupportedModel;

/**
 * Presenter for Exercise Detection.
 */
class ExerciseDetectionSettingsPresenter {

    /**
     * Interface to be implemented by the View.
     */
    interface ExerciseDetectionSettingsView {
        void removePreference(@ExerciseKey String key);
        void setPreferenceSummary(@ExerciseKey String key, String summary);
        void updatePreference(SettingsEntry settingsEntry);
    }

    static class SettingsEntry {
        @ExerciseKey public final String exercise;
        final String selectedAppName;
        final String selectedComponent;
        final CharSequence[] appNames;
        final CharSequence[] components;

        @VisibleForTesting
        SettingsEntry(String exercise, String selectedAppName, String selectedComponent,
                CharSequence[] appNames, CharSequence[] components) {
            this.exercise = exercise;
            this.selectedAppName = selectedAppName;
            this.selectedComponent = selectedComponent;
            this.appNames = appNames;
            this.components = components;
        }
    }

    private final ExerciseDetectionSettingsView mView;
    private final ExercisesEnabledModel mEnabledModel;
    private final ExercisesSupportedModel mSupportedModel;
    private final ExerciseAssociationModel mAssociationModel;

    ExerciseDetectionSettingsPresenter(ExerciseDetectionSettingsView view,
            ExercisesEnabledModel enabledModel,
            ExercisesSupportedModel supportedModel,
            ExerciseAssociationModel associationModel) {
        mView = view;
        mEnabledModel = enabledModel;
        mSupportedModel = supportedModel;
        mAssociationModel = associationModel;
    }

    /**
     * Removes unsupported preferences.
     */
    void init() {
        EXERCISE_KEYS.stream()
                .filter(key -> !mSupportedModel.isSupported(key))
                .forEach(mView::removePreference);
    }

    /**
     * Updates all supported preferences.
     */
    void updatePreferences() {
        EXERCISE_KEYS.stream()
                .filter(mSupportedModel::isSupported)
                .map(this::constructSettingsEntry)
                .forEach(mView::updatePreference);
    }

    /**
     * Handles a user requested change for a particular exercise, setting whether it is enabled and
     * updating its association as necessary.
     */
    boolean onSelectionChanged(String exercise, String component) {
        String noneLabel = mAssociationModel.getNoneLabel();
        if (noneLabel.equals(component)) {
            mView.setPreferenceSummary(exercise, noneLabel);
            mEnabledModel.setIsDetectionEnabled(exercise, false);
        } else {
            mAssociationModel.setDefaultApp(exercise, component);
            mView.setPreferenceSummary(exercise,
                    mAssociationModel.getAppLabelFromComponentName(component));
            mEnabledModel.setIsDetectionEnabled(exercise, true);
        }

        return true;
    }

    private SettingsEntry constructSettingsEntry(@ExerciseKey String exercise) {
        return new SettingsEntry(exercise,
                getGetSelectedAppName(exercise),
                getSelectedComponent(exercise),
                getAppNames(exercise),
                getComponents(exercise));
    }

    /**
     * Returns the label of the preferred app for a particular exercise or a translated 'none' if it
     * doesn't have a preferred app.
     */
    @VisibleForTesting
    String getGetSelectedAppName(String exercise) {
        return mEnabledModel.isDetectionEnabled(exercise)
                ? mAssociationModel.getPreferredAppLabelForExercise(exercise)
                : mAssociationModel.getNoneLabel();
    }

    /**
     * Returns all entries (app labels) for a given exercise type. These are guaranteed to be in the
     * same order as those returned by {@link #getComponents}.
     */
    @VisibleForTesting
    CharSequence[] getAppNames(@ExerciseKey String exercise) {
        return mAssociationModel.getWhiteListedAppLabelsWithNoneFirst(exercise);
    }

    /**
     * Returns all entry values (flattened component names) for a given exercise type. These are
     * guaranteed to be in the same order as those returned by {@link #getAppNames}.
     */
    @VisibleForTesting
    CharSequence[] getComponents(@ExerciseKey String exercise) {
        return mAssociationModel.getWhiteListedFlattenedComponentNamesWithNoneFirst(exercise);
    }

    /**
     * Returns the entry value (flattened component name) corresponding to the selected app, or none
     * if detection isn't enabled.
     */
    @VisibleForTesting
    String getSelectedComponent(@ExerciseKey String exercise) {
        return mEnabledModel.isDetectionEnabled(exercise)
                ? mAssociationModel.getPreferredAppComponent(exercise)
                : mAssociationModel.getNoneLabel();
    }
}
