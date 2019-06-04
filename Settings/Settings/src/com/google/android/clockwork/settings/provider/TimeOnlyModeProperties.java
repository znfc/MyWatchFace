package com.google.android.clockwork.settings.provider;

import android.content.ContentValues;
import android.content.Context;
import android.os.Build;
import com.android.clockwork.power.TimeOnlyMode;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.utils.SettingsCursor;

/** Wrapper around {@link TimeOnlyMode} to expose settings to Home. */
public class TimeOnlyModeProperties extends SettingProperties {
    private final TimeOnlyMode mTimeOnlyMode;

    TimeOnlyModeProperties(Context context) {
        this(new TimeOnlyMode(context));
    }

    TimeOnlyModeProperties(TimeOnlyMode timeOnlyMode) {
        super(SettingsContract.TIME_ONLY_MODE_PATH);
        mTimeOnlyMode = timeOnlyMode;
    }

    @Override
    public SettingsCursor query() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
            return new SettingsCursor()
                .addRow(SettingsContract.KEY_TIME_ONLY_MODE_FEATURE_SUPPORTED,
                        mTimeOnlyMode.isFeatureSupported()
                                ? SettingsContract.VALUE_TRUE : SettingsContract.VALUE_FALSE)
                .addRow(SettingsContract.KEY_TIME_ONLY_MODE_DISABLE_HOME,
                        mTimeOnlyMode.isDisableHomeFeatureEnabled()
                                ? SettingsContract.VALUE_TRUE : SettingsContract.VALUE_FALSE);
        } else {
            // Used mainly for testing, as robotests have issues with some hidden APIs.
            // See b/110378645.
            return new SettingsCursor()
                .addRow(SettingsContract.KEY_TIME_ONLY_MODE_FEATURE_SUPPORTED,
                        SettingsContract.VALUE_FALSE)
                .addRow(SettingsContract.KEY_TIME_ONLY_MODE_DISABLE_HOME,
                        SettingsContract.VALUE_FALSE);
        }
    }

    @Override
    public int update(ContentValues values) {
        throw new UnsupportedOperationException("time only mode properties cannot be altered");
    }
}
