package com.google.android.clockwork.settings;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;

import com.google.android.clockwork.settings.SettingsIntents;

/**
  * Service responsible for handling changes to "disable Fitness" by proxying them to
  * SettingsProvider.
  */
public class DisableFitnessService extends IntentService {
    private static final String TAG = "DisableFitnessService";

    public DisableFitnessService() {
        super("DisableFitnessService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        if (SettingsIntents.ACTION_DISABLE_FITNESS_DURING_SETUP.equals(action)) {
            disableFitness(intent.getBooleanExtra(
                    SettingsIntents.EXTRA_FITNESS_DISABLED_DURING_SETUP, false));
        }
    }

    private void disableFitness(boolean disabled) {
        ContentValues values = new ContentValues();
        values.put(SettingsContract.KEY_FITNESS_DISABLED_DURING_SETUP, disabled);
        getContentResolver().update(
                SettingsContract.FITNESS_DISABLED_DURING_SETUP_URI, values, null, null);
    }
}
