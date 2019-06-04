package com.google.android.clockwork.settings.provider;

import android.content.ContentValues;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.Test;

@RunWith(ClockworkRobolectricTestRunner.class)
public class ImmutablePropertiesTest {
    private static final String TEST_KEY = "test_key";
    private static final String TEST_PATH = "test_path";

    @Rule public ExpectedException thrown = ExpectedException.none();

    /** Persist a basic string. */
    @Test
    public void test_basic() {
        // GIVEN a String
        String s = "a given string";

        // WHEN properties is created with the given string
        SettingProperties props = new ImmutableProperties(TEST_PATH, TEST_KEY, s);

        // THEN queried key should match given string
        ProviderTestUtils.assertKeyValue(props, TEST_KEY, s);
    }

    /** Value is immutable, throw an exception if something attempts to alter value. */
    @Test
    public void test_updateFails() {
        // GIVEN content values
        ContentValues values = new ContentValues();
        // GIVEN properties is created
        SettingProperties props = new ImmutableProperties(TEST_PATH, TEST_KEY, true);

        // THROWS UnsupportedOperationException is immutable
        thrown.expect(UnsupportedOperationException.class);
        // WHEN properties is updated
        props.update(values);
    }
}
