package com.google.android.clockwork.settings.personal.fitness;

import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.IMPLICIT_INTENT_EXTRA;
import static com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.MIME_TYPES;

import android.app.ActivityManager;
import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import com.google.android.clockwork.settings.DefaultSettingsContentResolver;
import com.google.android.clockwork.settings.SettingsContentResolver;
import com.google.android.clockwork.settings.personal.fitness.models.ExerciseAssociationModel;
import com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants.ExerciseKey;
import com.google.android.clockwork.settings.personal.fitness.models.ExerciseLastStartedModel;
import com.google.android.clockwork.settings.personal.fitness.models.ExercisesEnabledModel;
import com.google.android.clockwork.settings.personal.fitness.models.ExercisesSupportedModel;
import com.google.android.clockwork.settings.personal.fitness.models.PackageWhiteListModel;
import com.google.android.clockwork.settings.personal.fitness.models.RunningAppsModel;

/**
 * Relays detected exercise intents to the user's exercise app of choice.
 *
 * <p>Fit Platform will fire an explicit intent when it has been detected that the user has begun
 * exercising. This service will then decide which app to launch based on which app was launched
 * last time for that activity, whether exercise detection should be enabled for that activity, and
 * whether the app is still installed. It will only fire explicit intents. It will also
 * disable/enable and update which app is associated with the particular exercise as needed.
 *
 * <p>This Service will allow us to avoid displaying disambiguation dialogs when a supported
 * exercise is detected by only ever firing explicit intents.
 */
public class RelayIntentService extends IntentService {

    @VisibleForTesting ExerciseDetectionRelayPresenter mPresenter;

    @VisibleForTesting RelayIntentService(String s) {
        super(s);
    }

    public RelayIntentService() {
        super("RelayIntentService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mPresenter = createPresenter();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        Intent implicitIntent = intent.getParcelableExtra(IMPLICIT_INTENT_EXTRA);
        if (implicitIntent == null) {
            return;
        }

        String mimeType = implicitIntent.getType();
        @ExerciseKey String exercise = MIME_TYPES.inverse().get(mimeType);
        if (exercise == null) {
            return;
        }

        mPresenter.onRelayRequest(exercise, implicitIntent);
    }

    private ExerciseDetectionRelayPresenter createPresenter() {
        ContentResolver contentResolver = getContentResolver();
        SettingsContentResolver settings =
                new DefaultSettingsContentResolver(contentResolver);
        ActivityManager activityManager =
                (ActivityManager) getSystemService(ACTIVITY_SERVICE);

        PackageWhiteListModel whiteListModel = new PackageWhiteListModel();
        ExercisesEnabledModel enabledModel = new ExercisesEnabledModel(contentResolver);
        ExerciseLastStartedModel lastStartedModel = new ExerciseLastStartedModel(settings, this);
        ExerciseAssociationModel associationModel =
                new ExerciseAssociationModel(this, whiteListModel, getPackageManager());
        ExercisesSupportedModel supportedModel =
                new ExercisesSupportedModel(getResources());
        RunningAppsModel runningAppsModel = new RunningAppsModel(activityManager);

        return new ExerciseDetectionRelayPresenter(
                lastStartedModel, associationModel, enabledModel, supportedModel,
                runningAppsModel);
    }
}
