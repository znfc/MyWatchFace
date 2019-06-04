package com.google.android.clockwork.settings.provider;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.text.TextUtils;
import junit.framework.Assert;
import org.robolectric.RuntimeEnvironment;
import com.google.android.clockwork.settings.utils.SettingsCursor;

public class ProviderTestUtils {
    public static int getIntValue(SettingProperties settingProperties, String key) {
        SettingsCursor c = settingProperties.query();
        while (c.moveToNext()) {
            if (TextUtils.equals(c.getString(0), key)) {
                return c.getInt(1);
            }
        }
        throw new RuntimeException("missing key " + key + " in " + settingProperties);
    }

    public static String getStringValue(SettingProperties settingProperties, String key) {
        SettingsCursor c = settingProperties.query();
        while (c.moveToNext()) {
            if (TextUtils.equals(c.getString(0), key)) {
                return c.getString(1);
            }
        }
        throw new RuntimeException("missing key " + key + " in " + settingProperties);
    }

    public static long getLongValue(SettingProperties settingProperties, String key) {
        SettingsCursor c = settingProperties.query();
        while (c.moveToNext()) {
            if (TextUtils.equals(c.getString(0), key)) {
                return c.getLong(1);
            }
        }
        throw new RuntimeException("missing key " + key + " in " + settingProperties);
    }

    public static boolean hasKey(SettingProperties settingProperties, String key) {
        SettingsCursor c = settingProperties.query();
        while (c.moveToNext()) {
            if (TextUtils.equals(c.getString(0), key)) {
                return true;
            }
        }
        return false;
    }

    public static void assertKeyValue(SettingProperties props, String key, String expected) {
        Assert.assertEquals("property " + props + " key \"" + key + "\" has invalid value",
                expected, getStringValue(props, key));
    }

    public static void assertKeyValue(SettingProperties props, String key, int expected) {
        Assert.assertEquals("property " + props + " key \"" + key + "\" has invalid value",
                expected, getIntValue(props, key));
    }

    public static void assertKeyValue(SettingProperties props, String key, long expected) {
        Assert.assertEquals("property " + props + " key \"" + key + "\" has invalid value",
                expected, getLongValue(props, key));
    }

    public static void assertKeyValue(SharedPreferences prefs, String key, String expected) {
        assertKeyExists(prefs, key);
        Assert.assertEquals("SharedPreferences " + prefs + " key \"" + key + "\" has invalid value",
                expected, prefs.getString(key, null));
    }

    public static void assertKeyValue(SharedPreferences prefs, String key, int expected) {
        assertKeyExists(prefs, key);
        Assert.assertEquals("SharedPreferences " + prefs + " key \"" + key + "\" has invalid value",
                expected, prefs.getInt(key, -expected));
    }

    public static void assertKeyValue(SharedPreferences prefs, String key, long expected) {
        assertKeyExists(prefs, key);
        Assert.assertEquals("SharedPreferences " + prefs + " key \"" + key + "\" has invalid value",
                expected, prefs.getLong(key, -expected));
    }

    public static void assertKeyValue(SharedPreferences prefs, String key, boolean expected) {
        assertKeyExists(prefs, key);
        Assert.assertEquals("SharedPreferences " + prefs + " key \"" + key + "\" has invalid value",
                expected, prefs.getBoolean(key, !expected));
    }

    public static void assertKeyValue(
            SettingProperties props, SharedPreferences prefs, String key, String expected) {
        assertKeyValue(props, key, expected);
        assertKeyValue(prefs, key, expected);
    }

    public static void assertKeyValue(
            SettingProperties props, SharedPreferences prefs, String key, int expected) {
        assertKeyValue(props, key, expected);
        assertKeyValue(prefs, key, expected);
    }

    public static void assertKeyValue(
            SettingProperties props, SharedPreferences prefs, String key, long expected) {
        assertKeyValue(props, key, expected);
        assertKeyValue(prefs, key, expected);
    }

    public static void assertKeyExists(SettingProperties props, String key) {
        Assert.assertTrue("property " + props + " key \"" + key + "\" is missing",
                hasKey(props, key));
    }

    public static void assertKeyExists(SharedPreferences prefs, String key) {
        Assert.assertTrue("SharedPreferences " + prefs + " key \"" + key + "\" is missing",
                prefs.contains(key));
    }

    public static void assertKeyNotExists(SettingProperties props, String key) {
        Assert.assertTrue("property " + props + " key \"" + key + "\" should be missing",
                !hasKey(props, key));
    }

    public static void assertKeyNotExists(SharedPreferences prefs, String key) {
        Assert.assertTrue("SharedPreferences " + prefs + " key \"" + key + "\" is missing",
                !prefs.contains(key));
    }

    public static SharedPreferences getEmptyPrefs() {
        SharedPreferences prefs = RuntimeEnvironment.application.getSharedPreferences(
                "settings_provider_preferences", 0);
        prefs.edit().clear().apply();
        return prefs;
    }

    public static ContentValues getContentValues(String key, int val) {
        ContentValues values = new ContentValues();
        values.put(key, val);
        return values;
    }

    public static ContentValues getContentValues(String key, String val) {
        ContentValues values = new ContentValues();
        values.put(key, val);
        return values;
    }

    public static ContentValues getContentValues(String key, long val) {
        ContentValues values = new ContentValues();
        values.put(key, val);
        return values;
    }

    public static ContentValues getContentValues(String key, boolean val) {
        ContentValues values = new ContentValues();
        values.put(key, val);
        return values;
    }
}
