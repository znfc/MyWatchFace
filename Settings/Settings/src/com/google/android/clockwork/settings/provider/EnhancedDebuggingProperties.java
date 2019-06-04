package com.google.android.clockwork.settings.provider;

import android.content.ContentValues;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.os.SystemProperties;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.utils.SettingsCursor;

class EnhancedDebuggingProperties extends SettingProperties {
    // Keep this list of tags in-sync with the namespace list in
    // device/google/clockwork/sepolicy/property_contexts
    private static final String[] ENHANCED_DEBUGGING_TAGS = {
        "log.tag.WearableService",
        "log.tag.rpcs",
        "log.tag.rpctransport",
        "log.tag.MicReader",
        "log.tag.SearchClient",
        "log.tag.voicelatency",
        "log.tag.voicelatencyutil",
        "log.tag.VoiceLatencyLogger"
    };

    private boolean mEnableEnhancedDebugging;

    public EnhancedDebuggingProperties(Resources res) {
        super(SettingsContract.ENHANCED_DEBUGGING_CONFIG_PATH);
        mEnableEnhancedDebugging = res.getBoolean(R.bool.config_enableEnhancedDebugging);
    }

    @Override
    public SettingsCursor query() {
        return new SettingsCursor(SettingsContract.KEY_ENHANCED_DEBUGGING,
                mEnableEnhancedDebugging ? 1 : 0);
    }

    @Override
    public int update(ContentValues values) {
        boolean newVal = PropertiesPreconditions.checkBoolean(
                values, SettingsContract.KEY_ENHANCED_DEBUGGING);
        if (newVal == mEnableEnhancedDebugging) {
            return 0;
        } else {
            mEnableEnhancedDebugging = newVal;
            for (String tagName : ENHANCED_DEBUGGING_TAGS) {
                SystemProperties.set(tagName, mEnableEnhancedDebugging ? "VERBOSE" : "");
            }
            return 1;
        }
    }
}
