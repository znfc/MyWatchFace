package com.google.android.clockwork.settings.provider;

import android.content.res.Resources;
import android.graphics.Color;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

@RunWith(ClockworkRobolectricTestRunner.class)
public class EnhancedDebuggingPropertiesTest {
    @Mock Resources mRes;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testQuery_true() {
        // GIVEN resource value is true
        Mockito.when(mRes.getBoolean(R.bool.config_enableEnhancedDebugging)).thenReturn(true);

        // WHEN properties is instantiated
        SettingProperties props = new EnhancedDebuggingProperties(mRes);

        // THEN KEY_ENHANCED_DEBUGGING should be true
        ProviderTestUtils.assertKeyValue(props, SettingsContract.KEY_ENHANCED_DEBUGGING, 1);
    }

    @Test
    public void testQuery_false() {
        // GIVEN resource value is false
        Mockito.when(mRes.getBoolean(R.bool.config_enableEnhancedDebugging)).thenReturn(false);

        // WHEN properties is instantiated
        SettingProperties props = new EnhancedDebuggingProperties(mRes);

        // THEN KEY_ENHANCED_DEBUGGING should be false
        ProviderTestUtils.assertKeyValue(props, SettingsContract.KEY_ENHANCED_DEBUGGING, 0);
    }

    @Test
    public void testUpdate() {
        // GIVEN resource value is false
        Mockito.when(mRes.getBoolean(R.bool.config_enableEnhancedDebugging)).thenReturn(false);
        // GIVEN properties is instantiated
        SettingProperties props = new EnhancedDebuggingProperties(mRes);

        // WHEN KEY_ENHANCED_DEBUGGING is updated to true
        int rowsChanged = props.update(ProviderTestUtils.getContentValues(
                SettingsContract.KEY_ENHANCED_DEBUGGING, true));

        // THEN rows changed should be 1
        Assert.assertEquals("rows changed should be 1", 1, rowsChanged);
    }
}
