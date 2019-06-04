package com.google.android.clockwork.settings.personal.fitness;

import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.FIT_REALTIME_ACTIVITY;

import android.content.Intent;
import com.google.android.clockwork.settings.personal.fitness.models.ExerciseAssociationModel;
import com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.ExerciseKey;
import com.google.android.clockwork.settings.personal.fitness.models.ExerciseLastStartedModel;
import com.google.android.clockwork.settings.personal.fitness.models.ExercisesEnabledModel;
import com.google.android.clockwork.settings.personal.fitness.models.ExercisesSupportedModel;
import com.google.android.clockwork.settings.personal.fitness.models.RunningAppsModel;
import java.util.Arrays;

/**
 * Presenter for {@link ExerciseDetectionRelayActivity}.
 */
public class ExerciseDetectionRelayPresenter {

    private static final String NO_COMPONENT = "";

    private final ExerciseLastStartedModel mLastedStartedModel;
    private final ExerciseAssociationModel mAssociationModel;
    private final ExercisesEnabledModel mEnabledModel;
    private final ExercisesSupportedModel mSupportedModel;
    private final RunningAppsModel mRunningAppsModel;

    public ExerciseDetectionRelayPresenter(ExerciseLastStartedModel lastedStartedModel,
            ExerciseAssociationModel associationModel, ExercisesEnabledModel enabledModel,
            ExercisesSupportedModel supportedModel, RunningAppsModel runningAppsModel) {
        mLastedStartedModel = lastedStartedModel;
        mAssociationModel = associationModel;
        mEnabledModel = enabledModel;
        mSupportedModel = supportedModel;
        mRunningAppsModel = runningAppsModel;
    }

    public void onRelayRequest(@ExerciseKey String exercise, Intent implicitIntent) {
        if (!mSupportedModel.hasHardwareSupport() || !mSupportedModel.isSupported(exercise)) {
            mEnabledModel.setIsDetectionEnabled(exercise, false);
            return;
        }

        String lastStartedComponent = mLastedStartedModel.getLastStarted(exercise);
        String componentToStart = getComponentToStart(exercise, lastStartedComponent);
        CharSequence[] installedApps = mAssociationModel.getAllFlattenedComponentNames(exercise);

        if (mRunningAppsModel.isOtherFitnessAppRunning(componentToStart, installedApps)) {
            return;
        }

        boolean wasSuccessful =
                !NO_COMPONENT.equals(componentToStart)
                && mLastedStartedModel.start(exercise, componentToStart, implicitIntent);

        if (wasSuccessful) {
            mAssociationModel.setDefaultApp(exercise, componentToStart);
        } else {
            mEnabledModel.setIsDetectionEnabled(exercise, false);
        }
    }

    private String getComponentToStart(@ExerciseKey String exercise, String lastComponent) {
        String currentlyAssociatedComponent = mAssociationModel.getPreferredAppComponent(exercise);

        if (wouldNotResolveToAnActivity(currentlyAssociatedComponent)) {
            return getComponentToStartWhenDisambigWouldShow(exercise, lastComponent);
        }

        return currentlyAssociatedComponent;
    }

    private String getComponentToStartWhenDisambigWouldShow(@ExerciseKey String exercise,
            String lastStartedComponent) {
        if (mAssociationModel.isComponentResolvable(lastStartedComponent)) {
            return lastStartedComponent;
        }

        CharSequence[] installedApps =
                mAssociationModel.getWhiteListedFlattenedComponentNames(exercise);
        if (installedApps.length == 1) {
            return (String) installedApps[0];
        } else if (isFitAppPresent(installedApps)) {
            return FIT_REALTIME_ACTIVITY;
        } else {
            return NO_COMPONENT;
        }
    }

    private boolean wouldNotResolveToAnActivity(String currentlyAssociatedComponent) {
        return NO_COMPONENT.equals(currentlyAssociatedComponent);
    }

    private boolean isFitAppPresent(CharSequence[] installedApps) {
        return Arrays.stream(installedApps).anyMatch(FIT_REALTIME_ACTIVITY::equals);
    }
}
