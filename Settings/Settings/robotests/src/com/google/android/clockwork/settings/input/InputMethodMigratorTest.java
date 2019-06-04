package com.google.android.clockwork.settings.input;

import static org.mockito.MockitoAnnotations.initMocks;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import com.google.android.clockwork.settings.utils.FeatureManager;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;

import java.util.Arrays;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.shadows.ShadowApplication;

@RunWith(ClockworkRobolectricTestRunner.class)
public class InputMethodMigratorTest {

    @Mock
    InputMethodManager mockImm;

    private static final String ALTERNATE_PACKAGE =
            "com.example.inputmethods.other/.OtherInputMethod";

    private static final String ALTERNATE_CLASS =
            "com.example.inputmethods.other.OtherInputMethod";

    private static final String MAIN_PACKAGE =
            "com.example.inputmethods.keyboard";

    private static final String MAIN_OLD_CLASS =
            "com.company.apps.inputmethod.KeyboardInputMethod";

    private static final String MAIN_NEW_CLASS =
            "com.example.inputmethods.keyboard.KeyboardInputMethod";

    private Context mContext;

    InputMethodInfo mAltIme;
    InputMethodInfo mMainImeOld;
    InputMethodInfo mMainImeNew;

    @Before
    public void setUp() {
        initMocks(this);
        mContext = ShadowApplication.getInstance().getApplicationContext();
        mAltIme = new InputMethodInfo(ALTERNATE_PACKAGE, ALTERNATE_CLASS, "Alternate IME", "");
        mMainImeOld = new InputMethodInfo(MAIN_PACKAGE, MAIN_OLD_CLASS, "Main IME (Old)", "");
        mMainImeNew = new InputMethodInfo(MAIN_PACKAGE, MAIN_NEW_CLASS, "Main IME (New)", "");

        SharedPreferences mPrefs =
                mContext.getSharedPreferences(InputMethodMigrator.PREFS_NAME, Context.MODE_PRIVATE);
        mPrefs.edit().clear().commit();

        Settings.Secure.putString(mContext.getContentResolver(),
                Secure.DEFAULT_INPUT_METHOD, mAltIme.getId());
    }

    @Test
    public void migrateInputMethodId_simple() throws Exception {
        Settings.Secure.putString(mContext.getContentResolver(),
                Secure.ENABLED_INPUT_METHODS, mAltIme.getId() + ":" + mMainImeOld.getId());

        when(mockImm.getInputMethodList()).thenReturn(Arrays.asList(mAltIme, mMainImeNew));

        InputMethodMigrator.migrateInputMethodId(mContext, mockImm,  mMainImeOld.getId(),
                mMainImeNew.getId(), true);

        assertThat(Settings.Secure.getString(mContext.getContentResolver(),
                Secure.ENABLED_INPUT_METHODS),
                is(mAltIme.getId() + ":" + mMainImeNew.getId()));

        assertThat(Settings.Secure.getString(mContext.getContentResolver(),
                Secure.DEFAULT_INPUT_METHOD),
                is(mMainImeNew.getId()));
    }

    @Test
    public void migrateInputMethodId_withSubtypes() throws Exception {
        Settings.Secure.putString(mContext.getContentResolver(),
                Secure.ENABLED_INPUT_METHODS, mAltIme.getId() + ":" + mMainImeOld.getId() +
                ";12345,67890");
        Settings.Secure.putString(mContext.getContentResolver(),
                Secure.DEFAULT_INPUT_METHOD, mAltIme.getId());

        when(mockImm.getInputMethodList()).thenReturn(Arrays.asList(mAltIme, mMainImeNew));

        InputMethodMigrator.migrateInputMethodId(mContext, mockImm,  mMainImeOld.getId(),
                mMainImeNew.getId(), true);

        assertThat(Settings.Secure.getString(mContext.getContentResolver(),
                Secure.ENABLED_INPUT_METHODS),
                is(mAltIme.getId() + ":" + mMainImeNew.getId() + ";12345,67890"));

        assertThat(Settings.Secure.getString(mContext.getContentResolver(),
                Secure.DEFAULT_INPUT_METHOD),
                is(mMainImeNew.getId()));
    }

    @Test
    public void migrateInputMethodId_oneShotOnly() throws Exception {
        Settings.Secure.putString(mContext.getContentResolver(),
                Secure.ENABLED_INPUT_METHODS, mAltIme.getId() + ":" + mMainImeOld.getId());
        Settings.Secure.putString(mContext.getContentResolver(),
                Secure.DEFAULT_INPUT_METHOD, mAltIme.getId());

        when(mockImm.getInputMethodList()).thenReturn(Arrays.asList(mAltIme, mMainImeNew));

        InputMethodMigrator.migrateInputMethodId(mContext, mockImm,  mMainImeOld.getId(),
                mMainImeNew.getId(), true);

        // After migration, the user changes the default ime.
        Settings.Secure.putString(mContext.getContentResolver(),
            Secure.DEFAULT_INPUT_METHOD, mAltIme.getId());

        // And later reboots.
        InputMethodMigrator.migrateInputMethodId(mContext, mockImm,  mMainImeOld.getId(),
                mMainImeNew.getId(), true);

        // Should have no effect.
        assertThat(Settings.Secure.getString(mContext.getContentResolver(),
                Secure.DEFAULT_INPUT_METHOD), is(mAltIme.getId()));
    }

    @Test
    public void migrateInputMethodId_onlyIfNewPresent() throws Exception {
        Settings.Secure.putString(mContext.getContentResolver(),
                Secure.ENABLED_INPUT_METHODS, mAltIme.getId() + ":" + mMainImeOld.getId());
        Settings.Secure.putString(mContext.getContentResolver(),
                Secure.DEFAULT_INPUT_METHOD, mAltIme.getId());

        when(mockImm.getInputMethodList()).thenReturn(Arrays.asList(mAltIme, mMainImeOld));

        InputMethodMigrator.migrateInputMethodId(mContext, mockImm,  mMainImeOld.getId(),
                mMainImeNew.getId(), true);

        assertThat(Settings.Secure.getString(mContext.getContentResolver(),
                Secure.ENABLED_INPUT_METHODS),
                is(mAltIme.getId() + ":" + mMainImeOld.getId()));

        // Should have not been changed.
        assertThat(Settings.Secure.getString(mContext.getContentResolver(),
                Secure.DEFAULT_INPUT_METHOD), is(mAltIme.getId()));
    }
}