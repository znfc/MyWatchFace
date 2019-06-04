package com.google.android.clockwork.settings.personal.fitness.models;

import static android.content.pm.PackageManager.MATCH_DEFAULT_ONLY;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.annotation.Nullable;
import android.util.ArrayMap;
import android.util.Log;
import com.google.android.apps.wearable.resolver.ResolverActivity;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.ConfigureChosenAppsService;
import com.google.android.clockwork.settings.SettingsIntents;
import com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.ExerciseKey;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Models the associations between {@link ExerciseKey ExerciseKeys} and {@link Activity Activities}.
 */
public class ExerciseAssociationModel {

    private static final String TAG = "ExerciseDetectionModel";

    private final String mNoneLabel;
    private final Context mContext;
    private final PackageManager mPackageManager;
    private final PackageWhiteListModel mWhiteListModel;
    private final Map<String, List<ResolveInfo>> mAllActivitiesCache =
            new ArrayMap<>(ExerciseConstants.INTENTS.size());
    private final ComponentName mResolverComponent;

    public ExerciseAssociationModel(Context context, PackageWhiteListModel whiteListModel,
            PackageManager packageManager) {
        mContext = context;
        mPackageManager = packageManager;
        mNoneLabel = context.getResources().getString(R.string.exerciseDetection_none);
        mWhiteListModel = whiteListModel;
        mResolverComponent = new ComponentName(mContext, ResolverActivity.class);
    }

    /** Returns the {@link String} associated with "None" being selected. */
    public String getNoneLabel() {
        return mNoneLabel;
    }

    /** Returns {@code true} if {@code exercise} has an {@link Activity} associated with it. */
    public boolean hasDefaultApp(@ExerciseKey String exercise) {
        ComponentName preferredComponent = getPreferredAppComponentName(exercise);
        return preferredComponent != null && !isSystemResolverActivity(preferredComponent);
    }

    /**
     * Changes which {@link ComponentName} is associated with the {@code exercise}.
     */
    public void setDefaultApp(@ExerciseKey String exercise, String flattenedComponentName) {
        if (flattenedComponentName == null) {
            return;
        }

        ComponentName componentName = ComponentName.unflattenFromString(flattenedComponentName);
        if (componentName == null) {
            return;
        }

        Intent originalIntent;
        if (ExerciseConstants.INTENTS.containsKey(exercise)) {
            originalIntent = ExerciseConstants.INTENTS.get(exercise);
        } else {
            if (Log.isLoggable(TAG, Log.ERROR)) {
                Log.e(TAG, "unrecognized key encountered when changing default app: " + exercise);
            }
            return;
        }

        PendingIntent verificationIntent = PendingIntent.getActivity(mContext, 0, new Intent(), 0);
        Intent intent = new Intent(mContext, ConfigureChosenAppsService.class)
                .setAction(SettingsIntents.ACTION_SET_LAST_CHOSEN_APP)
                .putExtra(SettingsIntents.EXTRA_PENDING_INTENT_KEY, verificationIntent)
                .putExtra(SettingsIntents.EXTRA_ORIGINAL_INTENT, originalIntent)
                .putExtra(SettingsIntents.EXTRA_COMPONENT_NAME, componentName);

        ComponentName startedComponentName = mContext.startService(intent);
        if (Log.isLoggable(TAG, Log.ERROR) && startedComponentName == null) {
            Log.e(TAG, "Failed to modify default app for " + exercise);
        }
    }

    /**
     * Returns an array of app labels to display for the {@code exercise}, preceded with a
     * translated None. As an example, if they only have Fit installed, that would be:
     * {@code ["None", "Fit Activity"]}.
     */
    public CharSequence[] getWhiteListedAppLabelsWithNoneFirst(@ExerciseKey String exercise) {
        List<ResolveInfo> activities = getWhiteListedActivitiesForExercise(exercise);
        return mapResolveInfos(activities, info -> info.loadLabel(mPackageManager));
    }

    /**
     * Returns an array of flattened {@link ComponentName ComponentNames} that support the exercise,
     * preceded by a translated None. As an example, if they only have Fit installed, that would be:
     * {"None", "com.google.android.apps.fitness/com.[...].fitness.realtime.RealtimeActivity"}.
     */
    public CharSequence[] getWhiteListedFlattenedComponentNamesWithNoneFirst(
            @ExerciseKey String exercise) {
        List<ResolveInfo> activities = getWhiteListedActivitiesForExercise(exercise);
        return mapResolveInfos(activities, this::flattenComponentName);
    }

    /**
     * Returns an array of flattened {@link ComponentName ComponentNames} that support the exercise
     * and are white listed. As an example, if they only have Fit installed, that would be:
     * {"com.google.android.apps.fitness/com.[...].fitness.realtime.RealtimeActivity"}.
     */
    public CharSequence[] getWhiteListedFlattenedComponentNames(@ExerciseKey String exercise) {
        return getWhiteListedActivitiesForExercise(exercise).stream()
                .map(this::flattenComponentName)
                .toArray(CharSequence[]::new);
    }

    /**
     * Returns an array of flattened {@link ComponentName ComponentNames} that support the exercise.
     * As an example, if they only have Fit installed, that would be:
     * {"com.google.android.apps.fitness/com.[...].fitness.realtime.RealtimeActivity"}.
     */
    public CharSequence[] getAllFlattenedComponentNames(@ExerciseKey String exercise) {
        return getAllActivitiesForExercise(exercise).stream()
                .map(this::flattenComponentName)
                .toArray(CharSequence[]::new);
    }

