package com.google.android.clockwork.settings.provider;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;
import com.google.android.clockwork.settings.SettingsContract;
import java.util.Arrays;

class PropertiesPreconditions {
    private static IllegalArgumentException genError(String key, String message, Object... args) {
        throw new IllegalArgumentException(String.format("for key <%s>: %s",
                key,
                String.format(message, args)));
    }

    public static boolean checkBoolean(ContentValues values, String key) {
        Boolean newVal = values.getAsBoolean(key);
        if (newVal == null) {
            throw genError(key, "value must be boolean, given <%s>", values.get(key));
        }
        return newVal;
    }

    public static int checkInt(ContentValues values, String key) {
        Integer newVal = values.getAsInteger(key);
        if (newVal == null) {
            throw genError(key, "value must be int, given <%s>", values.get(key));
        }
        return newVal;
    }

    public static int checkInt(ContentValues values, String key,
            int... validVals) {
        Integer newVal = values.getAsInteger(key);
        if (newVal != null) {
            if (validVals == null || validVals.length == 0) {
                return newVal;
            }

            for (int validVal : validVals) {
                if (validVal == newVal) return newVal;
            }
        }

        if (validVals == null || validVals.length == 0) {
            throw genError(key, "value must be int, given<%s>", values.get(key));
        } else {
            throw genError(key, "value must be one of <%s>, given<%s>",
                    Arrays.toString(validVals), values.get(key));
        }
    }

    public static long checkLong(ContentValues values, String key) {
        Long newVal = values.getAsLong(key);
        if (newVal == null) {
            throw genError(key, "value must be long, given <%s>", values.get(key));
        }
        return newVal;
    }
}
