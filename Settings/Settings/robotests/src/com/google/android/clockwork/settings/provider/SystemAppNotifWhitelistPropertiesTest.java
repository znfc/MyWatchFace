package com.google.android.clockwork.settings.provider;

import android.content.res.Resources;
import android.database.Cursor;
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
public class SystemAppNotifWhitelistPropertiesTest {
    @Rule public ExpectedException thrown = ExpectedException.none();
    @Mock Resources mRes;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testQuery() {
        // GIVEN 3 whitelist values
        Mockito.when(mRes.getStringArray(R.array.system_app_notif_whitelist))
                .thenReturn(new String[] {"1", "2", "3"});

        // GIVEN properties is instantiated
        SettingProperties props = new SystemAppNotifWhitelistProperties(mRes);

        // WHEN properties is queried
        Cursor c = props.query();

        // THEN cursor matches given whitelist values
        Assert.assertEquals("invalid number of rows", 3, c.getCount());
        c.moveToFirst();
        Assert.assertEquals("1", c.getString(1));
        c.moveToNext();
        Assert.assertEquals("2", c.getString(1));
        c.moveToNext();
        Assert.assertEquals("3", c.getString(1));
    }

    @Test
    public void testUpdate() {
        // GIVEN properties is created
        SettingProperties props = new SystemAppNotifWhitelistProperties(mRes);

        // THROWS UnsupportedOperationException is immutable
        thrown.expect(UnsupportedOperationException.class);
        // WHEN properties is updated
        props.update(ProviderTestUtils.getContentValues(
                SettingsContract.KEY_SYSTEM_APPS_NOTIF_WHITELIST, 1));
    }
}
