package com.google.android.clockwork.settings.connectivity.cellular;

import static com.google.android.clockwork.settings.connectivity.cellular.CellularSettingsFragment.getDataRoamingTitle;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(ClockworkRobolectricTestRunner.class)
public class CellularSettingsFragmentTest {
    @Test
    public void showInternationalDataRoamingOnLeDevice() {
        assertThat(getDataRoamingTitle(true /* isLocalEditionDevice */),
                is(R.string.pref_internationalDataRoaming));
    }

    @Test
    public void showDataRoamingOnRowDevice() {
        assertThat(getDataRoamingTitle(false /* isLocalEditionDevice */),
                is(R.string.pref_dataRoaming));
    }
}
