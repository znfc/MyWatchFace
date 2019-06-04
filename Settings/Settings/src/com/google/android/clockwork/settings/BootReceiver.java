package com.google.android.clockwork.settings;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import com.android.settingslib.Utils;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.common.concurrent.AbstractCwRunnable;
import com.google.android.clockwork.common.concurrent.Executors;
import com.google.android.clockwork.host.GKeys;
import com.google.android.clockwork.settings.cellular.SimStateNotification;
import com.google.android.clockwork.settings.input.InputMethodMigrator;
import com.google.android.clockwork.settings.personal.buttons.ButtonManager;
import com.google.android.clockwork.settings.personal.fitness.FitnessSettingsChecker;
import com.google.android.clockwork.settings.utils.FeatureManager;
import com.google.android.clockwork.settings.wifi.WifiAutoModeUtil;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "settings";

    public static final long MB_IN_BYTES = 1024 * 1024;

    private static final String WIFI_BACKOFF_DELAY_KEY = "cw_wifi_backoff_delay";
    private static final String WIFI_BACKOFF_DURATION_KEY = "cw_wifi_backoff_duration";

    // The class name of connectivity change receiver.
    private static final String CONNECTIVITY_CHANGE_RECEIVER_CLASS =
            "com.google.android.clockwork.settings.ConnectivityChangeReceiver";

    private static final String LE_SAFE_CONNECTIVITY_CHECK_HTTPS_URL =
            "https://connectivitycheck.gstatic.com/generate_204";
    private static final String LE_SAFE_CONNECTIVITY_CHECK_HTTP_URL =
            "http://connectivitycheck.gstatic.com/generate_204";

    private static final String LE_NTP_SERVER = "2.android.pool.ntp.org";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            onBootCompleted(context);
        } else {
            // Otherwise, simply starting the process during PRE_BOOT hotloads some of the settings
            // code, a minor optimization.
        }
    }

    private void onBootCompleted(Context context) {
        updateInputMethods(context);
        configureWirelessDebugging(context);
        if (BatterySaverUtil.useTwm(context)) {
            BatterySaverUtil.disablePowerSaverModeTwm(context);
        } else if (!BatterySaverUtil.isPowerSaverModeInitialized(context)) {
            BatterySaverUtil.configurePowerSaverMode(context, true);
        }
        startPowerSaveModeListenerService(context);
        configureWifiBackoff(context);
        disableAccelerometerRotation(context);
        disableTheaterMode(context);
        ensureScreenTimeout(context);
        adjustSysStorageThresholdMaxBytes(context);
        startVolumeUIService(context);
        avoidCaptivePortals(context);
        updateNtpServer(context);
        disableWifiNetworksAvailableNotification(context);
        ensureSetupComplete(context);
        startTimeSyncing(context);
        startTimeZoneSyncing(context);
        startTimeDebugService(context);
        ensureTextSizeWithinRange(context);
        checkAutoWifiOverrideSetting(context);
        overridePowerSaveForCellDevices(context);
        SimStateNotification.maybeCreateNotification(context.getApplicationContext());
        initializePayOnStem(context);
        FitnessSettingsChecker.getInstance(context).verifyConsistency().verifyDefaultAssociations();
        initializeDozeEnabledSetting(context);
        disableChargingSounds(context);
        configureDefaultVoiceProvider(context);
        ensureLocationSettingsEnabled(context);
        updateDnsOverTls(context);
    }

    /**
     * Ensures that system input methods are properly enabled.
     */
    private void updateInputMethods(Context context) {
        // Migrate Wear keyboard to the new input method ID
        // TODO: Remove after this fix has been triggered for all internal users (dogfooders, etc).
        // See b/33679233 for details
        InputMethodMigrator.migrateInputMethodId(context,
                "com.google.android.inputmethod.latin"
                        + "/com.google.android.apps.inputmethod.wear.latin.WearLatinIME",
                "com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME",
                true /* setAsDefault */);
    }

    /**
     * Start the {@link PowerSaveModeListenerService} to check for battery saver mode changes.
     */
    private void startPowerSaveModeListenerService(Context context) {
        Intent intent = new Intent(context, PowerSaveModeListenerService.class);
        context.startService(intent);
    }

    /**
     * Notify {@link TimeService} that it needs to start the appropriate time syncer.
     */
    private void startTimeSyncing(Context context) {
        Intent intent = new Intent (context, TimeService.class);
        intent.setAction(SettingsIntents.ACTION_EVALUATE_TIME_SYNCING);
        context.startService(intent);
    }

    /**
     * Notify {@link TimeService} that it needs to start the appropriate time zone syncer.
     */
    private void startTimeZoneSyncing(Context context) {
        Intent intent = new Intent (context, TimeService.class);
        intent.setAction(SettingsIntents.ACTION_EVALUATE_TIME_ZONE_SYNCING);
        context.startService(intent);
    }

    /**
     * Start the {@link TimeDebugService} which will dump information about time and timezone
     * syncing on the device. Only for userdebug and eng builds.
     */
    private void startTimeDebugService(Context context) {
        if ("userdebug".equals(Build.TYPE) || "eng".equals(Build.TYPE)) {
            Intent intent = new Intent(context, TimeDebugService.class);
            context.startService(intent);
        }
    }

    /**
     * Start the {@link VolumeUIService} which will listen to volume changes and handle them by
     * displaying volume UI.
     */
    private void startVolumeUIService(Context context) {
        Intent intent = new Intent(context, VolumeUIService.class);
        context.startService(intent);
    }

    private void adjustSysStorageThresholdMaxBytes(Context context) {
        Settings.Global.putLong(context.getContentResolver(),
                Settings.Global.SYS_STORAGE_THRESHOLD_MAX_BYTES,
                GKeys.SYS_STORAGE_THRESHOLD_MAX_MEGABYTES_OVERRIDE.get() * MB_IN_BYTES);
    }

    /**
     * Sets up ADB for wireless debugging if ADB is enabled.
     */
    private void configureWirelessDebugging(final Context context) {
        Executors.INSTANCE.get(context).getBackgroundExecutor().submit(
                new AbstractCwRunnable("configureWirelessDebugging") {
                    @Override
                    public void run() {
                        if (Log.isLoggable(TAG, Log.VERBOSE)) {
                            Log.v(TAG, "Configuring wireless debugging");
                        }
                        AdbUtil.onBoot(context);
                    }
                });
    }

    /**
     * Sets the parameters for SimpleTimerWifiBackoff in WifiMediator.
     */
    private void configureWifiBackoff(Context context) {
        Settings.System.putLong(context.getContentResolver(),
                WIFI_BACKOFF_DELAY_KEY,
                GKeys.WIFI_BACKOFF_DELAY_MS.get());
        Settings.System.putLong(context.getContentResolver(),
                WIFI_BACKOFF_DURATION_KEY,
                GKeys.WIFI_BACKOFF_DURATION_MS.get());
    }

    /**
     * Resets accelerometer_rotation to the off state on every boot.
     */
    private void disableAccelerometerRotation(Context context) {
        Settings.System.putInt(context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION,
                0);
    }

    /**
     * Resets theater mode to the off state on every boot.
     */
    private void disableTheaterMode(Context context) {
        Settings.Global.putInt(context.getContentResolver(), Settings.Global.THEATER_MODE_ON, 0);
    }

    private void ensureScreenTimeout(Context context) {
        // Ensures that the screen timeout setting is what we expect.
        // We need to do this because factory ROM was set to 10s and now we want it higher by default.
        Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT,
                context.getResources().getInteger(R.integer.screen_off_override));
    }

    private void ensureSetupComplete(Context context) {
        ContentResolver resolver = context.getContentResolver();
        if (Settings.System.getInt(resolver,
                Settings.System.SETUP_WIZARD_HAS_RUN, 0) == 1) {
            // We must set the global key here in addition to in setup to cover the upgrade case.
            if (!CapabilitiesConfig.getInstance(context).isButtonSet()) {
                context.sendBroadcast(new Intent(SettingsIntents.ACTION_SET_GLOBAL_KEY));
            }

            // LMP MR1 requires USER_SETUP_COMPLETE to be set to 1 after setup.
            if (Settings.Secure.getInt(resolver, Settings.Secure.USER_SETUP_COMPLETE, 0) != 1) {
                Settings.Secure.putInt(resolver, Settings.Secure.USER_SETUP_COMPLETE, 1);
            }
        }
    }

    @VisibleForTesting
    void avoidCaptivePortals(Context context) {
        Settings.Global.putInt(context.getContentResolver(),
                Settings.Global.CAPTIVE_PORTAL_MODE,
                Settings.Global.CAPTIVE_PORTAL_MODE_AVOID);

        if (FeatureManager.INSTANCE.get(context).isLocalEditionDevice()) {
            // For LE devices, we need to set different URLs for connectivity checks that are
            // accessible in China.
            Settings.Global.putString(context.getContentResolver(),
                    Settings.Global.CAPTIVE_PORTAL_HTTPS_URL,
                    LE_SAFE_CONNECTIVITY_CHECK_HTTPS_URL);
            Settings.Global.putString(context.getContentResolver(),
                    Settings.Global.CAPTIVE_PORTAL_HTTP_URL,
                    LE_SAFE_CONNECTIVITY_CHECK_HTTP_URL);
            Settings.Global.putString(context.getContentResolver(),
                    Settings.Global.CAPTIVE_PORTAL_FALLBACK_URL,
                    LE_SAFE_CONNECTIVITY_CHECK_HTTP_URL);
        }
    }

    @VisibleForTesting
    void updateNtpServer(Context context) {
        if (FeatureManager.INSTANCE.get(context).isLocalEditionDevice()) {
            // On LE change NTP server as the default one is not accessible in China.
            Settings.Global.putString(context.getContentResolver(),
                    Settings.Global.NTP_SERVER,
                    LE_NTP_SERVER);
        }
    }

    @VisibleForTesting
    void updateDnsOverTls(Context context) {
      if (FeatureManager.INSTANCE.get(context).isLocalEditionDevice()) {
          // DNS over TLS doesn't have much large adaption at this moment in China and
          // we cannot rely on Google public DNS there. However, on Android P, DNS over
          // TLS will be default to opportunistic mode. That means, when connecting
          // via BT proxy, DNS will try to go through TLS to the primary China server
          // then fallback to the two Google servers. However, our China primary DNS
          // doesn't support TLS yet, and thus only two Google DNS servers are taking
          // effect which is less ideal situation for us in China. Therefore, we revert
          // private dns mode to OFF. See more details in b/124830299.
          Settings.Global.putString(context.getContentResolver(),
                  Settings.Global.PRIVATE_DNS_DEFAULT_MODE,
                  ConnectivityManager.PRIVATE_DNS_MODE_OFF);
      }
    }

    /**
     * The Android built-in WiFi network available notification is not useful on Wear.
     */
    private void disableWifiNetworksAvailableNotification(Context context) {
        Settings.Global.putInt(context.getContentResolver(),
            Settings.Global.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON, 0);
    }

    /**
     * Ensures that font sizes lie within values specified for the device.
     *
     * This is a simple check that clamps the font size to the available sizes. This
     * is used during OTAs where we have the potential to change values and want to
     * ensure that users get the correct values.
     */
    private void ensureTextSizeWithinRange(Context context) {
        String[] textSizeEntries = context.getResources().getStringArray(R.array.text_size_entries);
        float minFontScale = Float.MAX_VALUE;
        float maxFontScale = Float.MIN_VALUE;
        float allowedScale;
        for (String scaleString : textSizeEntries) {
            allowedScale = Float.parseFloat(scaleString);
            minFontScale = Math.min(minFontScale, allowedScale);
            maxFontScale = Math.max(maxFontScale, allowedScale);
        }

        try {
            final Configuration config = new Configuration();
            config.updateFrom(ActivityManager.getService().getConfiguration());
            float clampedScale = Math.max(minFontScale, Math.min(config.fontScale, maxFontScale));
            if (config.fontScale != clampedScale) {
                config.fontScale = clampedScale;
                ActivityManager.getService().updatePersistentConfiguration(config);
            }
        } catch (RemoteException e) {
            // Ignore
        }
    }

    private void checkAutoWifiOverrideSetting(Context context) {
        if (FeatureManager.INSTANCE.get(context).isAutoOverrideEnabled()) {
            Log.i(TAG, "AutoWifi override is enabled; disabling Automatic Wifi Toggling.");
            final boolean previouslyEnabled = WifiAutoModeUtil.getAutoWifiSetting(context);
            WifiAutoModeUtil.setAutoWifiSetting(context, false);

            // b/27453334 this ensures that the first time we boot up, we ensure that wifi
            // remains on after enabling auto wifi override; we don't want to do this
            // unconditionally, because in the steady state the user should be able to manually
            // keep wifi on or off and persist that through reboots
            if (previouslyEnabled) {
                Log.i(TAG, "AutoWifi override enforced for the first time; enabling wifi.");
                ((WifiManager) context.getSystemService(Context.WIFI_SERVICE)).setWifiEnabled(true);
            }
        }
    }

    /**
     * For cellular devices, we don't want wifi power save at all. We do this for now
     * by setting the timer to be really, really high.  see b/27385477
     */
    private void overridePowerSaveForCellDevices(Context context) {
        if (com.google.android.clockwork.phone.Utils.isCurrentDeviceCellCapable(context)) {
            int veryHighNumberMinutes = 360000; // 6000 hours
            Log.i(TAG, "Cellular device detected; setting wifi power save timer to: "
                    + veryHighNumberMinutes + " minutes");
            ContentValues values = new ContentValues();
            values.put(SettingsContract.KEY_WIFI_POWER_SAVE, veryHighNumberMinutes);
            context.getContentResolver().update(SettingsContract.WIFI_POWER_SAVE_URI,
                    values, null, null);
        }
    }

    private void initializePayOnStem(Context context) {
        // Tell Settings to update its value for "Pay on stem".
        ButtonManager bm = new ButtonManager(context);
        context.startService(SettingsIntents.getPayOnStemIntent(bm.isPayConfiguredOnStem()));
    }

    private void initializeDozeEnabledSetting(Context context) {
        AmbientConfig ambientConfig = DefaultAmbientConfig.getInstance(context);
        Settings.Secure.putInt(context.getContentResolver(), Settings.Secure.DOZE_ENABLED,
                ambientConfig.isAmbientEnabled() ? 1 : 0);
    }

    private void disableChargingSounds(Context context) {
        if(context.getResources().getBoolean(R.bool.config_disableChargingSounds)) {
            Settings.Global.putInt(context.getContentResolver(),
                    Settings.Global.CHARGING_SOUNDS_ENABLED, 0);
        }
    }

    /**
     * Ensures only one pre-integrated voice provider is enabled on LE device.
     *
     * Available providers are taken from list in resources with pre-integrated providers. The
     * choice which provider will be enabled depends on the order of providers on the list. The
     * first enabled provider will be kept enabled and other will be disabled. It is based on the
     * "Voice Provider in China" design (go/cw-voice-providers-in-china)
     */
    @VisibleForTesting
    void configureDefaultVoiceProvider(Context context) {
        if (!FeatureManager.INSTANCE.get(context).isLocalEditionDevice()) {
            return;
        }
        PackageManager pm = context.getPackageManager();
        String[] voiceProviders =
                context.getResources()
                        .getStringArray(R.array.config_le_system_voice_assistant_packages);
        if (voiceProviders.length == 0) {
            // No providers are installed or the voice providers settings is disabled.
            Log.e(TAG, "No pre-integrated voice provider available.");
            return;
        }
        String defaultProvider = null;
        for (String voiceProvider : voiceProviders) {
            try {
                boolean isProviderEnabled = pm.getApplicationInfo(voiceProvider, 0).enabled;
                if (isProviderEnabled) {
                    if (defaultProvider == null) {
                        Log.i(TAG, "Default voice provider is set to: " + voiceProvider);
                        defaultProvider = voiceProvider;
                    } else {
                        Log.i(TAG, "Multiple voice provers enabled, disabling: " + voiceProvider);
                        pm.setApplicationEnabledSetting(
                                voiceProvider, pm.COMPONENT_ENABLED_STATE_DISABLED, 0);
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Pre-integrated voice provider '" + voiceProvider + "' not found.", e);
            }
        }
        // If no provider is enabled, enable the first one.
        if (defaultProvider == null) {
            pm.setApplicationEnabledSetting(
                    voiceProviders[0], pm.COMPONENT_ENABLED_STATE_ENABLED, 0);
        }
    }

    /**
     * Ensure that the location settings are correctly set on boot.
     *
     * As outlined in b/110043206, a change in how the Settings.Secure.LOCATION_PROVIDERS_ALLOWED
     * values are interpreted has allowed for the situation where location settings could be
     * enabled but then after an OTA they would be disabled.
     * When the device boots we want to preserve the previous user specified location settings. This
     * is done by checking the value of Settings.Secure.LOCATION_PROVIDERS_ALLOWED. If this is empty
     * then location was disabled, otherwise, it was previously enabled.
     * In the case that location settings were previously enabled we use updateLocationEnabled to
     * ensure that Settings.Secure.LOCATION_PROVIDERS_ALLOWED is populated with the correct
     * values during boot.
     */
    void ensureLocationSettingsEnabled(Context context) {
      final String providers = Settings.Secure.getString(
              context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
      if (providers == null || providers.isEmpty()) {
        return;
      }
      Utils.updateLocationEnabled(context, true, context.getContentResolver().getUserId(),
          Settings.Secure.LOCATION_CHANGER_SYSTEM_SETTINGS);
    }
}
