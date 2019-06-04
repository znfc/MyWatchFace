package com.google.android.clockwork.settings.provider;

import android.content.ContentValues;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import com.google.android.clockwork.settings.utils.SettingsCursor;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.Test;

@RunWith(ClockworkRobolectricTestRunner.class)
public class SettingPropertiesTest {
    private static final String TEST_PATH = "test_path";

    @Rule public ExpectedException thrown = ExpectedException.none();

    @Test
    public void test_path() {
        // WHEN instantiated with path
        SettingProperties props = new DummySettingProperties(TEST_PATH);

        // THEN expected path is returned
        Assert.assertEquals("path does not match specified", TEST_PATH, props.getPath());
    }

    @Test
    public void test_path_null() {
        // THROWS IllegalArgumentException
        thrown.expect(IllegalArgumentException.class);
        // WHEN instantiated with null path
        new DummySettingProperties(null);
    }

    @Test
    public void test_path_empty() {
        // THROWS IllegalArgumentException
        thrown.expect(IllegalArgumentException.class);
        // WHEN instantiated with empty path
        new DummySettingProperties("");
    }

    @Test
    public void test_backupAndRestore() {
        // WHEN instantiated with empty path
        BackupAndRestoreSettingProperties prop = new BackupAndRestoreSettingProperties("PATH");

        // THEN backup and restore are commutative
        prop.restore(prop.getBackupData());
    }

    class DummySettingProperties extends SettingProperties {
        DummySettingProperties(String path) {
            super(path);
        }

        @Override
        public SettingsCursor query() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int update(ContentValues values) {
            throw new UnsupportedOperationException();
        }
    }

    class BackupAndRestoreSettingProperties extends SettingProperties {
        BackupAndRestoreSettingProperties(String path) {
            super(path);
        }

        @Override
        public SettingsCursor query() {
            SettingsCursor cursor = new SettingsCursor();
            cursor.addRow("key1", "val1");
            cursor.addRow("key2", true);
            cursor.addRow("key3", 1);
            return cursor;
        }

        @Override
        public int update(ContentValues values){
            Assert.assertEquals("val1", values.getAsString("key1"));
            Assert.assertTrue(values.getAsBoolean("key2"));
            Assert.assertEquals(Integer.valueOf(1), values.getAsInteger("key3"));
            return 1;
        }
    }
}
