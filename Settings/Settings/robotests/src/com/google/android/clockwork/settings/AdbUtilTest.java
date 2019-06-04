package com.google.android.clockwork.settings;

import android.content.ContentResolver;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import com.google.android.clockwork.settings.AdbUtil;
import com.google.android.clockwork.settings.provider.SettingsProvider;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import com.google.common.collect.Sets;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

@RunWith(ClockworkRobolectricTestRunner.class)
public class AdbUtilTest {
    private ContentResolver mContentResolver;
    private SettingsProvider mSettingsProvider;

    @Before
    public void setup() {
        mContentResolver = RuntimeEnvironment.application.getContentResolver();

        mSettingsProvider = new SettingsProvider();
        ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.authority = SettingsContract.SETTINGS_AUTHORITY;
        mSettingsProvider.attachInfoForTesting(RuntimeEnvironment.application, providerInfo);
    }

    @Test
    public void getWifiDebugPort_missing() {
        // GIVEN no properties defined

        // WHEN queried for wireless debug port
        int actual = AdbUtil.getWifiDebugPort(RuntimeEnvironment.application);

        // THEN expect WIRELESS_DEBUG_OFF
        Assert.assertEquals(AdbUtil.ADB_DEFAULT_PORT, actual);
    }

    @Test
    public void getWirelessDebugSetting_missing() {
        // GIVEN no properties defined

        // WHEN queried for wireless debug setting
        int actual = AdbUtil.getWirelessDebugSetting(RuntimeEnvironment.application);

        // THEN expect WIRELESS_DEBUG_OFF
        Assert.assertEquals(AdbUtil.WIRELESS_DEBUG_OFF, actual);
    }

    @Test
    public void setWifiDebugPort() {
        // GIVEN no properties defined

        // WHEN set to 1234 for wireless debug port
        AdbUtil.setWifiDebugPort(RuntimeEnvironment.application, 1234);

        // THEN expect 1234
        Assert.assertEquals(1234, AdbUtil.getWifiDebugPort(RuntimeEnvironment.application));
    }

    @Test
    public void setWirelessDebugSetting_off() {
        // GIVEN no properties defined

        // WHEN set to bluetooth for wireless debug setting
        AdbUtil.setWirelessDebugSetting(RuntimeEnvironment.application,
                AdbUtil.WIRELESS_DEBUG_OFF);

        // THEN expect WIRELESS_DEBUG_OFF
        Assert.assertEquals(AdbUtil.WIRELESS_DEBUG_OFF,
                AdbUtil.getWirelessDebugSetting(RuntimeEnvironment.application));
    }

    @Test
    public void setWirelessDebugSetting_bluetooth() {
        // GIVEN no properties defined

        // WHEN set to bluetooth for wireless debug setting
        AdbUtil.setWirelessDebugSetting(RuntimeEnvironment.application,
                AdbUtil.WIRELESS_DEBUG_BLUETOOTH);

        // THEN expect WIRELESS_DEBUG_OFF
        Assert.assertEquals(AdbUtil.WIRELESS_DEBUG_BLUETOOTH,
                AdbUtil.getWirelessDebugSetting(RuntimeEnvironment.application));
    }

    @Test
    public void setWirelessDebugSetting_wifi() {
        // GIVEN no properties defined

        // WHEN set to bluetooth for wireless debug setting
        AdbUtil.setWirelessDebugSetting(RuntimeEnvironment.application,
                AdbUtil.WIRELESS_DEBUG_WIFI);

        // THEN expect WIRELESS_DEBUG_OFF
        Assert.assertEquals(AdbUtil.WIRELESS_DEBUG_WIFI,
                AdbUtil.getWirelessDebugSetting(RuntimeEnvironment.application));
    }
}
