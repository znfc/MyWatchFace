package com.google.android.clockwork.settings.connectivity;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.connectivity.bluetooth.BluetoothDevicePreference;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.shadows.ShadowApplication;

/**
 *
 */
@RunWith(ClockworkRobolectricTestRunner.class)
public class BluetoothSettingsFragmentTest {

    private BluetoothSettingsFragment mFragment;
    @Mock BluetoothDevice mBluetoothDevice;
    @Mock BluetoothDevicePreference mBluetoothDevicePreference;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testBluetoothPhoneBonded_Connected() {
        Context context = ShadowApplication.getInstance().getApplicationContext();

        mFragment = new BluetoothSettingsFragment();

        mBluetoothDevice = mock(BluetoothDevice.class);
        mBluetoothDevicePreference = new BluetoothDevicePreference(context, mBluetoothDevice, null);

        mFragment.reallyUpdatePreferenceBondState(mBluetoothDevice, BluetoothDevice.BOND_BONDED,
                BluetoothClass.Device.Major.PHONE, mBluetoothDevicePreference,
                BluetoothProfile.STATE_CONNECTED);
        assertThat(mBluetoothDevicePreference.isEnabled(), is(true));
        assertThat(mBluetoothDevicePreference.isSelectable(), is(false));
        assertThat(mBluetoothDevicePreference.getOrder() ,
                is(BluetoothSettingsFragment.PREFERENCE_ORDER_IMPORTANT));
        assertThat(mBluetoothDevicePreference.getSummary(),
                is(context.getString(R.string.bluetooth_connected)));
    }

    @Test
    public void testBluetoothPhoneBonded_Disconnected() {
        Context context = ShadowApplication.getInstance().getApplicationContext();

        mFragment = new BluetoothSettingsFragment();
        mBluetoothDevice = mock(BluetoothDevice.class);
        mBluetoothDevicePreference = new BluetoothDevicePreference(context, mBluetoothDevice, null);

        mFragment.reallyUpdatePreferenceBondState(mBluetoothDevice, BluetoothDevice.BOND_BONDED,
                BluetoothClass.Device.Major.PHONE, mBluetoothDevicePreference,
                BluetoothProfile.STATE_DISCONNECTED);
        assertThat(mBluetoothDevicePreference.isEnabled(), is(true));
        assertThat(mBluetoothDevicePreference.isSelectable(), is(false));
        assertThat(mBluetoothDevicePreference.getOrder() ,
                is(BluetoothSettingsFragment.PREFERENCE_ORDER_IMPORTANT));
        assertThat(mBluetoothDevicePreference.getSummary(),
                is(context.getString(R.string.bluetooth_disconnected)));
    }

    @Test
    public void testBluetoothPhoneNoBond() {
        Context context = ShadowApplication.getInstance().getApplicationContext();

        mFragment = new BluetoothSettingsFragment();
        mBluetoothDevice = mock(BluetoothDevice.class);
        mBluetoothDevicePreference = new BluetoothDevicePreference(context, mBluetoothDevice, null);

        mFragment.reallyUpdatePreferenceBondState(mBluetoothDevice, BluetoothDevice.BOND_NONE,
                BluetoothClass.Device.Major.PHONE, mBluetoothDevicePreference, 10);
        assertThat(mBluetoothDevicePreference.isEnabled(), is(false));
    }

    @Test
    public void testBluetoothHeadsetBonded_ShowDevice() {
        Context context = ShadowApplication.getInstance().getApplicationContext();

        TestFragment testFragment = new TestFragment();
        mBluetoothDevice = mock(BluetoothDevice.class);
        mBluetoothDevicePreference = new BluetoothDevicePreference(context, mBluetoothDevice, null);

        testFragment.reallyUpdatePreferenceBondState(mBluetoothDevice, BluetoothDevice.BOND_BONDED,
                BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES, mBluetoothDevicePreference, 10);
        assertThat(mBluetoothDevicePreference.isEnabled(), is(true));
        assertThat(mBluetoothDevicePreference.getOrder(),
                is(BluetoothSettingsFragment.PREFERENCE_ORDER_NORMAL));

        verify(testFragment.getPreferenceScreen()).addPreference(mBluetoothDevicePreference);
    }

    @Test
    public void testBluetoothHeadsetBondedThenForgot_RemoveDevice() {
        Context context = ShadowApplication.getInstance().getApplicationContext();

        TestFragment testFragment = new TestFragment();
        mBluetoothDevice = mock(BluetoothDevice.class);
        mBluetoothDevicePreference = new BluetoothDevicePreference(context, mBluetoothDevice, null);

        testFragment.reallyUpdatePreferenceBondState(mBluetoothDevice, BluetoothDevice.BOND_BONDED,
                BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES, mBluetoothDevicePreference,
                BluetoothProfile.STATE_CONNECTED);
        assertThat(mBluetoothDevicePreference.isEnabled(), is(true));
        assertThat(mBluetoothDevicePreference.getOrder(),
                is(BluetoothSettingsFragment.PREFERENCE_ORDER_NORMAL));
        verify(testFragment.getPreferenceScreen()).addPreference(mBluetoothDevicePreference);

        // Verify unbonding and disconnecting hides the device
        testFragment.reallyUpdatePreferenceBondState(mBluetoothDevice, BluetoothDevice.BOND_NONE,
                BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES, mBluetoothDevicePreference,
                BluetoothProfile.STATE_DISCONNECTED);
        assertThat(mBluetoothDevicePreference.isEnabled(), is(false));
        verify(testFragment.getPreferenceScreen()).removePreference(mBluetoothDevicePreference);
    }

    /** Testable fragment to handle preference access */
    public static class TestFragment extends BluetoothSettingsFragment {
        private final PreferenceManager mPreferenceManager;
        private final PreferenceScreen mScreen;

        public TestFragment() {
            mPreferenceManager = mock(PreferenceManager.class);
            mScreen = mock(PreferenceScreen.class);
        }

        @Override
        public PreferenceScreen getPreferenceScreen() {
            return mScreen;
        }

        @Override
        public PreferenceManager getPreferenceManager() {
            return mPreferenceManager;
        }
    }
}
