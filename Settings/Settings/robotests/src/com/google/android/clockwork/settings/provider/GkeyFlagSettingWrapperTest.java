package com.google.android.clockwork.settings.provider;

import android.content.ContentValues;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import com.google.android.gsf.GservicesValue;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

@RunWith(ClockworkRobolectricTestRunner.class)
public class GkeyFlagSettingWrapperTest {
    private static final String TEST_KEY = "test_key";
    private static final String TEST_PATH = "test_path";

    @Rule public ExpectedException thrown = ExpectedException.none();
    @Mock private GservicesValue<Boolean> mFlag;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testQuery_true() {
        // GIVEN a true flag
        Mockito.when(mFlag.get()).thenReturn(true);

        // WHEN properties is created with true
        SettingProperties props = new GkeyFlagSettingWrapper(TEST_PATH, TEST_KEY, mFlag);

        // THEN queried key should be true
        ProviderTestUtils.assertKeyValue(props, TEST_KEY, 1);
    }

    @Test
    public void testQuery_false() {
        // GIVEN a false flag
        Mockito.when(mFlag.get()).thenReturn(false);

        // WHEN properties is created with false
        SettingProperties props = new GkeyFlagSettingWrapper(TEST_PATH, TEST_KEY, mFlag);

        // THEN queried key should be false
        ProviderTestUtils.assertKeyValue(props, TEST_KEY, 0);
    }

    /** Value is immutable, throw an exception if something attempts to alter value. */
    @Test
    public void test_updateFails() {
        // GIVEN content values
        ContentValues values = new ContentValues();
        // GIVEN properties is created
        SettingProperties props = new GkeyFlagSettingWrapper(TEST_PATH, TEST_KEY, mFlag);

        // THROWS UnsupportedOperationException is immutable
        thrown.expect(UnsupportedOperationException.class);
        // WHEN properties is updated
        props.update(values);
    }
}
