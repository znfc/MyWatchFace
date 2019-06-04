package com.google.android.clockwork.settings.provider;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.provider.Settings;
import com.android.clockwork.power.TimeOnlyMode;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(ClockworkRobolectricTestRunner.class)
@Config(sdk = 28)
public class TimeOnlyModePropertiesTest {
    private ContentResolver mContentResolver;
    private TimeOnlyModeProperties mTimeOnlyModeProperties;

    @Before
    public void setUp() {
        mContentResolver = RuntimeEnvironment.application.getContentResolver();
        mTimeOnlyModeProperties = new TimeOnlyModeProperties(
                new TimeOnlyMode(RuntimeEnvironment.application));
    }

    @Test
    public void query_enabledTrue() {
        Settings.Global.putString(
                mContentResolver,
                Settings.Global.TIME_ONLY_MODE_CONSTANTS,
                timeOnlyModeSettingsString(true, false));
        ProviderTestUtils.assertKeyValue(mTimeOnlyModeProperties,
                SettingsContract.KEY_TIME_ONLY_MODE_FEATURE_SUPPORTED, 1);
    }

    @Test
    public void query_enabledFalse() {
        Settings.Global.putString(
                mContentResolver,
                Settings.Global.TIME_ONLY_MODE_CONSTANTS,
                timeOnlyModeSettingsString(false, false));
        ProviderTestUtils.assertKeyValue(mTimeOnlyModeProperties,
                SettingsContract.KEY_TIME_ONLY_MODE_FEATURE_SUPPORTED, 0);
    }

    @Test
    public void query_disableHomeTrue() {
        Settings.Global.putString(
                mContentResolver,
                Settings.Global.TIME_ONLY_MODE_CONSTANTS,
                timeOnlyModeSettingsString(false, true));
        ProviderTestUtils.assertKeyValue(mTimeOnlyModeProperties,
                SettingsContract.KEY_TIME_ONLY_MODE_DISABLE_HOME, 1);
    }

    @Test
    public void query_disableHomeFalse() {
        Settings.Global.putString(
                mContentResolver,
                Settings.Global.TIME_ONLY_MODE_CONSTANTS,
                timeOnlyModeSettingsString(false, false));
        ProviderTestUtils.assertKeyValue(mTimeOnlyModeProperties,
                SettingsContract.KEY_TIME_ONLY_MODE_DISABLE_HOME, 0);
    }

    /**
     * Returns a Settings string which corresponds to the specified time only settings.
     *
     * <p>The Settings parser expects the setting string to be of the following format:
     *
     * <pre>key1=value,key2=value,key3=value</pre>
     */
    private String timeOnlyModeSettingsString(boolean timeOnlyEnabled, boolean disableHome) {
        StringBuilder sb = new StringBuilder();
        sb.append("enabled").append("=").append(timeOnlyEnabled);
        sb.append(",");
        sb.append("disable_home").append("=").append(disableHome);
        return sb.toString();
    }
}