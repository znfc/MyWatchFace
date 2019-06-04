package com.google.android.clockwork.settings.personal.buttons;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.view.KeyEvent;

import com.google.android.clockwork.common.content.CwPrefs;
import com.google.android.clockwork.settings.SettingsIntents;

public class StemPressedActivity extends Activity {
  private static final String TAG = "StemPressedActivity";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Intent intent = getIntent();

    int keycode = getKeycodeFromIntent(intent);
    String oldComponent = intent.getStringExtra(SettingsIntents.EXTRA_OLD_COMPONENT);
    String newComponent = intent.getStringExtra(SettingsIntents.EXTRA_NEW_COMPONENT);
    if (keycode == -1) {
      Log.w(TAG, "Stem key not set up.");
    } else {

      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, "Received keycode: " + keycode);
      }

      ButtonManager bm = new ButtonManager(this);

      startActivity(bm.getIntentForButton(keycode, oldComponent, newComponent));
    }

    // b/32122353: The invisible Activity seems to be still on top.
    finish();
  }

  @VisibleForTesting
  int getKeycodeFromIntent(Intent intent) {
    int stemPressed = intent.getIntExtra(SettingsIntents.EXTRA_STEM_ID, -1);

    switch (stemPressed) {
      case 1:
        return KeyEvent.KEYCODE_STEM_1;
      case 2:
        return KeyEvent.KEYCODE_STEM_2;
      case 3:
        return KeyEvent.KEYCODE_STEM_3;
      default:
        if (Build.TYPE.equals("user")) {
          return -1;
        } else {
          throw new IllegalArgumentException(
              "The extra 'stem_id' did not contain a valid "
                  + "stem number.  Received <" + stemPressed + ">");
        }
    }
  }
}
