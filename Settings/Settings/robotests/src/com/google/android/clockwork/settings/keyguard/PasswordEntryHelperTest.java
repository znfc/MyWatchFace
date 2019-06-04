package com.google.android.clockwork.settings.keyguard;

import static android.text.InputType.TYPE_CLASS_NUMBER;
import static android.text.InputType.TYPE_CLASS_TEXT;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.widget.EditText;

import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.shadows.ShadowApplication;

@RunWith(ClockworkRobolectricTestRunner.class)
public class PasswordEntryHelperTest {

    private Context mContext;

    @Before
    public void setup() {
        mContext = ShadowApplication.getInstance().getApplicationContext();
    }

    @Test
    public void testConstructor_isAlphabetic_hasTextInputType() {
        PasswordEntryHelper helper = new PasswordEntryHelper(mContext, true);
        EditText editText = helper.getPasswordEntryView();

        int inputType = editText.getInputType();
        assertEquals(TYPE_CLASS_TEXT, inputType & TYPE_CLASS_TEXT);
        assertEquals(0, inputType & TYPE_CLASS_NUMBER);
    }

    @Test
    public void testConstructor_isNotAlphabetic_hasNumericInputType() {
        PasswordEntryHelper helper = new PasswordEntryHelper(mContext, false);
        EditText editText = helper.getPasswordEntryView();

        int inputType = editText.getInputType();
        assertEquals(0, inputType & TYPE_CLASS_TEXT);
        assertEquals(TYPE_CLASS_NUMBER, inputType & TYPE_CLASS_NUMBER);
    }
}