    /**
     * Returns an app label given a flattened {@link ComponentName}. For example, this would return
     * "Fit Activity" if given the {@code RealtimeActivity} component.
     */
    public String getAppLabelFromComponentName(String flattenedComponentName) {
        ComponentName component = ComponentName.unflattenFromString(flattenedComponentName);
        Intent intent = new Intent().setComponent(component);
        ResolveInfo resolveInfo = mPackageManager.resolveActivity(intent, MATCH_DEFAULT_ONLY);
        return resolveInfo == null ? mNoneLabel : (String) resolveInfo.loadLabel(mPackageManager);
    }

    /**
     * Returns {@code true} if the flattened {@link ComponentName} is resolvable. If it is, then it
     * is both installed and enabled.
     */
    public boolean isComponentResolvable(String flattenedComponentName) {
        ComponentName component = ComponentName.unflattenFromString(flattenedComponentName);
        Intent intent = new Intent().setComponent(component);
        ResolveInfo resolveInfo = mPackageManager.resolveActivity(intent, MATCH_DEFAULT_ONLY);
        return resolveInfo != null
                && resolveInfo.activityInfo != null
                && mWhiteListModel.isWhiteListed(resolveInfo.activityInfo.packageName);
    }

    /**
     * Returns the app label of the preferred Activity associated with {@code exercise}. For
     * example, if Fit is the default running app, this would return "Fit Activity". If no activity
     * could be found, this will return an empty String.
     */
    public String getPreferredAppLabelForExercise(@ExerciseKey String exercise) {
        Intent implicitIntent = ExerciseConstants.INTENTS.get(exercise);
        ResolveInfo info = mPackageManager.resolveActivity(implicitIntent, MATCH_DEFAULT_ONLY);
        if (info != null
                && info.activityInfo != null
                && info.activityInfo.packageName != null
                && info.activityInfo.name != null
                && !info.activityInfo.packageName.equals(mResolverComponent.getPackageName())
                && !info.activityInfo.name.equals(mResolverComponent.getClassName())
                && mWhiteListModel.isWhiteListed(info.activityInfo.packageName)) {
            // activityInfo isn't needed to load a label, but elsewhere we consider that to be a
            // non-package, so do so here as well.
            return (String) info.loadLabel(mPackageManager);
        }

        return "";
    }

    /**
     * Returns the flattened {@link ComponentName} of the preferred Activity associated with
     * {@code exercise}, or an empty String if none could be found.
     */
    public String getPreferredAppComponent(@ExerciseKey String prefKey) {
        ComponentName componentName = getPreferredAppComponentName(prefKey);
        if (componentName == null || isSystemResolverActivity(componentName)) {
            return "";
        }

        return componentName.flattenToString();
    }

    /**
     * Maps over {@code activities}, transforming each {@link ResolveInfo} into a {@link
     * CharSequence}, and returns an array of them with {@link #mNoneLabel} prepended.
     */
    private CharSequence[] mapResolveInfos(List<ResolveInfo> activities,
            Function<ResolveInfo, CharSequence> mapper) {
        return Stream
                .concat(Stream.of(mNoneLabel), activities.stream().map(mapper))
                .toArray(CharSequence[]::new);
    }

    private List<ResolveInfo> getWhiteListedActivitiesForExercise(@ExerciseKey String exercise) {
        return getAllActivitiesForExercise(exercise)
                .stream()
                .filter(info -> mWhiteListModel.isWhiteListed(info.activityInfo.packageName))
                .collect(Collectors.toList());
    }

    private List<ResolveInfo> getAllActivitiesForExercise(@ExerciseKey String exercise) {
        List<ResolveInfo> activities = mAllActivitiesCache.getOrDefault(exercise, null);
        if (activities != null) {
            return activities;
        }

        Intent intent = ExerciseConstants.INTENTS.getOrDefault(exercise, null);
        if (intent == null) {
            if (Log.isLoggable(TAG, Log.ERROR)) {
                Log.e(TAG, String.format("Unsupported exercise: %s", exercise));
            }
            return Collections.emptyList();
        }

        activities = mPackageManager.queryIntentActivities(intent, MATCH_DEFAULT_ONLY).stream()
                .filter(info -> info.activityInfo != null)
                .collect(Collectors.toList());

        mAllActivitiesCache.put(exercise, activities);
        return activities;
    }

    @Nullable
    private ComponentName getPreferredAppComponentName(@ExerciseKey String prefKey) {
        Intent intent = ExerciseConstants.INTENTS.getOrDefault(prefKey, null);
        ResolveInfo info = mPackageManager.resolveActivity(intent, MATCH_DEFAULT_ONLY);
        if (info != null
                && info.activityInfo != null
                && info.activityInfo.packageName != null
                && info.activityInfo.name != null
                && mWhiteListModel.isWhiteListed(info.activityInfo.packageName)) {
            return new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
        }

        return null;
    }

    private CharSequence flattenComponentName(ResolveInfo info) {
        return new ComponentName(info.activityInfo.packageName, info.activityInfo.name)
                .flattenToString();
    }

    private boolean isSystemResolverActivity(ComponentName componentName) {
        return mResolverComponent.equals(componentName);
    }
}
