package com.google.android.clockwork.settings.provider;

import android.content.res.Resources;
import com.android.internal.R;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/** Test burn in properties */
@RunWith(ClockworkRobolectricTestRunner.class)
public class BurnInProtectionPropertiesTest {
    @Mock Resources mResources;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testQuery_enableBurnInProtectionTrue() {
        // GIVEN enableBurnInProtection is true
        Mockito.when(mResources.getBoolean(R.bool.config_enableBurnInProtection))
                .thenReturn(true);

        // WHEN BurnInProtectionProperties is instantiated
        SettingProperties props = new BurnInProtectionProperties(mResources);

        // THEN BurnInProtectionProperties should be true
        ProviderTestUtils.assertKeyValue(props, SettingsContract.KEY_BURN_IN_PROTECTION, 1);
    }

    @Test
    public void testQuery_enableBurnInProtectionFalse() {
        // GIVEN enableBurnInProtection is false
        Mockito.when(mResources.getBoolean(R.bool.config_enableBurnInProtection))
                .thenReturn(false);

        // WHEN BurnInProtectionProperties is instantiated
        SettingProperties props = new BurnInProtectionProperties(mResources);

        // THEN BurnInProtectionProperties should be false
        ProviderTestUtils.assertKeyValue(props, SettingsContract.KEY_BURN_IN_PROTECTION, 0);
    }

    @Test
    public void testQuery_enableBurnInProtectionDev() {
        // WHEN BurnInProtectionProperties is instantiated
        SettingProperties props = new BurnInProtectionProperties(mResources);

        // THEN burn in protection dev should be present
        ProviderTestUtils.assertKeyExists(props, SettingsContract.KEY_BURN_IN_PROTECTION_DEV);
    }

    @Test
    public void testUpdate_enableBurnInProtectionDev() {
        // WHEN properties is instantiated
        SettingProperties props = new BurnInProtectionProperties(mResources);

        // WHEN KEY_BURN_IN_PROTECTION_DEV is updated to true
        int rowsChanged = props.update(ProviderTestUtils.getContentValues(
                SettingsContract.KEY_BURN_IN_PROTECTION_DEV, 1));

        // THEN rows changed should be 1
        Assert.assertEquals("rows changed should be 1", 1, rowsChanged);
    }
}
