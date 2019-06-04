package com.google.android.clockwork.settings.input;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class InputMethodMigrator {
    private static final String TAG = "InputMethodMigrator";

    @VisibleForTesting
    static final String PREFS_NAME = "input_method_migrator";

    private static final String ONE_TIME_ENABLED = "one_time_enabled_";
    private static final boolean DEBUG = false;

    /**
     * Transitions between old and new input methods. The old Id will be disabled
     * and the new id will be enabled.
     */
    public static void migrateInputMethodId(Context context, String oldInputMethodId,
            String newInputMethodId, boolean setAsDefault) {
        migrateInputMethodId(context,
            (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE),
            oldInputMethodId, newInputMethodId, setAsDefault);
    }

    @VisibleForTesting
    static void migrateInputMethodId(Context context, InputMethodManager imm,
            String oldInputMethodId, String newInputMethodId, boolean setAsDefault) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        final String oneTimeEnabledKey = ONE_TIME_ENABLED + newInputMethodId;

        boolean wasEnabled = prefs.getBoolean(oneTimeEnabledKey, false);
        if (wasEnabled) {
            if (DEBUG) {
                Log.d(TAG, "Migration already performed for " + newInputMethodId);
            }
            return;
        }

        // Ensure newInputMethodId exists.
        List<InputMethodInfo> inputMethodList = imm.getInputMethodList();
        boolean newImiIdExists = false;
        for (InputMethodInfo imi : inputMethodList) {
            if (imi.getId().equals(newInputMethodId)) {
                newImiIdExists = true;
                break;
            }
        }
        if (!newImiIdExists) {
            if (DEBUG) {
                Log.d(TAG, "New input method is not installed: " + newInputMethodId);
            }
            return;
        }

        String enabledInputMethodsString =
            Settings.Secure.getString(context.getContentResolver(), Secure.ENABLED_INPUT_METHODS);

        HashMap<String, HashSet<String>> enabledInputMethods = InputMethodAndSubtypeUtil
            .parseInputMethodsAndSubtypesString(enabledInputMethodsString);


        if (enabledInputMethods.containsKey(oldInputMethodId)) {
            // Old inputmethod id is present. Update things.
            // Transfer enabled subtypes (if any) to the new id.
            HashSet<String> enabledSubtypes = enabledInputMethods.remove(oldInputMethodId);

            // Only add if it's not already enabled (to avoid overwriting subtypes)
            if (!enabledInputMethods.containsKey(newInputMethodId)) {
                Log.i(TAG, "Enabling: " + newInputMethodId);
                enabledInputMethods.put(newInputMethodId, enabledSubtypes);
            } else {
                Log.w(TAG, "Was already enabled: " + newInputMethodId);
            }
        }

        // Serialize.
        enabledInputMethodsString =
            InputMethodAndSubtypeUtil.buildInputMethodsAndSubtypesString(enabledInputMethods);

        // Enable.
        Settings.Secure.putString(context.getContentResolver(), Secure.ENABLED_INPUT_METHODS,
            enabledInputMethodsString);

        if (setAsDefault) {
            Log.i(TAG, "Setting as default: " + newInputMethodId);
            Settings.Secure.putString(context.getContentResolver(),
                Settings.Secure.DEFAULT_INPUT_METHOD, newInputMethodId);
        }
        // Record this as migrated to avoid future checks.
        prefs.edit().putBoolean(oneTimeEnabledKey, true).commit();
    }
}
