package com.google.android.clockwork.settings;

import com.google.android.clockwork.settings.SettingsIntents;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;

/**
  * Service responsible for handling changes to "Pay on stem" by proxying them to
  * SettingsProvider.
  */
public class PayOnStemService extends IntentService {
    private static final String TAG = "PayOnStem";

    public PayOnStemService() {
        super("PayOnStem");
        // Redeliver intent if we're killed while running tasks.
        setIntentRedelivery(true);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null 
                && SettingsIntents.ACTION_PAY_ON_STEM.equals(intent.getAction())) {
            updatePayOnStem(intent.getBooleanExtra(
                    SettingsIntents.EXTRA_PAY_ON_STEM, false));
        }
    }

    private void updatePayOnStem(boolean isPayOnStem) {
        ContentValues values = new ContentValues();
        values.put(SettingsContract.KEY_PAY_ON_STEM, isPayOnStem);
        getContentResolver().update(
                SettingsContract.PAY_ON_STEM_URI, values, null, null);
    }
}
