package com.google.android.clockwork.settings.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.google.android.clockwork.settings.AdbUtil;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.personal.buttons.ButtonManager;
import com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants;
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
public class SettingsProviderTest {
    /**
     * Should match all the Uris defined in SettingsContract.
     * <p>
     * In a method as UriBuilder requires the test to be initialized first.
     *
     * @return array of all the Uris the SettingsProvider must support.
     */
    private static final Set<Uri> getSettingsContractUris() {
        return Sets.newHashSet(
                SettingsContract.RETAIL_MODE_URI,
                SettingsContract.DISPLAY_SHAPE_URI,
                SettingsContract.BUG_REPORT_URI,
                SettingsContract.MOBILE_SIGNAL_DETECTOR_URI,
                SettingsContract.BLUETOOTH_MODE_URI,
                SettingsContract.BLUETOOTH_URI,
                SettingsContract.PLAY_STORE_AVAILABILITY_URI,
                SettingsContract.ALT_BYPASS_WIFI_REQUIREMENT_TIME_URI,
                SettingsContract.CLOCKWORK_AUTO_TIME_URI,
                SettingsContract.CLOCKWORK_AUTO_TIME_ZONE_URI,
                SettingsContract.CLOCKWORK_24HR_TIME_URI,
                SettingsContract.DISABLE_AMBIENT_IN_THEATER_MODE_URI,
                SettingsContract.BURN_IN_CONFIG_URI,
                SettingsContract.AMBIENT_CONFIG_URI,
                SettingsContract.FORCE_SCREEN_TIMEOUT_URI,
                SettingsContract.LICENSE_PATH_URI,
                SettingsContract.FITNESS_DISABLED_DURING_SETUP_URI,
                SettingsContract.PAY_ON_STEM_URI,
                SettingsContract.CAPABILITIES_URI,
                SettingsContract.CHECKIN_URI,
                SettingsContract.AUTO_WIFI_URI,
                SettingsContract.WIFI_POWER_SAVE_URI,
                SettingsContract.SYSTEM_APPS_NOTIF_WHITELIST_URI,
                SettingsContract.SMART_ILLUMINATE_ENABLED_URI,
                SettingsContract.WRIST_GESTURES_ENABLED_URI,
                SettingsContract.WRIST_GESTURES_ENABLED_PROGRAMMATIC_URI,
                SettingsContract.WRIST_GESTURES_ENABLED_PREF_EXISTS_URI,
                SettingsContract.UPDOWN_GESTURES_ENABLED_URI,
                SettingsContract.MASTER_GESTURES_ENABLED_URI,
                SettingsContract.UNGAZE_ENABLED_URI,
                SettingsContract.SETUP_SKIPPED_URI,
                SettingsContract.SETUP_LOCALE_URI,
                SettingsContract.CUSTOM_COLORS_URI,
                SettingsContract.ENHANCED_DEBUGGING_CONFIG_URI,
                SettingsContract.SCREEN_BRIGHTNESS_LEVELS_URI,
                SettingsContract.SYSTEM_INFO_URI,
                SettingsContract.SMART_REPLIES_ENABLED_URI,
                SettingsContract.CARD_PREVIEW_MODE_URI,
                SettingsContract.OEM_URI,
                SettingsContract.DEFAULT_VIBRATION_URI,
                SettingsContract.LAST_CALL_FORWARD_ACTION_URI,
                SettingsContract.LOCATION_CONFIG_URI,
                SettingsContract.HOTWORD_CONFIG_URI,
                SettingsContract.CHANNELS_PATH_URI,
                SettingsContract.CORNER_ROUNDNESS_URI,
                SettingsContract.GUARDIAN_MODE_URI,
                SettingsContract.MUTE_WHEN_OFF_BODY_CONFIG_URI,
                SettingsContract.TAP_AND_PAY_PATH_URI,
                SettingsContract.WEAR_OS_VERSION_URI,
                SettingsContract.ALTERNATE_LAUNCHER_URI,
                SettingsContract.TIME_ONLY_MODE_URI);
    }

    /**
     * Returns an array of all the URIs defined (and used) only in Clockwork Settings.
     * <p>
     * In a method as UriBuilder requires the test to be initialized first.
     */
    private static final Set<Uri> getSettingsOnlyUris() {
        String[] paths = new String[] {
                AdbUtil.WIRELESS_DEBUG_CONFIG_PATH,
                ButtonManager.BUTTON_MANAGER_CONFIG_PATH,
                ExerciseConstants.EXERCISE_DETECTION_PATH,
        };
        return Arrays.stream(paths)
                .map(path -> new Uri.Builder()
                        .scheme("content")
                        .authority(SettingsContract.SETTINGS_AUTHORITY)
                        .path(path)
                        .build())
                .collect(Collectors.toSet());
    }

