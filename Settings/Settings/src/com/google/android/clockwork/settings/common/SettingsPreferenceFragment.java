package com.google.android.clockwork.settings.common;

import android.os.Build;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

import com.google.protos.wireless.android.clockwork.apps.logs.CwEnums.CwSettingsCustomPartnerUiEvent;
import com.google.protos.wireless.android.clockwork.apps.logs.CwEnums.CwSettingsUiEvent;

/** Setting preference fragment layer to catch preference clicks */
public abstract class SettingsPreferenceFragment extends PreferenceFragment {
    private static final String TAG = "CwSettings";

    public static final String EXTRA_LOGGING_KEY = "logging_key";
    public static final String PARTNER_PREFERENCE_KEY_PREFIX = "pref_custom:";

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, String.format("on screen %s; click on %s", preferenceScreen, preference));
        }
        String prefLoggingKey = preference.getExtras().getString(EXTRA_LOGGING_KEY, null);
        if (prefLoggingKey == null) {
            prefLoggingKey = preference.getKey();
        }
        if (prefLoggingKey != null && prefLoggingKey.startsWith(PARTNER_PREFERENCE_KEY_PREFIX)) {
            CwSettingsCustomPartnerUiEvent partnerEvent =
                CwSettingsCustomPartnerUiEvent.PARTNER_SETTING_UNKNOWN;
            String partnerEnumIndexString = prefLoggingKey
                .substring(PARTNER_PREFERENCE_KEY_PREFIX.length());
            try {
                int enumIndex = Integer.parseInt(partnerEnumIndexString);
                if (CwSettingsCustomPartnerUiEvent.internalGetVerifier().isInRange(enumIndex)) {
                    partnerEvent = CwSettingsCustomPartnerUiEvent.forNumber(enumIndex);
                } else {
                    Log.e(TAG, "Unknown partner index: " + partnerEnumIndexString);
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Malformed partner key: " + prefLoggingKey);
            }
            LogUtils.logPreferenceSelection(getActivity(),
                CwSettingsUiEvent.SETTINGS_SELECTED_CUSTOM_PARTNER, partnerEvent);
        } else {
            try {
                CwSettingsUiEvent event =
                        SettingsPreferenceLogConstants.getLoggingId(prefLoggingKey);
                LogUtils.logPreferenceSelection(getActivity(), event);
            } catch (IllegalArgumentException ignore) {
                // Exception is only thrown on userdebug/eng, pop up a toast instead of crashing.
                if (Build.IS_DEBUGGABLE && getContext() != null) {
                    String loggingMessage = "NOT LOGGED: " + prefLoggingKey;
                    Toast.makeText(getContext(), loggingMessage, Toast.LENGTH_LONG).show();
                }
            }
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}
