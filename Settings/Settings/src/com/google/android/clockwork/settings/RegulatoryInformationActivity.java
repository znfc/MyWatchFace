package com.google.android.clockwork.settings;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

import com.google.android.apps.wearable.settings.R;

public class RegulatoryInformationActivity extends Activity {
    /**
     * Only one file should be present on the device. We will will prioritize file in vendor
     * partition, and the file in the system partition is kept for legacy reasons.
     */
    public static final String[] REGULATORY_INFO_PATHS =
            {"/vendor/etc/regulatory_info%s.png", "/system/etc/regulatory_info%s.png"};

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.regulatory_information_activity);
        ImageView view = (ImageView) findViewById(R.id.content);
        Bitmap bitmap = BitmapFactory.decodeFile(getIntent().getExtras().getString("filePath"));
        view.setImageBitmap(bitmap);
    }
}
