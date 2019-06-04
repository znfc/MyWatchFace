package com.linlin.lpa2;

import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.telephony.euicc.EuiccManager;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends WearableActivity {

    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EuiccManager mgr = (EuiccManager) getSystemService(EUICC_SERVICE);
        mTextView = (TextView) findViewById(R.id.text);

        boolean isEnabled = mgr.isEnabled();
       Log.i("zhao11","isEnabled:"+isEnabled);
        // Enables Always-on
        setAmbientEnabled();
    }
}
