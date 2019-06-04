package com.google.android.clockwork.settings.personal.fitness;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;

/**
 * Relays detected exercise intents from Fit platform to the user's exercise app of choice.
 *
 * <p>Fit Platform will fire an explicit intent to this {@link Activity} when it has been detected
 * that the user has begun exercising. This activity will then pass the intent along to {@link
 * RelayIntentService} which decides which app to launch based on which app was launched last time
 * for that activity, whether exercise detection should be enabled for that activity, and whether
 * the app is still installed. It will only fire explicit intents. It will also disable/enable and
 * update which app is associated with the particular exercise as needed.
 *
 * <p>This Activity will allow us to avoid displaying disambiguation dialogs when a supported
 * exercise is detected by only ever firing explicit intents.
 */
public class ExerciseDetectionRelayActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
        finish();
    }

    @VisibleForTesting
    void handleIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        intent.setClass(this, RelayIntentService.class);
        startService(intent);
    }
}
