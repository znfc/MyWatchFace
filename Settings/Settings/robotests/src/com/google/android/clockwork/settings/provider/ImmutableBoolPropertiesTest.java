package com.google.android.clockwork.settings.provider;

import android.content.ContentValues;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.Test;

@RunWith(ClockworkRobolectricTestRunner.class)
public class ImmutableBoolPropertiesTest {
    private static final String TEST_KEY = "test_key";
    private static final String TEST_PATH = "test_path";

    @Rule public ExpectedException thrown = ExpectedException.none();

    /** Persist false. */
    @Test
    public void test_basicFalse() {
        // WHEN properties is created with false
        SettingProperties props = new ImmutableBoolProperties(TEST_PATH, TEST_KEY, false);

        // THEN queried key should be false
        ProviderTestUtils.assertKeyValue(props, TEST_KEY, 0);
    }

    /** Persist true. */
    @Test
    public void test_basicTrue() {
        // WHEN properties is created with true
        SettingProperties props = new ImmutableBoolProperties(TEST_PATH, TEST_KEY, true);

        // THEN queried key should be true
        ProviderTestUtils.assertKeyValue(props, TEST_KEY, 1);
    }

    /** Value is immutable, throw an exception if something attempts to alter value. */
    @Test
    public void test_updateFails() {
        // GIVEN content values
        ContentValues values = new ContentValues();
        // GIVEN properties is created
        SettingProperties props = new ImmutableBoolProperties(TEST_PATH, TEST_KEY, true);

        // THROWS UnsupportedOperationException is immutable
        thrown.expect(UnsupportedOperationException.class);
        // WHEN properties is updated
        props.update(values);
    }
}
