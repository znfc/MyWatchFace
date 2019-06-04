package com.google.android.clockwork.settings.personal.fitness;

import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.EXERCISE_KEYS;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.annotation.StringRes;
import android.util.Log;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.personal.fitness.ExerciseDetectionSettingsPresenter.ExerciseDetectionSettingsView;
import com.google.android.clockwork.settings.personal.fitness.models.ExerciseAssociationModel;
import com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants;
import com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.ExerciseKey;
import com.google.android.clockwork.settings.personal.fitness.models.ExercisesEnabledModel;
import com.google.android.clockwork.settings.personal.fitness.models.ExercisesSupportedModel;
import com.google.android.clockwork.settings.personal.fitness.models.PackageWhiteListModel;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Exercise Detection settings.
 */
public class ExerciseDetectionSettingsFragment extends PreferenceFragment
    implements ExerciseDetectionSettingsView {

    private static final String TAG = "FitnessSettings";

    public static final String ACTION_EXERCISE_DETECTION_SETTINGS
            = "com.google.android.clockwork.settings.EXERCISE_DETECTION_SETTINGS";

    /** Maps from {@link ExerciseKey} to {@link StringRes}. */
    private static final Map<String, Integer> STRING_RESOURCE =  ImmutableMap.of(
            ExerciseConstants.WALKING_KEY, R.string.pref_exerciseDetection_walking,
            ExerciseConstants.RUNNING_KEY, R.string.pref_exerciseDetection_running,
            ExerciseConstants.BIKING_KEY, R.string.pref_exerciseDetection_biking);

    /** Maps from {@link ExerciseKey} to a list of {@link Activity Activities} that support it. */
    private Map<String, ListPreference> mPreferences;
    private ExerciseDetectionSettingsPresenter mPresenter;
    private PreferenceScreen mPreferenceScreen;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = getContext();
        Resources res = getResources();
        PackageManager pm = context.getPackageManager();
        ContentResolver contentResolver = context.getContentResolver();

        PackageWhiteListModel whiteListModel = new PackageWhiteListModel();
        ExercisesEnabledModel enabledModel = new ExercisesEnabledModel(contentResolver);
        ExercisesSupportedModel supportedModel = new ExercisesSupportedModel(res);
        ExerciseAssociationModel associationModel =
                new ExerciseAssociationModel(context, whiteListModel, pm);

        new FitnessSettingsChecker(enabledModel, supportedModel, associationModel)
                .verifyConsistency();

        mPresenter = new ExerciseDetectionSettingsPresenter(this, enabledModel, supportedModel,
                associationModel);

        addPreferencesFromResource(R.xml.prefs_exercise_detection);
        mPreferenceScreen = getPreferenceScreen();
        mPreferences = EXERCISE_KEYS.stream().collect(
                Collectors.toMap(Function.identity(), key -> (ListPreference) findPreference(key)));
        mPresenter.init();
    }

    @Override
    public void onResume() {
        super.onResume();
        mPresenter.updatePreferences();
    }

    @Override
    public void removePreference(@ExerciseKey String key) {
        mPreferenceScreen.removePreference(mPreferences.get(key));
    }

    @Override
    public void setPreferenceSummary(@ExerciseKey String key, String summary) {
        mPreferences.get(key).setSummary(summary.toUpperCase(Locale.getDefault()));
    }

    @Override
    public void updatePreference(ExerciseDetectionSettingsPresenter.SettingsEntry entry) {

        if (!STRING_RESOURCE.containsKey(entry.exercise)) {
            if (Log.isLoggable(TAG, Log.ERROR)) {
                Log.e(TAG, "Could not find string resource for " + entry.exercise);
            }
            return;
        }

        Resources res = getResources();
        String name = res.getString(STRING_RESOURCE.get(entry.exercise));
        ListPreference pref = mPreferences.get(entry.exercise);
        pref.setTitle(res.getString(R.string.exerciseDetection_exerciseLabel, name));
        pref.setDialogTitle(res.getString(R.string.exerciseDetection_defaultAppTitle, name));
        pref.setEntries(entry.appNames);
        pref.setEntryValues(entry.components);
        pref.setSummary(entry.selectedAppName.toUpperCase(Locale.getDefault()));
        pref.setValue(entry.selectedComponent);
        pref.setOnPreferenceChangeListener((changedPref, value) ->
                mPresenter.onSelectionChanged(changedPref.getKey(), value.toString()));
    }
}
