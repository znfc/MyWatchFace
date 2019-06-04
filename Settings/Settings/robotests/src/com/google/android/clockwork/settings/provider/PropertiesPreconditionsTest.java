package com.google.android.clockwork.settings.provider;

import android.content.ContentValues;
import android.content.SharedPreferences;
import com.google.android.clockwork.common.setup.wearable.Constants;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.Test;

@RunWith(ClockworkRobolectricTestRunner.class)
public class PropertiesPreconditionsTest {
    private static final String TEST_KEY = "test_key";

    @Rule public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testCheckBoolean_booleanTrue() {
        ContentValues values = new ContentValues();
        values.put(TEST_KEY, true);

        boolean actual = PropertiesPreconditions.checkBoolean(values, TEST_KEY);
        Assert.assertEquals(true, actual);
    }

    @Test
    public void testCheckBoolean_booleanFalse() {
        ContentValues values = new ContentValues();
        values.put(TEST_KEY, false);

        boolean actual = PropertiesPreconditions.checkBoolean(values, TEST_KEY);
        Assert.assertEquals(false, actual);
    }

    @Test
    public void testCheckBoolean_missing() {
        ContentValues values = new ContentValues();

        thrown.expect(IllegalArgumentException.class);
        PropertiesPreconditions.checkBoolean(values, TEST_KEY);
    }

    @Test
    public void testCheckBoolean_null() {
        ContentValues values = new ContentValues();
        values.put(TEST_KEY, (Boolean) null);

        thrown.expect(IllegalArgumentException.class);
        PropertiesPreconditions.checkBoolean(values, TEST_KEY);
    }

    @Test
    public void testCheckInt_value() {
        ContentValues values = new ContentValues();
        values.put(TEST_KEY, 193);

        int actual = PropertiesPreconditions.checkInt(values, TEST_KEY);
        Assert.assertEquals(193, actual);
    }

    @Test
    public void testCheckInt_missing() {
        ContentValues values = new ContentValues();

        thrown.expect(IllegalArgumentException.class);
        PropertiesPreconditions.checkInt(values, TEST_KEY);
    }

    @Test
    public void testCheckInt_null() {
        ContentValues values = new ContentValues();
        values.put(TEST_KEY, (Integer) null);

        thrown.expect(IllegalArgumentException.class);
        PropertiesPreconditions.checkInt(values, TEST_KEY);
    }

    @Test
    public void testCheckInt_valid() {
        ContentValues values = new ContentValues();
        values.put(TEST_KEY, 103);

        int actual = PropertiesPreconditions.checkInt(values, TEST_KEY, 151, 103);
        Assert.assertEquals(103, actual);
    }

    @Test
    public void testCheckInt_invalid() {
        ContentValues values = new ContentValues();
        values.put(TEST_KEY, 43);

        thrown.expect(IllegalArgumentException.class);
        PropertiesPreconditions.checkInt(values, TEST_KEY, 211, 137);
    }

    @Test
    public void testCheckInt_invalid_missing() {
        ContentValues values = new ContentValues();

        thrown.expect(IllegalArgumentException.class);
        PropertiesPreconditions.checkInt(values, TEST_KEY, 89, 131);
    }

    @Test
    public void testCheckInt_invalid_null() {
        ContentValues values = new ContentValues();
        values.put(TEST_KEY, (Integer) null);

        thrown.expect(IllegalArgumentException.class);
        PropertiesPreconditions.checkInt(values, TEST_KEY, 13, 223);
    }

    @Test
    public void testCheckLong_value() {
        ContentValues values = new ContentValues();
        values.put(TEST_KEY, 199L);

        long actual = PropertiesPreconditions.checkLong(values, TEST_KEY);
        Assert.assertEquals(199L, actual);
    }

    @Test
    public void testCheckLong_missing() {
        ContentValues values = new ContentValues();

        thrown.expect(IllegalArgumentException.class);
        PropertiesPreconditions.checkLong(values, TEST_KEY);
    }

    @Test
    public void testCheckLong_null() {
        ContentValues values = new ContentValues();
        values.put(TEST_KEY, (Integer) null);

        thrown.expect(IllegalArgumentException.class);
        PropertiesPreconditions.checkLong(values, TEST_KEY);
    }

}
