package com.google.android.clockwork.settings.keyguard;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.os.UserHandle;

import com.android.internal.widget.LockPatternUtils;

import java.lang.ref.WeakReference;

/**
 * Helper for showing a pattern/pin/password verification activity.
 */
public class ChooseLockSettingsHelper {

    public static final String EXTRA_KEY_PASSWORD = "password";

    /**
     * If a pattern, password or PIN exists, prompt the user before allowing them to change it.
     * @return true if one exists and we launched an activity to confirm it
     * @param request startActivityForResult request code.
     */
    public static boolean launchConfirmationActivity(Activity parent, int request) {
        if (parent == null) {
            return false;
        }

        LockPatternUtils lockPatternUtils = new LockPatternUtils(parent);
        int userId = UserHandle.myUserId();
        switch (lockPatternUtils.getActivePasswordQuality(userId)) {
            case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                parent.startActivityForResult(new Intent(parent, RetrieveLockPattern.class),
                        request);
                return true;
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
            case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
            case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
            case DevicePolicyManager.PASSWORD_QUALITY_MANAGED:
                parent.startActivityForResult(new Intent(parent, RetrieveLockPassword.class),
                        request);
                return true;
        }

        return false;
    }
}
