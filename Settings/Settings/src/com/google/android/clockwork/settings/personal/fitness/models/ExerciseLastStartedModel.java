package com.google.android.clockwork.settings.personal.fitness.models;

import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.EXERCISE_DETECTION_URI;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;
import com.google.android.clockwork.settings.SettingsContentResolver;
import com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.ExerciseKey;

/**
 * Gets and sets the last component started for given exercises. Additionally, as a convenience,
 * a component may be started and then recorded all by invoking {@link #start}.
 */
public class ExerciseLastStartedModel {

    private static final String TAG = "LastStartedModel";

    private final SettingsContentResolver mSettings;
    private final Context mContext;

    public ExerciseLastStartedModel(SettingsContentResolver settings, Context context) {
        mSettings = settings;
        mContext = context;
    }

    public String getLastStarted(@ExerciseKey String exercise) {
        return mSettings.getStringValueForKey(EXERCISE_DETECTION_URI, exercise, "");
    }

    void setLastStarted(@ExerciseKey String exercise, @Nullable String component) {
        mSettings.putStringValueForKey(EXERCISE_DETECTION_URI, exercise, component);
    }

    /**
     * Fires an explicit intent for an Activity resolved by adding the {@code component} to the
     * {@code implicitIntent}, returning {@code true} and recording the {@code component} if it's
     * successful. Returns {@code false} if the Activity could not be started.
     */
    public boolean start(@ExerciseKey String exercise, String component, Intent implicitIntent) {
        ComponentName componentName = ComponentName.unflattenFromString(component);
        if (componentName == null) {
            Log.e(TAG, String.format("Could not unflatten component=%s", component));
            return false;
        }

        Intent explicitIntent = new Intent(implicitIntent)
                .setComponent(componentName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            mContext.startActivity(explicitIntent);
        } catch (ActivityNotFoundException exception) {
            Log.e(TAG, String.format("Could not start component=%s", component), exception);
            return false;
        }

        setLastStarted(exercise, component);
        return true;
    }
}
