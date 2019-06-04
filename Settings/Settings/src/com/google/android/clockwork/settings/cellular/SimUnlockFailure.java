package com.google.android.clockwork.settings.cellular;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.apps.wearable.settings.R;

public class SimUnlockFailure extends Activity {
    private TextView simLockedText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sim_unlock_failure);
        simLockedText = (TextView) findViewById(R.id.sim_locked_text);
        boolean sPukPin = getIntent().getBooleanExtra(Constants.EXTRA_IS_PUK_PIN, false);

        simLockedText.setText(getString(sPukPin
            ? R.string.puk_unlock_too_many_attempts: R.string.sim_unlock_too_many_attempts));
    }
}
