package com.google.android.clockwork.settings.display;

import android.app.IActivityManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import org.mockito.Mockito;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.shadows.ShadowApplication;

@RunWith(ClockworkRobolectricTestRunner.class)
public class FontSizePreferenceTest {
    private FontSizePreference.FontManager mManager;
    private FontSizePreference mPref;
    private Context mContext;

    @Before
    public void setup() {
        mContext = ShadowApplication.getInstance().getApplicationContext();

        mManager = Mockito.mock(FontSizePreference.FontManager.class);
        Mockito.when(mManager.getInitialConfiguration()).thenReturn(new Configuration());
    }

    @Test
    public void testConstructor_simple() {
        mPref = new FontSizePreference(mContext, mContext.getResources(), mManager);
    }


    @Test
    public void testConstructor_mismatchResources() {
        Resources res = Mockito.mock(Resources.class);
        Mockito.when(res.getStringArray(R.array.text_size_labels))
                .thenReturn(new String[] { "label1", "label2"});
        Mockito.when(res.getStringArray(R.array.text_size_entries))
                .thenReturn(new String[] { "1", "2", "3"});

        mPref = new FontSizePreference(mContext, res, mManager);
    }
}
