package com.google.android.clockwork.settings.apps;

import android.content.Context;
import com.google.android.clockwork.settings.shadows.TestSettingsProvider;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.robolectric.annotation.Config;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

@RunWith(ClockworkRobolectricTestRunner.class)
public class VibrationModeTest {

    private VibrationModeConfig mConfig;

    @Before
    public void setup() {
        Context context = ShadowApplication.getInstance().getApplicationContext();

        TestSettingsProvider.initForTesting();

        mConfig = new VibrationModeConfig(context);
    }

    @Test
    public void testDefaultNormal() {
        Assert.assertEquals(mConfig.getVibrationMode(), VibrationModeConfig.NORMAL);
    }

    @Test
    @Config(qualifiers="en")
    public void testParsePatternsEnglish() {
        testParsePatterns();
    }

    @Test
    @Config(qualifiers="zh-rCN")
    public void testParsePatternsChinese() {
        testParsePatterns();
    }

    @Test
    @Config(qualifiers="ar")
    public void testParsePatternsArabic() {
        testParsePatterns();
    }

    private void testParsePatterns() {
        Assert.assertNotNull(AppsSettingsFragment.parseVibrationPattern(
                mConfig.getVibrationPatternForMode(VibrationModeConfig.LONG)));
        Assert.assertNotNull(AppsSettingsFragment.parseVibrationPattern(
                mConfig.getVibrationPatternForMode(VibrationModeConfig.DOUBLE)));
        Assert.assertNotNull(AppsSettingsFragment.parseVibrationPattern(
                mConfig.getVibrationPatternForMode(VibrationModeConfig.NORMAL)));
    }

    @Test
    public void testSaveLoad() {
        mConfig.setVibrationMode(VibrationModeConfig.LONG);
        Assert.assertEquals(mConfig.getVibrationMode(), VibrationModeConfig.LONG);

        mConfig.setVibrationMode(VibrationModeConfig.DOUBLE);
        Assert.assertEquals(mConfig.getVibrationMode(), VibrationModeConfig.DOUBLE);

        mConfig.setVibrationMode(VibrationModeConfig.NORMAL);
        Assert.assertEquals(mConfig.getVibrationMode(), VibrationModeConfig.NORMAL);
    }

}