    /**
     * All the static Uris defined in SettingsContract.
     *
     * @return array of all the static Uri fields in SettingsContract.
     */
    private static final Set<Uri> getSettingsContractUrisViaReflection()
            throws IllegalAccessException {
        Set<Uri> uris = new HashSet<>();
        for (Field field : SettingsContract.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())
                    && Uri.class.equals(field.getType())) {
                uris.add((Uri) field.get(null));
            }
        }
        return uris;
    }

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

    private Context getContext() {
        return RuntimeEnvironment.application;
    }

    /** Ensure all queries registered with the provider return a Cursor. */
    @Test
    public void testQuery_registered() {
        // GIVEN set of loaded properties
        SettingsProvider.PropertiesWrapper wrapper = mSettingsProvider.mPropertiesSupplier.get();

        // WHEN queried for all Uris in SettingsContract
        List<Uri> missingCursors = new ArrayList<>();
        for (SettingProperties props : wrapper.properties) {
            Uri uri = new Uri.Builder().scheme("content")
                    .authority(SettingsContract.SETTINGS_AUTHORITY).path(props.getPath()).build();
            Cursor c = mContentResolver.query(uri, null, null, null, null);
            if (c == null) {
                missingCursors.add(uri);
            }
        }

        // THEN all queries should have a proper response
        Assert.assertTrue("missing the following cursors when queried: " + missingCursors,
                missingCursors.isEmpty());
    }

    /** Ensure all expected uris from SettingsContract return a Cursor. */
    @Test
    public void testQuery_expected() {
        // GIVEN expected uris
        Set<Uri> uris = getSettingsContractUris();

        // WHEN queried for all Uris in SettingsContract
        List<Uri> missingCursors = new ArrayList<>();
        for (Uri uri : uris) {
            Cursor c = mContentResolver.query(uri, null, null, null, null);
            if (c == null) {
                missingCursors.add(uri);
            }
        }

        // THEN all queries should have a proper response
        Assert.assertTrue("missing the following cursors when queried: " + missingCursors,
                missingCursors.isEmpty());
    }

    /** Ensure the backup flag cannot be externally updated. */
    @Test
    public void testUpdateBackupFlag_fails() {
        // WHEN trying to updating backup flag Uri in SettingsContract
        Uri uri = new Uri.Builder()
                .scheme("content")
                .authority(SettingsContract.SETTINGS_AUTHORITY)
                .path(SettingsBackupAgent.BACKUP_UPDATES_PATH)
                .build();
        ContentValues cv = new ContentValues();
        cv.put(SettingsContract.COLUMN_KEY, 1);
        cv.put(SettingsContract.COLUMN_VALUE, 1);
        int update = mContentResolver.update(uri, cv, null, null);

        // THEN all queries should have a proper response
        Assert.assertEquals(0, update);
    }

    /** Ensure the backup flag defaults to off. */
    @Test
    public void testQueryBackupFlag_defaultOff() {
        // GIVEN the provider is initialized

        // THEN the backup flag will be off
        Uri uriBackup = new Uri.Builder()
                .scheme("content")
                .authority(SettingsContract.SETTINGS_AUTHORITY)
                .path(SettingsBackupAgent.BACKUP_UPDATES_PATH)
                .build();
        Cursor c = mContentResolver.query(uriBackup, null, null, null, null);
        Assert.assertTrue(c.moveToFirst());
        Assert.assertEquals(0, c.getInt(c.getColumnIndex(SettingsContract.COLUMN_VALUE)));
    }

    /** Ensure the backup flag is updated when a setting updates. */
    @Test
    public void testQueryBackupFlag_selfUpdates() {
        // GIVEN tilt to wake is enabled by default

        // WHEN tilt to wake is updated to off
        Uri uriTilt = new Uri.Builder()
                .scheme("content")
                .authority(SettingsContract.SETTINGS_AUTHORITY)
                .path(SettingsContract.AMBIENT_CONFIG_PATH)
                .build();
        ContentValues cv = new ContentValues();
        cv.put(SettingsContract.KEY_AMBIENT_TILT_TO_WAKE, false);
        int update = mContentResolver.update(uriTilt, cv, null, null);

        // THEN the backup flag will be on
        Uri uriBackup = new Uri.Builder()
                .scheme("content")
                .authority(SettingsContract.SETTINGS_AUTHORITY)
                .path(SettingsBackupAgent.BACKUP_UPDATES_PATH)
                .build();
        Cursor c = mContentResolver.query(uriBackup, null, null, null, null);
        Assert.assertTrue(c.moveToFirst());
        Assert.assertEquals(1, c.getInt(c.getColumnIndex(SettingsContract.COLUMN_VALUE)));
    }

    /** Ensure the backup flag resets after a backup pass. */
    @Test
    public void testQueryBackupFlag_resets() {
        // GIVEN tilt to wake is updated to off
        Uri uriTilt = new Uri.Builder()
                .scheme("content")
                .authority(SettingsContract.SETTINGS_AUTHORITY)
                .path(SettingsContract.AMBIENT_CONFIG_PATH)
                .build();
        ContentValues cv = new ContentValues();
        cv.put(SettingsContract.KEY_AMBIENT_TILT_TO_WAKE, false);
        int update = mContentResolver.update(uriTilt, cv, null, null);

        // WHEN a backup pass completes
        RuntimeEnvironment.application.sendBroadcast(
                new Intent("com.google.android.clockwork.settings.ACTION_BACKUP"),
                "com.google.android.clockwork.settings.permission.PROVIDER_BACKUP");

        // THEN the backup flag will be off
        Uri uriBackup = new Uri.Builder()
                .scheme("content")
                .authority(SettingsContract.SETTINGS_AUTHORITY)
                .path(SettingsBackupAgent.BACKUP_UPDATES_PATH)
                .build();
        Cursor c = mContentResolver.query(uriBackup, null, null, null, null);
        Assert.assertTrue(c.moveToFirst());
        Assert.assertEquals(0, c.getInt(c.getColumnIndex(SettingsContract.COLUMN_VALUE)));
    }

    /** Ensure the backup flag resets after a backup pass. */
    @Test
    public void testQuery_afterRestore() {
        // GIVEN tilt to wake defaults to on and we backup its data
        PropertiesMap map = new PropertiesMap(this::getContext);
        AmbientProperties prop = (AmbientProperties) map.get(SettingsContract.AMBIENT_CONFIG_PATH);
        byte[] backupData = prop.getBackupData();

        // AND tilt to wake is then turned off
        Uri uriTilt = new Uri.Builder()
                .scheme("content")
                .authority(SettingsContract.SETTINGS_AUTHORITY)
                .path(SettingsContract.AMBIENT_CONFIG_PATH)
                .build();
        ContentValues cv = new ContentValues();
        cv.put(SettingsContract.KEY_AMBIENT_TILT_TO_WAKE, false);
        int update = mContentResolver.update(uriTilt, cv, null, null);

        // WHEN a restore pass runs
        map = new PropertiesMap(this::getContext);
        prop = (AmbientProperties) map.get(SettingsContract.AMBIENT_CONFIG_PATH);
        prop.restore(backupData);
        RuntimeEnvironment.application.sendBroadcast(
                new Intent("com.google.android.clockwork.settings.ACTION_RESTORE"),
                "com.google.android.clockwork.settings.permission.PROVIDER_BACKUP");

        // THEN tilt to wake will be off
        Uri uriBackup = new Uri.Builder()
                .scheme("content")
                .authority(SettingsContract.SETTINGS_AUTHORITY)
                .path(SettingsContract.AMBIENT_CONFIG_PATH)
                .build();
        Cursor c = mContentResolver.query(uriBackup, null, null, null, null);
        while(c.moveToNext()) {
            if(c.getString(c.getColumnIndex(SettingsContract.COLUMN_KEY)).equals(SettingsContract.KEY_AMBIENT_TILT_TO_WAKE)) {
                Assert.assertEquals(0, c.getInt(c.getColumnIndex(SettingsContract.COLUMN_VALUE)));
            }
        }
    }

    /** Ensure all expected uris from SettingsContract matches uris that are registered. */
    @Test
    public void test_registeredMatchesExpectedUris() {
        // GIVEN set of loaded properties
        SettingsProvider.PropertiesWrapper wrapper = mSettingsProvider.mPropertiesSupplier.get();
        Set<Uri> registered = Arrays.stream(wrapper.properties).map((props) ->
                new Uri.Builder().scheme("content")
                .authority(SettingsContract.SETTINGS_AUTHORITY).path(props.getPath()).build())
                .collect(Collectors.toSet());

        // GIVEN set of expected uris
        Set<Uri> expected = Sets.union(
                getSettingsContractUris(),
                getSettingsOnlyUris());

        // WHEN taking the difference of the two sets
        Set<Uri> missingExpected = Sets.difference(registered, expected);
        Set<Uri> missingRegistered = Sets.difference(expected, registered);

        // THEN the difference should be empty
        if (!missingExpected.isEmpty() || !missingRegistered.isEmpty()) {
            Assert.fail(String.format(
                    "mismatch between registered and expected uris; missing from expected uri <%s>;"
                    + " missing from registered <%s>",
                    missingExpected,
                    missingRegistered));
        }
    }

    /**
     * Ensure all expected uris from SettingsContract matches what we see from reflection on
     * SettingsContract.
     */
    @Ignore("flaky from google3 changes")
    @Test
    public void test_reflectedMatchesExpectedUris() throws IllegalAccessException {
        // GIVEN set of properties from reflection
        Set<Uri> reflected = getSettingsContractUrisViaReflection();

        // GIVEN set of expected uris
        Set<Uri> expected = getSettingsContractUris();

        // WHEN taking the difference of the two sets
        Set<Uri> missingExpected = Sets.difference(reflected, expected);
        Set<Uri> missingReflected = Sets.difference(expected, reflected);

        // THEN the difference should be empty
        if (!missingExpected.isEmpty() || !missingReflected.isEmpty()) {
            Assert.fail(String.format(
                    "mismatch between reflected and expected uris; missing from expected uri <%s>;"
                    + " missing from reflected <%s>",
                    missingExpected,
                    missingReflected));
        }
    }
}
