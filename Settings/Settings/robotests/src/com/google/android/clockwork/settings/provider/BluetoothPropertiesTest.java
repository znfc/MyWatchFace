package com.google.android.clockwork.settings.provider;

import static com.google.android.clockwork.phone.common.Constants.GLOBAL_CALL_TWINNING_STATE_KEY;
import static com.google.android.clockwork.phone.common.Constants.STATE_OFF;
import static com.google.android.clockwork.phone.common.Constants.STATE_ON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.bluetooth.BluetoothProfile;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.utils.SettingsCursor;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import java.util.ArrayList;
import java.util.HashSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(ClockworkRobolectricTestRunner.class)
public class BluetoothPropertiesTest {
    private static final long HEADSET_CLIENT = 1 << BluetoothProfile.HEADSET_CLIENT;

    @Mock ContentResolver mockResolver;
    BluetoothProperties properties;
    SharedPreferences mPrefs;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mPrefs = ProviderTestUtils.getEmptyPrefs();
        properties = new BluetoothProperties(mPrefs, () -> mockResolver);
    }

    /**
     * Test that notify will notify the observers watching both bluetooth Uris and the given Uri.
     * Also checks that it won't notify other Uris.
     */
    @Test
    public void testNotify_differentUri() {
        // GIVEN unique mock Uri
        Uri mockUri = mock(Uri.class);

        // WHEN notifyChange is called with unique Uri
        properties.notifyChange(mockResolver, mockUri);

        // THEN bluetooth Uri was notified
        verify(mockResolver, times(1)).notifyChange(
                eq(SettingsContract.BLUETOOTH_URI), isNull());
        // THEN legacy bluetooth Uri was notified
        verify(mockResolver, times(1)).notifyChange(
                eq(SettingsContract.BLUETOOTH_MODE_URI), isNull());
        // THEN unique Uri was notified
        verify(mockResolver, times(1)).notifyChange(
                eq(mockUri), isNull());
        // THEN no other Uris were notified
        verifyNoMoreInteractions(mockResolver);
    }

    /**
     * Test that notify will notify the observers watching both bluetooth Uris only once when given
     * a bluetooth Uri. Also checks that it won't notify other Uris.
     */
    @Test
    public void testNotify_bluetoothUri() {
        // WHEN notifyChange is called with bluetooth Uri
        properties.notifyChange(mockResolver, SettingsContract.BLUETOOTH_URI);

        // THEN bluetooth Uri was notified
        verify(mockResolver, times(1)).notifyChange(
                eq(SettingsContract.BLUETOOTH_URI), isNull());
        // THEN legacy bluetooth Uri was notified
        verify(mockResolver, times(1)).notifyChange(
                eq(SettingsContract.BLUETOOTH_MODE_URI), isNull());
        // THEN no other Uris were notified
        verifyNoMoreInteractions(mockResolver);
    }

    /**
     * Test that notify will notify the observers watching both bluetooth Uris only once when given
     * a legacy bluetooth Uri. Also checks that it won't notify other Uris.
     */
    @Test
    public void testNotify_bluetoothModeUri() {
        // WHEN notifyChange is called with legacy bluetooth Uri
        properties.notifyChange(mockResolver, SettingsContract.BLUETOOTH_MODE_URI);

        // THEN bluetooth Uri was notified
        verify(mockResolver, times(1)).notifyChange(
                eq(SettingsContract.BLUETOOTH_URI), isNull());
        // THEN legacy bluetooth Uri was notified
        verify(mockResolver, times(1)).notifyChange(
                eq(SettingsContract.BLUETOOTH_MODE_URI), isNull());
        // THEN no other Uris were notified
        verifyNoMoreInteractions(mockResolver);
    }

    /**
     * Test that populateCursor contains the expected number of keys, and that the values for each
     * key accurate reflect the values in the shared preferences.
     */
    @Test
    public void testPopulateCursor_HfpClientProfileEnabled() {
        String deviceAddress = "AA:BB:CC:DD:EE:FF";
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(SettingsContract.KEY_COMPANION_ADDRESS, deviceAddress);
        editor.putInt(SettingsContract.KEY_BLUETOOTH_MODE, SettingsContract.BLUETOOTH_MODE_ALT);
        editor.putBoolean(SettingsContract.KEY_USER_HFP_CLIENT_SETTING, true);
        editor.commit();

        SettingsCursor cursor = properties.query();
        // Keeps track of which keys are in the cursor.
        HashSet<String> cursorKeys = new HashSet<String>();
        cursor.moveToFirst();

        for (int i = 0; i < cursor.getCount(); i++) {
            String key = cursor.getString(0);
            cursorKeys.add(key);
            if (key.equals(SettingsContract.KEY_COMPANION_ADDRESS)) {
                assertEquals(deviceAddress, cursor.getString(1));
            } else if (key.equals(SettingsContract.KEY_BLUETOOTH_MODE)) {
                assertEquals(SettingsContract.BLUETOOTH_MODE_ALT, cursor.getInt(1));
            } else if (key.equals(SettingsContract.KEY_USER_HFP_CLIENT_SETTING)) {
                assertEquals(SettingsContract.HFP_CLIENT_ENABLED, cursor.getInt(1));
            } else if (key.equals(SettingsContract.KEY_HFP_CLIENT_PROFILE_ENABLED)) {
                assertEquals(1, cursor.getInt(1));
            } else {
                fail("Unexpected key inserted: " + key);
            }
            cursor.moveToNext();
        }

        // There should be four elements in the cursor: address, mode, user hfp client setting, and
        // hfp client profile enabled
        ArrayList<String> expectedKeys = getExpectedKeys();
        assertTrue(cursorKeys.containsAll(expectedKeys));
    }

    /** Test if properties path matches Bluetooth path. */
    @Test
    public void testPath() {
        assertEquals("path should be Bluetooth path",
                SettingsContract.BLUETOOTH_PATH, properties.getPath());
    }

    /**
     * Test that setting BLUETOOTH_DISABLED_PROFILES to 1 << BluetoothProfile.HEADSET_CLIENT should
     * set KEY_HFP_CLIENT_PROFILE_ENABLED to 0. isHfpClientProfileEnabled() returns false when the
     * bitwise-and of Settings.Global.GetLong(Settings.Global.BLUETOOTH_DISABLED_PROFILES) and 1 <<
     * BluetoothProfile.HEADSET_CLIENT equals 1.
     */
    @Test
    public void testPopulateCursor_HfpClientProfileDisabled() {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putBoolean(SettingsContract.KEY_USER_HFP_CLIENT_SETTING, true);
        editor.commit();
        Settings.Global.putLong(
                mockResolver, Settings.Global.BLUETOOTH_DISABLED_PROFILES, HEADSET_CLIENT);

        SettingsCursor cursor = properties.query();
        // Keeps track of which keys are in the cursor.
        HashSet<String> cursorKeys = new HashSet<String>();
        cursor.moveToFirst();

        for (int i = 0; i < cursor.getCount(); i++) {
            String key = cursor.getString(0);
            cursorKeys.add(key);
            if (key.equals(SettingsContract.KEY_COMPANION_ADDRESS)) {
                // address should be defaulted to empty string if not in mPrefs
                assertEquals("", cursor.getString(1));
            } else if (key.equals(SettingsContract.KEY_BLUETOOTH_MODE)) {
                // mode should be defaulted to unknown if not in mPrefs
                assertEquals(SettingsContract.BLUETOOTH_MODE_UNKNOWN, cursor.getInt(1));
            } else if (key.equals(SettingsContract.KEY_USER_HFP_CLIENT_SETTING)) {
                assertEquals(SettingsContract.HFP_CLIENT_ENABLED, cursor.getInt(1));
            } else if (key.equals(SettingsContract.KEY_HFP_CLIENT_PROFILE_ENABLED)) {
                assertEquals(0, cursor.getInt(1));
            } else {
                fail("Unexpected key inserted: " + key);
            }
            cursor.moveToNext();
        }

        // There should still be four elements in the cursor: address, mode, user hfp client setting
        // , and hfp client profile enabled (even though only two of them were specified in
        // mPrefs)
        ArrayList<String> expectedKeys = getExpectedKeys();
        assertTrue(cursorKeys.containsAll(expectedKeys));
    }

    /**
     * Test putting a companion's bluetooth address in. This should modify the shared
     * preferences.
     */
    @Test
    public void testUpdate_CompanionAddress() {
        String deviceAddress = "AA:BB:CC:DD:EE:FF";
        int rowsChanged = properties.update(ProviderTestUtils.getContentValues(
                SettingsContract.KEY_COMPANION_ADDRESS,
                deviceAddress));

        assertEquals(deviceAddress, mPrefs.getString(SettingsContract.KEY_COMPANION_ADDRESS, ""));
        assertEquals("no rows changed, should be 1", 1, rowsChanged);
    }

    /**
     * Test inserting the companion's bluetooth mode. This should insert into shared preferences and
     * send a notification via the content resolver about the change.
     */
    @Test
    public void testUpdate_BluetoothMode() {
        int rowsChanged = properties.update(ProviderTestUtils.getContentValues(
                SettingsContract.KEY_BLUETOOTH_MODE,
                SettingsContract.BLUETOOTH_MODE_NON_ALT));

        assertEquals(SettingsContract.BLUETOOTH_MODE_NON_ALT,
                mPrefs.getInt(
                        SettingsContract.KEY_BLUETOOTH_MODE,
                        SettingsContract.BLUETOOTH_MODE_UNKNOWN));
        assertEquals("no rows changed, should be 1", 1, rowsChanged);
    }

    /**
     * Test that the user shouldn't be allowed to update the bluetooth headset if
     * GLOBAL_CALL_TWINNING_STATE_KEY's global setting is set to STATE_ON. The return value should
     * be 0 to indicate that no rows were modified.
     */
    @Test
    public void testUpdate_CallTwinningEnabled() {
        Settings.Global.putInt(mockResolver, GLOBAL_CALL_TWINNING_STATE_KEY, STATE_ON);
        ContentValues values = new ContentValues();

        values.put(SettingsContract.KEY_USER_HFP_CLIENT_SETTING, 1);

        assertEquals(0, properties.update(values));
    }

    /** Test the case where the HFP client is currently disabled and we want to enable it. */
    @Test
    public void testUpdate_EnableHfpClient() {
        Settings.Global.putLong(
                mockResolver, Settings.Global.BLUETOOTH_DISABLED_PROFILES, HEADSET_CLIENT);
        Settings.Global.putInt(mockResolver, GLOBAL_CALL_TWINNING_STATE_KEY, STATE_OFF);
        ContentValues values = new ContentValues();

        values.put(
                SettingsContract.KEY_USER_HFP_CLIENT_SETTING, SettingsContract.HFP_CLIENT_ENABLED);

        assertEquals(1, properties.update(values));
    }

    /** Test the case where the HFP client is currently enabled and we want to disable it. */
    @Test
    public void testUpdate_DisableHfpClient() {
        Settings.Global.putLong(mockResolver, Settings.Global.BLUETOOTH_DISABLED_PROFILES, 1);
        Settings.Global.putInt(mockResolver, GLOBAL_CALL_TWINNING_STATE_KEY, STATE_OFF);
        ContentValues values = new ContentValues();

        values.put(
                SettingsContract.KEY_USER_HFP_CLIENT_SETTING, SettingsContract.HFP_CLIENT_DISABLED);

        assertEquals(1, properties.update(values));
    }

    private ArrayList<String> getExpectedKeys() {
        ArrayList<String> expectedKeys = new ArrayList<String>();
        expectedKeys.add(SettingsContract.KEY_COMPANION_ADDRESS);
        expectedKeys.add(SettingsContract.KEY_BLUETOOTH_MODE);
        expectedKeys.add(SettingsContract.KEY_USER_HFP_CLIENT_SETTING);
        expectedKeys.add(SettingsContract.KEY_HFP_CLIENT_PROFILE_ENABLED);
        return expectedKeys;
    }
}
