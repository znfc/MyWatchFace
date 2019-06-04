package com.google.android.clockwork.settings.provider;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Build;
import android.os.SystemProperties;
import android.support.annotation.VisibleForTesting;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.common.content.CwPrefs;
import com.google.android.clockwork.host.GKeys;
import com.google.android.clockwork.settings.AdbUtil;
import com.google.android.clockwork.settings.CardPreviewModeConfig;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.notification.NotificationBackend;
import com.google.android.clockwork.settings.personal.buttons.ButtonManager;
import com.google.android.clockwork.settings.personal.buttons.ButtonUtils;
import com.google.android.clockwork.settings.personal.buttons.Constants;
import com.google.android.clockwork.settings.personal.fitness.models.ExerciseConstants;
import com.google.android.clockwork.settings.utils.FeatureManager;

import java.util.HashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;


/**
 * Wrapper around a map for easy insertion and consolidation of SettingProperties
 */
class PropertiesMap extends HashMap<String, SettingProperties> {

    private static final String PREFS_FILE = "settings_provider_preferences";

    /** System property storing the license path. */
    private static final String PROP_LICENSE_PATH = "ro.config.license_path";
    private static final String DEFAULT_LICENSE_PATH = "/system/etc/NOTICE.html.gz";

    private static final boolean DEFAULT_FITNESS_DISABLED_DURING_SETUP = false;
    private static final boolean DEFAULT_PAY_ON_STEM = false;

    private static final int DEFAULT_GMS_CHECKIN_TIMEOUT_MIN = 6;

