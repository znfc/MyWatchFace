package com.google.android.clockwork.settings;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Fragment;
import android.content.Intent;
import android.provider.Settings;
import com.google.android.clockwork.settings.connectivity.nfc.NfcSettingsFragment;
import com.google.android.clockwork.settings.utils.BluetoothModeManager;
import com.google.android.clockwork.settings.utils.DefaultBluetoothModeManager;
import com.google.android.clockwork.settings.utils.FeatureManager;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;

@RunWith(ClockworkRobolectricTestRunner.class)
public class MainSettingsActivityTest {
    @Mock BluetoothModeManager mBluetoothModeManager;
    @Mock FeatureManager mFeatureManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        FeatureManager.INSTANCE.setTestInstance(mFeatureManager);
        DefaultBluetoothModeManager.INSTANCE.setTestInstance(mBluetoothModeManager);
    }

    @After
    public void tearDown() {
        FeatureManager.INSTANCE.clearTestInstance();
        DefaultBluetoothModeManager.INSTANCE.clearTestInstance();
    }

    @Test
    public void startNfcSettingsFragment_when_ACTION_NFC_SETTINGS() {
        Intent nfcSettingsIntent = new Intent(Settings.ACTION_NFC_SETTINGS);
        MainSettingsActivity mainSettingsActivity =
                Robolectric.buildActivity(MainSettingsActivity.class, nfcSettingsIntent)
                        .create().start().resume().visible()
                        .get();
        Fragment fragment = mainSettingsActivity
                .getFragmentManager()
                .findFragmentById(android.R.id.content);
        assertTrue(fragment instanceof NfcSettingsFragment);
    }

    @Test
    public void initFeatureManager_androidCompanion() {
        when(mBluetoothModeManager.getBluetoothMode())
                .thenReturn(SettingsContract.BLUETOOTH_MODE_NON_ALT);

        Robolectric.buildActivity(
            MainSettingsActivity.class, new Intent(Settings.ACTION_NFC_SETTINGS))
                .create();

        verify(mFeatureManager).setIosMode(false);
    }

    @Test
    public void initFeatureManager_iosCompanion() {
        when(mBluetoothModeManager.getBluetoothMode())
                .thenReturn(SettingsContract.BLUETOOTH_MODE_ALT);

        Robolectric.buildActivity(
            MainSettingsActivity.class, new Intent(Settings.ACTION_NFC_SETTINGS))
                .create();

        verify(mFeatureManager).setIosMode(true);
    }

    @Test
    public void initFeatureManager_unknownCompanion() {
        when(mBluetoothModeManager.getBluetoothMode())
                .thenReturn(SettingsContract.BLUETOOTH_MODE_UNKNOWN);

        Robolectric.buildActivity(
            MainSettingsActivity.class, new Intent(Settings.ACTION_NFC_SETTINGS))
                .create();

        verify(mFeatureManager, never()).setIosMode(anyBoolean());
    }
}
