package com.google.android.clockwork.settings.personal.fitness.models;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.IntDef;
import android.support.annotation.StringDef;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.personal.fitness.ExerciseDetectionRelayActivity;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Constants used in Exercise Detection settings.
 */
public class ExerciseConstants {

    private ExerciseConstants() {}

    /**
     * Contains implicit intent passed from Fit Platform to {@link ExerciseDetectionRelayActivity}.
     */
    public static final String IMPLICIT_INTENT_EXTRA = "extra_implicit_intent";

    private static final String TRACK_EXERCISE_ACTION = "vnd.google.fitness.TRACK";
    private static final String MIME_TYPE_PREFIX = "vnd.google.fitness.activity/";
    private static final Intent BASE_EXERCISE_INTENT = new Intent()
            .setAction(TRACK_EXERCISE_ACTION)
            .addCategory(Intent.CATEGORY_DEFAULT);

    /**
     * Path used for settings in {@link
     * com.google.android.clockwork.settings.provider.SettingsProvider}.
     */
    public static final String EXERCISE_DETECTION_PATH = "pref_exerciseDetection";
    public static final Uri EXERCISE_DETECTION_URI = new Uri.Builder()
            .scheme("content")
            .authority(SettingsContract.SETTINGS_AUTHORITY)
            .path(EXERCISE_DETECTION_PATH)
            .build();

    /** Enumeration of all exercise preference keys. */
    @Retention(RetentionPolicy.SOURCE)
    @StringDef({WALKING_KEY, RUNNING_KEY, BIKING_KEY})
    public @interface ExerciseKey {}
    public static final String WALKING_KEY = "pref_exerciseDetection_walking";
    public static final String RUNNING_KEY = "pref_exerciseDetection_running";
    public static final String BIKING_KEY = "pref_exerciseDetection_biking";

    /** List of all available {@link ExerciseKey ExerciseKeys}. */
    public static final List<String> EXERCISE_KEYS = Arrays.asList(
            WALKING_KEY,
            RUNNING_KEY,
            BIKING_KEY);

    /** Maps from {@link ExerciseKey} to mime type. */
    public static final BiMap<String, String> MIME_TYPES = ImmutableBiMap.of(
            WALKING_KEY, MIME_TYPE_PREFIX + "walking",
            RUNNING_KEY, MIME_TYPE_PREFIX + "running",
            BIKING_KEY, MIME_TYPE_PREFIX + "biking");

    /** Maps from {@link ExerciseKey} to {@link Intent}. */
    public static final Map<String, Intent> INTENTS = ImmutableMap.of(
            WALKING_KEY, new Intent(BASE_EXERCISE_INTENT).setType(MIME_TYPES.get(WALKING_KEY)),
            RUNNING_KEY, new Intent(BASE_EXERCISE_INTENT).setType(MIME_TYPES.get(RUNNING_KEY)),
            BIKING_KEY, new Intent(BASE_EXERCISE_INTENT).setType(MIME_TYPES.get(BIKING_KEY)));

    /** Flattened component name for Fit's exercise activity. */
    public static final String FIT_REALTIME_ACTIVITY = "com.google.android.apps.fitness/"
            + "com.google.android.wearable.fitness.realtime.RealtimeActivity";

    /** Tri-state enumeration used to indicate whether the Exercise has detection enabled. */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({DISABLED, UNSET, ENABLED})
    @interface EnabledStatus {}
    static final int DISABLED = -1;
    static final int UNSET = 0;
    static final int ENABLED = 1;
}