    PropertiesMap(Supplier<Context> context) {
        this(
                context,
                context.get().getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE),
                context.get().getResources(),
                () -> context.get().getPackageManager(),
                FeatureManager.INSTANCE.get(context.get()),
                () -> context.get().getContentResolver(),
                () -> {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                        return false;
                    } else {
                        return context.get().getSystemService(LocationManager.class)
                                .isLocationEnabled();
                    }
                },
                new DefaultServiceStarter(context));
    }

    @VisibleForTesting PropertiesMap(
            Supplier<Context> context,
            SharedPreferences prefs,
            Resources res,
            Supplier<PackageManager> pm,
            FeatureManager fm,
            Supplier<ContentResolver> resolver,
            BooleanSupplier locationEnabled,
            ServiceStarter serviceStarter) {
        // ADD ALL AVAILABLE PROPERTIES
        // Misc properties
        add(initGmsCheckinProperties(prefs));
        add(initFitnessDisabledDuringSetupProperties(prefs));
        add(initExerciseDetectionProperties(prefs));
        add(initHotwordProperties(prefs, res));
        add(initSmartRepliesProperties(prefs));
        add(initCardPreviewProperties(prefs));
        add(initDefaultVibrationProperties(prefs, res));
        add(initPayOnStemProperties(prefs));
        add(initLocationProperties(prefs, pm, locationEnabled));
        add(initRetailModeProperties(prefs));
        add(initPlayStoreAvailabilityProperties(prefs));

        // OEM and System properties
        add(new CapabilitiesProperties(prefs, res));
        add(new EnhancedDebuggingProperties(res));
        add(new CustomColorProperties(prefs));
        add(new OemProperties(prefs));
        add(new SystemInfoProperties(res, pm, fm));
        add(new SystemAppNotifWhitelistProperties(res));
        add(initDeviceLicenseProperties());
        add(initBugReportProperties(prefs));

        // Bluetooth properties
        add(new BluetoothProperties(prefs, resolver));
        add(new BluetoothLegacyProperties(prefs, resolver));

        // Channels properties
        add(new ChannelsProperties(pm, context, new NotificationBackend()));

        // Display properties
        add(new AmbientProperties(context.get(), prefs, res));
        add(new BurnInProtectionProperties(res));
        add(new DisplayShapeProperties(prefs));
        add(new ScreenBrightnessProperties(res));
        add(initForceScreenTimeoutProperties());
        add(initSmartIlluminateProperties(prefs, res));
        add(initCornerRoundnessProperties(res));

        // Theater mode
        add(initDisableAmbientInTheaterModeProperties(res));

        // Time properties
        add(initAutoTimeProperties(prefs));
        add(initAutoTimeZoneProperties(prefs));
        add(init24HourTimeProperties(prefs));

        // Wi-Fi
        add(initAutoWifiProperties(prefs));
        add(initWifiPowerSaveProperties(prefs, res));
        add(initAltBypassWifiRequirementTimeProperties(prefs));
        add(initWirelessDebugConfig(prefs));

        // Wrist Gestures
        add(new WristGesturesProperties(prefs, res, resolver));
        add(new WristGesturesPrefExistsProperties(prefs));
        add(initWristGesturesEnabledProgrammaticProperties(prefs));
        add(initUpDownGesturesEnabledProperties(prefs, res));
        add(initMasterGesturesEnabledProperties());
        add(initUngazeEnabledProperties());

        // Setup
        add(initSetupSkippedProperties(prefs));
        add(new SetupLocaleProperties(prefs, fm));

        // Cellular
        add(initLastCallForwardActionProperties(prefs));
        add(new MobileSignalDetectorAllowedProperties(prefs, res));

        // Button Manager
        add(initButtonManagerConfig(prefs, context.get()));

        // Guardian Mode
        add(initGuardianModeProperties(prefs));

        // Mute When Off Body
        add(initMuteOffBodyProperties(prefs, res));

        // TapAndPay
        add(initTapAndPayProperties(prefs));

        // Wear OS
        add(initWearOsVersionProperties(prefs, res));

        // Alternate Launcher
        add(initAlternateLauncherProperties(prefs, res));

        // Time only mode
        add(new TimeOnlyModeProperties(context.get()));
    }

    SettingProperties[] toArray() {
        return values().toArray(new SettingProperties[size()]);
    }

    private void add(SettingProperties properties) {
        String path = properties.getPath();
        if (containsKey(path)) {
            throw new IllegalArgumentException("path already exists: " + path);
        }
        put(path, properties);
    }

    @VisibleForTesting
    SettingProperties initTapAndPayProperties(SharedPreferences prefs) {
        return new PreferencesProperties(prefs, SettingsContract.TAP_AND_PAY_PATH)
                .addBoolean(SettingsContract.KEY_HAS_PAY_TOKENS, false);
    }

    // TODO(b/67100119): Disconnected code to be deleted in H

    @VisibleForTesting
    SettingProperties initGmsCheckinProperties(SharedPreferences prefs) {
        return new PreferencesProperties(prefs, SettingsContract.CHECKIN_PATH)
                .addInt(SettingsContract.KEY_GMS_CHECKIN_TIMEOUT_MIN,
                        DEFAULT_GMS_CHECKIN_TIMEOUT_MIN);
    }

    @VisibleForTesting
    SettingProperties initFitnessDisabledDuringSetupProperties(
            SharedPreferences prefs) {
        return new PreferencesProperties(prefs, SettingsContract.FITNESS_DISABLED_DURING_SETUP)
                .addBoolean(SettingsContract.KEY_FITNESS_DISABLED_DURING_SETUP,
                        DEFAULT_FITNESS_DISABLED_DURING_SETUP);
    }

    @VisibleForTesting
    SettingProperties initExerciseDetectionProperties(SharedPreferences prefs) {
        PreferencesProperties properties =
                new PreferencesProperties(prefs, ExerciseConstants.EXERCISE_DETECTION_PATH);

        ExerciseConstants.EXERCISE_KEYS.forEach(key -> properties.addString(key, ""));

        return properties;
    }

    @VisibleForTesting
    SettingProperties initHotwordProperties(SharedPreferences prefs,
            Resources res) {
        return new PreferencesProperties(prefs, SettingsContract.HOTWORD_CONFIG_PATH)
                .addBoolean(SettingsContract.KEY_HOTWORD_DETECTION_ENABLED,
                        res.getBoolean(R.bool.config_hotwordDetectionDefaultEnabled));
    }

    @VisibleForTesting
    SettingProperties initSmartRepliesProperties(SharedPreferences prefs) {
        return new PreferencesProperties(prefs, SettingsContract.SMART_REPLIES_ENABLED_PATH)
                .addBoolean(SettingsContract.KEY_SMART_REPLIES_ENABLED, true);
    }

    @VisibleForTesting
    SettingProperties initCardPreviewProperties(SharedPreferences prefs) {
        return new PreferencesProperties(prefs, SettingsContract.CARD_PREVIEW_MODE_PATH)
                .addInt(SettingsContract.KEY_CARD_PREVIEW_MODE,
                        CardPreviewModeConfig.NORMAL,
                        // valid values:
                        CardPreviewModeConfig.LOW,
                        CardPreviewModeConfig.NORMAL,
                        CardPreviewModeConfig.HIGH);
    }

    @VisibleForTesting
    SettingProperties initDefaultVibrationProperties(SharedPreferences prefs,
            Resources res) {
        return new PreferencesProperties(prefs, SettingsContract.DEFAULT_VIBRATION_PATH)
                .addString(SettingsContract.KEY_DEFAULT_VIBRATION,
                        res.getString(R.string.config_default_vibration));
    }

    @VisibleForTesting
    SettingProperties initPayOnStemProperties(SharedPreferences prefs) {
        return new PreferencesProperties(prefs, SettingsContract.PAY_ON_STEM)
                .addBoolean(SettingsContract.KEY_PAY_ON_STEM, DEFAULT_PAY_ON_STEM);
    }

    @VisibleForTesting
    SettingProperties initLocationProperties(SharedPreferences prefs,
            final Supplier<PackageManager> pm, final BooleanSupplier locationEnabled) {
        return new PreferencesProperties(prefs, SettingsContract.LOCATION_CONFIG_PATH)
                .addInt(SettingsContract.KEY_OBTAIN_PAIRED_DEVICE_LOCATION,
                        () ->  locationEnabled.getAsBoolean() ? SettingsContract.VALUE_TRUE
                                : SettingsContract.VALUE_FALSE,
                        // valid values:
                        SettingsContract.VALUE_TRUE, SettingsContract.VALUE_FALSE);
    }

    @VisibleForTesting
    SettingProperties initRetailModeProperties(SharedPreferences prefs) {
        return new PreferencesProperties(prefs, SettingsContract.RETAIL_MODE_PATH)
                .addInt(SettingsContract.KEY_RETAIL_MODE,
                        SettingsContract.RETAIL_MODE_CONSUMER,
                        // valid values:
                        SettingsContract.RETAIL_MODE_CONSUMER,
                        SettingsContract.RETAIL_MODE_RETAIL);
    }

    @VisibleForTesting
    SettingProperties initPlayStoreAvailabilityProperties(
            SharedPreferences prefs) {
        return new PreferencesProperties(prefs, SettingsContract.PLAY_STORE_AVAILABILITY_PATH)
                .addInt(SettingsContract.KEY_PLAY_STORE_AVAILABILITY,
                        SettingsContract.PLAY_STORE_AVAILABILITY_UNKNOWN,
                        // valid values:
                        SettingsContract.PLAY_STORE_AVAILABLE,
                        SettingsContract.PLAY_STORE_UNAVAILABLE);
    }

    @VisibleForTesting
    SettingProperties initDeviceLicenseProperties() {
        return new ImmutableProperties(
                SettingsContract.LICENSE_PATH_PATH, SettingsContract.KEY_LICENSE_PATH,
                SystemProperties.get(PROP_LICENSE_PATH, DEFAULT_LICENSE_PATH));
    }

    @VisibleForTesting
    SettingProperties initBugReportProperties(SharedPreferences prefs) {
        return new PreferencesProperties(prefs, SettingsContract.BUG_REPORT_PATH)
                .addInt(SettingsContract.KEY_BUG_REPORT,
                        // Enable bug reporting by default for non user builds.
                        "user".equals(Build.TYPE) // is user build?
                                ? SettingsContract.BUG_REPORT_DISABLED
                                : SettingsContract.BUG_REPORT_ENABLED,
                        // valid values:
                        SettingsContract.BUG_REPORT_DISABLED,
                        SettingsContract.BUG_REPORT_ENABLED);
    }

    @VisibleForTesting
    SettingProperties initForceScreenTimeoutProperties() {
        return new ImmutableBoolProperties(
                SettingsContract.FORCE_SCREEN_TIMEOUT_PATH,
                SettingsContract.KEY_FORCE_SCREEN_TIMEOUT,
                SystemProperties.getBoolean("ro.force_screen_timeout", false));
    }

    @VisibleForTesting
    SettingProperties initSmartIlluminateProperties(SharedPreferences prefs,
            Resources res) {
        // cache beforehand, don't leak the Resources object
        final boolean defaultSmartIlluminateOn =
                res.getBoolean(R.bool.config_defaultSmartIlluminateOn);
        return new PreferencesProperties(prefs, SettingsContract.SMART_ILLUMINATE_ENABLED_PATH)
                .addBoolean(SettingsContract.KEY_SMART_ILLUMINATE_ENABLED,
                        () -> { // a supplier is used so Gkeys are only called if needed
                            // Use default value set from gkeys if available, otherwise, fall back
                            // on config overlay.
                            int smartIlluminateDefaultKey =
                                    GKeys.SMART_ILLUMINATE_DEFAULT_SETTING.get();
                            return (smartIlluminateDefaultKey < 0)
                                    // Use the config setting if there's no GKeys override.
                                    ? defaultSmartIlluminateOn
                                    // Otherwise a gkeys value > 0 means true.
                                    : (smartIlluminateDefaultKey > 0);
                        });
    }

    @VisibleForTesting
    SettingProperties initCornerRoundnessProperties(Resources res) {
        return new ImmutableProperties(
                SettingsContract.CORNER_ROUNDNESS_PATH,
                SettingsContract.KEY_CORNER_ROUNDNESS,
                res.getInteger(R.integer.config_square_screen_corner_roundness));
    }

    @VisibleForTesting
    SettingProperties initDisableAmbientInTheaterModeProperties(Resources res) {
        return new ImmutableBoolProperties(
                SettingsContract.DISABLE_AMBIENT_IN_THEATER_MODE_PATH,
                SettingsContract.KEY_DISABLE_AMBIENT_IN_THEATER_MODE,
                res.getBoolean(R.bool.config_disableAmbientInTheaterMode));
    }

    @VisibleForTesting
    SettingProperties initAutoTimeProperties(SharedPreferences prefs) {
        return new PreferencesProperties(prefs, SettingsContract.CLOCKWORK_AUTO_TIME_PATH)
                .addInt(SettingsContract.KEY_CLOCKWORK_AUTO_TIME,
                        SettingsContract.SYNC_TIME_FROM_PHONE,
                        // valid values:
                        SettingsContract.SYNC_TIME_FROM_PHONE,
                        SettingsContract.SYNC_TIME_FROM_NETWORK,
                        SettingsContract.AUTO_TIME_OFF);
    }

    @VisibleForTesting
    SettingProperties initAutoTimeZoneProperties(SharedPreferences prefs) {
        return new PreferencesProperties(prefs, SettingsContract.CLOCKWORK_AUTO_TIME_ZONE_PATH)
                .addInt(SettingsContract.KEY_CLOCKWORK_AUTO_TIME_ZONE,
                        SettingsContract.SYNC_TIME_ZONE_FROM_PHONE,
                        // valid values:
                        SettingsContract.SYNC_TIME_ZONE_FROM_PHONE,
                        SettingsContract.SYNC_TIME_ZONE_FROM_NETWORK,
                        SettingsContract.AUTO_TIME_ZONE_OFF);
    }

    @VisibleForTesting
    SettingProperties init24HourTimeProperties(SharedPreferences prefs) {
        return new PreferencesProperties(prefs, SettingsContract.CLOCKWORK_24HR_TIME_PATH)
                .addBoolean(SettingsContract.KEY_CLOCKWORK_24HR_TIME, false);
    }

    @VisibleForTesting
    SettingProperties initAutoWifiProperties(SharedPreferences prefs) {
        return new PreferencesProperties(prefs, SettingsContract.AUTO_WIFI_PATH)
                .addBoolean(SettingsContract.KEY_AUTO_WIFI, true);
    }

    @VisibleForTesting
    SettingProperties initWifiPowerSaveProperties(SharedPreferences prefs,
            Resources res) {
        // cache beforehand, don't leak the Resources object
        final int configValue = res.getInteger(
                R.integer.config_defaultOffChargerWifiUsageLimitMinutes);
        return new PreferencesProperties(prefs, SettingsContract.WIFI_POWER_SAVE_PATH)
                .addInt(SettingsContract.KEY_WIFI_POWER_SAVE,
                        () -> { // use a supplier to only access Gkeys as needed
                            // If DEFAULT_OFF_CHARGER_WIFI_USAGE_LIMIT_MINUTES fails to fetch value
                            // from GServices (which returns a negative value), we should use the
                            // value from config.xml. So that Moto can have different value from
                            // overlay.
                            int gKeysValue =
                                    GKeys.DEFAULT_OFF_CHARGER_WIFI_USAGE_LIMIT_MINUTES.get();
                            return gKeysValue < 0 ? configValue : gKeysValue;
                        });
    }

    @VisibleForTesting
    SettingProperties initAltBypassWifiRequirementTimeProperties(
            SharedPreferences prefs) {
        return new PreferencesProperties(prefs,
                SettingsContract.ALT_BYPASS_WIFI_REQUIREMENT_TIME_PATH)
                .addLong(SettingsContract.KEY_ALT_BYPASS_WIFI_REQUIREMENT_TIME_MILLIS, 0L);
    }

    @VisibleForTesting
    SettingProperties initWirelessDebugConfig(SharedPreferences prefs) {
        return new PreferencesProperties(prefs, AdbUtil.WIRELESS_DEBUG_CONFIG_PATH)
                .addInt(AdbUtil.KEY_WIRELESS_DEBUG_MODE, AdbUtil.WIRELESS_DEBUG_OFF,
                        //valid vals:
                        AdbUtil.WIRELESS_DEBUG_OFF,
                        AdbUtil.WIRELESS_DEBUG_BLUETOOTH,
                        AdbUtil.WIRELESS_DEBUG_WIFI)
                .addInt(AdbUtil.KEY_WIFI_DEBUG_PORT, AdbUtil.ADB_DEFAULT_PORT);
    }

    @VisibleForTesting
    SettingProperties initWristGesturesEnabledProgrammaticProperties(
            SharedPreferences prefs) {
        return new PreferencesProperties(prefs,
                SettingsContract.WRIST_GESTURES_ENABLED_PROGRAMMATIC_PATH)
                .addBoolean(SettingsContract.KEY_WRIST_GESTURES_ENABLED_PROGRAMMATIC, false);
    }

    @VisibleForTesting
    SettingProperties initUpDownGesturesEnabledProperties(
            SharedPreferences prefs, Resources res) {
        return new PreferencesProperties(prefs, SettingsContract.UPDOWN_GESTURES_ENABLED_PATH)
                .addBoolean(SettingsContract.KEY_UPDOWN_GESTURES_ENABLED,
                        res.getBoolean(R.bool.config_upDownGesturesDefaultEnabled));
    }

    @VisibleForTesting
    SettingProperties initMasterGesturesEnabledProperties() {
        return new GkeyFlagSettingWrapper(
                SettingsContract.MASTER_GESTURES_ENABLED_PATH,
                SettingsContract.KEY_MASTER_GESTURES_ENABLED,
                GKeys.GESTURE_CONTROL_ENABLED);
    }

    @VisibleForTesting
    SettingProperties initUngazeEnabledProperties() {
        return new GkeyFlagSettingWrapper(
                SettingsContract.UNGAZE_ENABLED_PATH,
                SettingsContract.KEY_UNGAZE_ENABLED,
                GKeys.UNGAZE_DEFAULT_SETTING);
    }

    @VisibleForTesting
    SettingProperties initSetupSkippedProperties(SharedPreferences prefs) {
        return new PreferencesProperties(prefs, SettingsContract.SETUP_SKIPPED_PATH)
                .addInt(SettingsContract.KEY_SETUP_SKIPPED,
                        SettingsContract.SETUP_SKIPPED_UNKNOWN,
                        // valid values:
                        SettingsContract.SETUP_SKIPPED_NO,
                        SettingsContract.SETUP_SKIPPED_YES);
    }

    @VisibleForTesting
    SettingProperties initLastCallForwardActionProperties(
            SharedPreferences prefs) {
        return new PreferencesProperties(prefs, SettingsContract.LAST_CALL_FORWARD_ACTION_PATH)
                .addInt(SettingsContract.KEY_LAST_CALL_FORWARD_ACTION,
                        SettingsContract.CALL_FORWARD_NO_LAST_ACTION,
                        // valid values:
                        SettingsContract.CALL_FORWARD_ACTION_ON,
                        SettingsContract.CALL_FORWARD_ACTION_OFF,
                        SettingsContract.CALL_FORWARD_NO_LAST_ACTION);
    }

    @VisibleForTesting
    SettingProperties initButtonManagerConfig(SharedPreferences prefs, Context context) {
        SharedPreferences sharedPreferences = CwPrefs.DEFAULT.get(context);
        PreferencesProperties props = new PreferencesProperties(prefs,
                ButtonManager.BUTTON_MANAGER_CONFIG_PATH);
        for (int keycode : ButtonUtils.CONFIGURABLE_BUTTON_KEYCODES) {
            // Migrate existing data (Feldspar -> Gold) and remove the SharedPref for both type and
            // data.
            String dataKey = ButtonUtils.getStemDataKey(keycode);
            String typeKey = ButtonUtils.getStemTypeKey(keycode);
            String defaultDataKey = ButtonUtils.getStemDefaultDataKey(keycode);
            String existingData = sharedPreferences.getString(dataKey, null);
            sharedPreferences.edit().remove(dataKey).remove(typeKey).apply();

            props.addInt(typeKey, Constants.STEM_TYPE_APP_LAUNCH)
                    .addString(dataKey, existingData)
                    .addString(defaultDataKey,
                            ButtonUtils.getStemDefaultDataValue(context.getResources(), keycode));
        }
        return props;
    }

    @VisibleForTesting
    SettingProperties initGuardianModeProperties(SharedPreferences prefs) {
        return new PreferencesProperties(prefs, SettingsContract.GUARDIAN_MODE_PATH)
                .addString(SettingsContract.KEY_GUARDIAN_MODE_PACKAGE, null);
    }

    @VisibleForTesting
    SettingProperties initMuteOffBodyProperties(SharedPreferences prefs,
            Resources res) {
        return new PreferencesProperties(prefs, SettingsContract.MUTE_WHEN_OFF_BODY_CONFIG_PATH)
                .addBoolean(SettingsContract.KEY_MUTE_WHEN_OFF_BODY_ENABLED,
                        res.getBoolean(R.bool.config_muteWhenOffBodyEnabled));
    }

    @VisibleForTesting
    SettingProperties initWearOsVersionProperties(SharedPreferences prefs,
            Resources res) {
        return new PreferencesProperties(prefs, SettingsContract.WEAR_OS_VERSION_PATH)
                .addString(SettingsContract.KEY_WEAR_OS_VERSION_STRING, "");
    }

    @VisibleForTesting SettingProperties initAlternateLauncherProperties(SharedPreferences prefs,
            Resources res) {
        return new PreferencesProperties(prefs, SettingsContract.ALTERNATE_LAUNCHER_PATH)
                .addBoolean(SettingsContract.KEY_ALTERNATE_LAUNCHER_ENABLED,
                        res.getBoolean(R.bool.config_alternateLauncherEnabled));
    }
}
