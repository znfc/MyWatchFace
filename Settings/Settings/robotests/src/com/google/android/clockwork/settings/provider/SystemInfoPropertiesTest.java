package com.google.android.clockwork.settings.provider;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.common.system.WearSystemConstants;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.utils.FeatureManager;
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
import org.robolectric.shadows.ShadowApplication;

@RunWith(ClockworkRobolectricTestRunner.class)
public class SystemInfoPropertiesTest {
    @Rule public ExpectedException thrown = ExpectedException.none();
    @Mock FeatureManager mFeatureManager;
    @Mock PackageManager mPackageManager;
    @Mock Resources mRes;

    private Context mContext;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(mRes.getString(Mockito.anyInt())).thenReturn("1234");
        mContext = ShadowApplication.getInstance().getApplicationContext();
    }

    @Test
    public void testQuery_androidWearVersion() {
        when(mRes.getString(R.string.system_android_wear_version))
                .thenReturn(Long.toString(Long.MAX_VALUE));

        SettingProperties props = new SystemInfoProperties(
                mRes, () -> mPackageManager, mFeatureManager);

        ProviderTestUtils.assertKeyValue(props,
                SettingsContract.KEY_ANDROID_WEAR_VERSION, Long.MAX_VALUE);
    }

    @Test
    public void testQuery_systemCapabilities() {
        SettingProperties props = new SystemInfoProperties(
                mRes, () -> mPackageManager, mFeatureManager);

        ProviderTestUtils.assertKeyExists(props, SettingsContract.KEY_SYSTEM_CAPABILITIES);
    }

    @Test
    public void testQuery_systemEdition() {
        SettingProperties props = new SystemInfoProperties(
                mRes, () -> mPackageManager, mFeatureManager);

        ProviderTestUtils.assertKeyExists(props, SettingsContract.KEY_SYSTEM_EDITION);
    }

    @Test
    public void testQuery_localEditionDoesnotSupportAccounts() {
        when(mFeatureManager.isLocalEditionDevice()).thenReturn(true);

        SettingProperties props = new SystemInfoProperties(
                mContext.getResources(), () -> mPackageManager, mFeatureManager);
        long capabilities = ProviderTestUtils.getLongValue(props,
                SettingsContract.KEY_SYSTEM_CAPABILITIES);

        Assert.assertTrue((capabilities & (1 << WearSystemConstants.CAPABILITY_ACCOUNTS - 1)) == 0);
    }

    @Test
    public void testQuery_globalEditionDoesSupportAccounts() {
        when(mFeatureManager.isLocalEditionDevice()).thenReturn(false);

        SettingProperties props = new SystemInfoProperties(
                mContext.getResources(), () -> mPackageManager, mFeatureManager);
        long capabilities = ProviderTestUtils.getLongValue(props,
                SettingsContract.KEY_SYSTEM_CAPABILITIES);

        Assert.assertTrue((capabilities & (1 << WearSystemConstants.CAPABILITY_ACCOUNTS - 1)) != 0);
    }

    @Test
    public void testUpdate() {
        SettingProperties props = new SystemInfoProperties(
                mRes, () -> mPackageManager, mFeatureManager);


        thrown.expect(UnsupportedOperationException.class);

        props.update(ProviderTestUtils.getContentValues(
                SettingsContract.KEY_ANDROID_WEAR_VERSION, 1));
    }
}
