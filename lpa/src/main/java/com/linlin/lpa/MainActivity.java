package com.linlin.lpa;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.AcceptDenyDialogFragment;
import android.telephony.euicc.EuiccManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends WearableActivity implements AcceptDenyDialogFragment.OnClickListener {

    public static final String TAG = "zhao11 MainActivity";
    private TextView mTextView;
    private Button mButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextView = (TextView) findViewById(R.id.text);
        mButton = (Button)findViewById(R.id.mybutton);
        mTextView.setOnClickListener(this);
        mButton.setOnClickListener(this);
        // Enables Always-on
        setAmbientEnabled();
//        setAmbientOffloadEnabled(false);
    }

//    @Override
//    public void onClick(View v) {
//        EuiccManager mgr = (EuiccManager) this.getSystemService(Context.EUICC_SERVICE);
//
//        Log.i(TAG,mgr.toString());
//        Toast.makeText(this,"ddddddddd",Toast.LENGTH_LONG).show();
//    }

    @Override
    public void onClick(@NonNull AcceptDenyDialogFragment fragment, int which) {

        EuiccManager mgr = (EuiccManager) this.getSystemService(Context.EUICC_SERVICE);

        Log.i(TAG,mgr.toString());
        Toast.makeText(this,"ddddddddd",Toast.LENGTH_LONG).show();
    }
}
