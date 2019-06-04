package com.google.android.clockwork.settings.common;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.util.Log;

import com.google.android.clockwork.common.logging.CwEventLogger;
import com.google.common.logging.Cw.CwEvent;
import com.google.common.logging.Cw.CwSettingsUiLog;
import com.google.protos.wireless.android.clockwork.apps.logs.CwEnums.CwSettingsCustomPartnerUiEvent;
import com.google.protos.wireless.android.clockwork.apps.logs.CwEnums.CwSettingsUiEvent;

/**
 * Utility class to help with logging
 */
public final class LogUtils {
    private static final String TAG = "CwLogUtils";
    private static final int MAX_TAG_LENGTH = 23;

    /**
     * Returns a log safe tag name for the given class.
     * @param targetClass {@link Class} to create log tag for.
     * @return {@link String} representing the log tag.
     */
    public static String getSafeTag(Class targetClass) {
        String name = targetClass.getSimpleName();

        return name.length() > MAX_TAG_LENGTH ? name.substring(0, MAX_TAG_LENGTH) : name;
    }

    /**
     * Push a clearcut logging preference datum.
     */
    public static void logPreferenceSelection(@NonNull final CwEventLogger eventLogger,
        @NonNull final CwSettingsUiEvent event) {
        logPreferenceSelection(eventLogger, event, null);
    }

    /**
     * Push a clearcut logging preference datum.
     */
    public static void logPreferenceSelection(Context context, CwSettingsUiEvent event){
        logPreferenceSelection(context, event, null);
    }

    /**
     * Push a clearcut logging preference datum with a partner setting.
     */
    public static void logPreferenceSelection(Context context, CwSettingsUiEvent event,
        CwSettingsCustomPartnerUiEvent partnerEvent) {
        // This may be run in a non-system server process (e.g. phone process) which
        // does not have a logger setup.  So we catch those cases under the assumption
        // that we are in the phone process, not that there is a problem with the
        // clockwork logger setting.
        // TODO(cmanton) Remove once all preferences running in phone process are eliminated.
        // b/62660987
        try {
            if (event != null) {
                LogUtils.logPreferenceSelection(CwEventLogger.getInstance(context), event,
                    partnerEvent);
            }
        } catch (NullPointerException e) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Logger is unavailable, must be phone process");
            }
        }
    }

    private static void logPreferenceSelection(@NonNull final CwEventLogger eventLogger,
        @NonNull final CwSettingsUiEvent event,
        @Nullable final CwSettingsCustomPartnerUiEvent partnerEvent) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            StringBuilder logString = new StringBuilder("Logging preference selection event: ");
            logString.append(event);
            if (partnerEvent != null) {
                logString.append(" with partner setting key: ").append(partnerEvent);
            }
            Log.d(TAG, logString.toString());
        }
        final CwSettingsUiLog.Builder log = CwSettingsUiLog.newBuilder().setEvent(event);
        if (partnerEvent != null) {
            log.setPartnerEvent(partnerEvent);
        }
        eventLogger.logEvent(CwEvent.newBuilder().setSettingsUiLog(log));
    }
}
