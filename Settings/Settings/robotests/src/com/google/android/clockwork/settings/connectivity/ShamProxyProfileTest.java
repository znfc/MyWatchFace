package com.google.android.clockwork.settings.connectivity;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.preference.PreferenceScreen;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.connectivity.bluetooth.BluetoothDevicePreference;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.shadows.ShadowApplication;

/** Tests for {@link ShamProxyProfile}. */
@RunWith(ClockworkRobolectricTestRunner.class)
public class ShamProxyProfileTest {
    private ShamProxyProfile mProfile;
    private PreferenceScreen mPrefScreen;

    @Mock BluetoothAdapter mBluetoothAdapter;

    @Before
    public void setup() {
      MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testBluetoothPhoneProxy_Connected() {
        Context context = ShadowApplication.getInstance().getApplicationContext();
        BluetoothAdapter adapter = Mockito.mock(BluetoothAdapter.class);
        mProfile = new ShamProxyProfile(context, adapter);

        mProfile.mMonitorBtProxyCallback.onAvailable(null);
        assertTrue(mProfile.isProxyConnected());
    }

    @Test
    public void testBluetoothPhoneProxy_Disconnected() {
        Context context = ShadowApplication.getInstance().getApplicationContext();
        BluetoothAdapter adapter = Mockito.mock(BluetoothAdapter.class);
        mProfile = new ShamProxyProfile(context, adapter);

        mProfile.mMonitorBtProxyCallback.onLost(null);
        assertFalse(mProfile.isProxyConnected());
    }
  }
