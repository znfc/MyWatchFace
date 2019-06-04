package com.google.android.clockwork.settings.common;

import android.os.Build;
import android.support.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.protos.wireless.android.clockwork.apps.logs.CwEnums.CwSettingsUiEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Loggable settings preference constants
 */
public final class SettingsPreferenceLogConstants {
    private static final Map<String, CwSettingsUiEvent> sPrefToEventMap;
    public static final String IGNORE_SUBSTRING = "ignore_";
    public static final String CTS_VERIFIER_DUMMY_SERVICE = "Dummy accessibility service";

    @VisibleForTesting
    static boolean sUserDebugOrEngBuild;

    static {
        Map<String, CwSettingsUiEvent> tmpMap = new HashMap();
        tmpMap.put("pref_about", CwSettingsUiEvent.SETTING_SELECTED_ABOUT);
        tmpMap.put("pref_accessibility_largeText",
                CwSettingsUiEvent.SETTING_SELECTED_ACCESSIBILITY_LARGETEXT);
        tmpMap.put("pref_accessibility_colorInversion",
                CwSettingsUiEvent.SETTING_SELECTED_ACCESSIBILITY_COLORINVERSION);
        tmpMap.put("pref_accessibility_magnification",
                CwSettingsUiEvent.SETTING_SELECTED_ACCESSIBILITY_MAGNIFICATION);
        tmpMap.put("pref_accessibility_sideButton",
                CwSettingsUiEvent.SETTING_SELECTED_ACCESSIBILITY_SIDEBUTTON);
        tmpMap.put("pref_accessibility_tts",
                CwSettingsUiEvent.SETTING_SELECTED_ACCESSIBILITY_TTS);
        tmpMap.put("pref_accessibility_tts_engine_selected", CwSettingsUiEvent.SETTING_SELECTED_ACCESSIBILITY_TTS_ENGINE);
        tmpMap.put("pref_accessibility_tts_engine_default",
                CwSettingsUiEvent.SETTING_SELECTED_ACCESSIBILITY_TTS_ENGINE_DEFAULT);
        tmpMap.put("pref_accessibility_tts_engine_language",
                CwSettingsUiEvent.SETTING_SELECTED_ACCESSIBILITY_TTS_ENGINE_LANGUAGE);
        tmpMap.put("pref_accessibility_tts_engine_listenToSample",
                CwSettingsUiEvent.SETTING_SELECTED_ACCESSIBILITY_TTS_ENGINE_LISTENTOSAMPLE);
        tmpMap.put("pref_accessibility_tts_rates",
                CwSettingsUiEvent.SETTING_SELECTED_ACCESSIBILITY_TTS_RATES);
        tmpMap.put("pref_accessPointNames",
                CwSettingsUiEvent.SETTING_SELECTED_ACCESSPOINTNAMES);
        tmpMap.put("pref_accounts", CwSettingsUiEvent.SETTING_SELECTED_ACCOUNTS);
        tmpMap.put("pref_active_device_admins",
                CwSettingsUiEvent.SETTING_SELECTED_ACTIVE_DEVICE_ADMINS);
        tmpMap.put("pref_adbDebugging",
                CwSettingsUiEvent.SETTING_SELECTED_ADBDEBUGGING);
        tmpMap.put("pref_addAccount",
                CwSettingsUiEvent.SETTING_SELECTED_ADDACCOUNT);
        tmpMap.put("pref_advanced",
                CwSettingsUiEvent.SETTING_SELECTED_ADVANCED);
        tmpMap.put("pref_advancedPermissions_drawOverlay",
                CwSettingsUiEvent.SETTING_SELECTED_ADVANCEDPERMISSIONS_DRAWOVERLAY);
        tmpMap.put("pref_advancedPermissions_writeSettings",
                CwSettingsUiEvent.SETTING_SELECTED_ADVANCEDPERMISSIONS_WRITESETTINGS);
        tmpMap.put("pref_advancedSettings",
                CwSettingsUiEvent.SETTING_SELECTED_ADVANCEDSETTINGS);
        tmpMap.put("pref_airplaneMode",
                CwSettingsUiEvent.SETTING_SELECTED_AIRPLANEMODE);
        tmpMap.put("pref_alarmVolume",
                CwSettingsUiEvent.SETTING_SELECTED_ALARMVOLUME);
        tmpMap.put("pref_alertReminder",
                CwSettingsUiEvent.SETTING_SELECTED_ALERTREMINDER);
        tmpMap.put("pref_alertSoundDuration",
                CwSettingsUiEvent.SETTING_SELECTED_ALERTSOUNDDURATION);
        tmpMap.put("pref_alertVibrate",
                CwSettingsUiEvent.SETTING_SELECTED_ALERTVIBRATE);
        tmpMap.put("pref_allowMockLocations",
                CwSettingsUiEvent.SETTING_SELECTED_ALLOWMOCKLOCATIONS);
        tmpMap.put("pref_alwaysOnScreen",
                CwSettingsUiEvent.SETTING_SELECTED_ALWAYSONSCREEN);
        tmpMap.put("pref_amberAlerts",
                CwSettingsUiEvent.SETTING_SELECTED_AMBERALERTS);
        tmpMap.put("pref_android_device_manager_settings",
                CwSettingsUiEvent.SETTING_SELECTED_ANDROID_DEVICE_MANAGER_SETTINGS);
        tmpMap.put("pref_autoDateTime",
                CwSettingsUiEvent.SETTING_SELECTED_AUTODATETIME);
        tmpMap.put("pref_autoTimeZone",
                CwSettingsUiEvent.SETTING_SELECTED_AUTOTIMEZONE);
        tmpMap.put("pref_available_networks_group",
                CwSettingsUiEvent.SETTING_SELECTED_AVAILABLE_NETWORKS_GROUP);
        tmpMap.put("pref_batteryInfo",
                CwSettingsUiEvent.SETTING_SELECTED_BATTERYINFO);
        tmpMap.put("pref_bluetooth",
                CwSettingsUiEvent.SETTING_SELECTED_BLUETOOTH);
        tmpMap.put("pref_bluetoothAvailable",
                CwSettingsUiEvent.SETTING_SELECTED_BLUETOOTHAVAILABLE);
        tmpMap.put("pref_bluetoothEnabled",
                CwSettingsUiEvent.SETTING_SELECTED_BLUETOOTHENABLED);
        tmpMap.put("pref_bluetoothHfp",
                CwSettingsUiEvent.SETTING_SELECTED_BLUETOOTHHFP);
        tmpMap.put("pref_bluetoothScan",
                CwSettingsUiEvent.SETTING_SELECTED_BLUETOOTHSCAN);
        tmpMap.put("pref_btSnoopLog",
                CwSettingsUiEvent.SETTING_SELECTED_BTSNOOPLOG);
        tmpMap.put("pref_bugReport",
                CwSettingsUiEvent.SETTING_SELECTED_BUGREPORT);
        tmpMap.put("pref_build",
                CwSettingsUiEvent.SETTING_SELECTED_BUILD);
        tmpMap.put("pref_buttons",
                CwSettingsUiEvent.SETTING_SELECTED_BUTTONS);
        tmpMap.put("pref_cache",
                CwSettingsUiEvent.SETTING_SELECTED_CACHE);
        tmpMap.put("pref_call",
                CwSettingsUiEvent.SETTING_SELECTED_CALL);
        tmpMap.put("pref_callForwarding",
                CwSettingsUiEvent.SETTING_SELECTED_CALLFORWARDING);
        tmpMap.put("pref_callForwardingIos",
                CwSettingsUiEvent.SETTING_SELECTED_CALLFORWARDINGIOS);
        tmpMap.put("pref_cardPreviews",
                CwSettingsUiEvent.SETTING_SELECTED_CARDPREVIEWS);
        tmpMap.put("pref_cardPreviews_high",
                CwSettingsUiEvent.SETTING_SELECTED_CARDPREVIEWS_HIGH);
        tmpMap.put("pref_cardPreviews_low",
                CwSettingsUiEvent.SETTING_SELECTED_CARDPREVIEWS_LOW);
        tmpMap.put("pref_cardPreviews_normal",
                CwSettingsUiEvent.SETTING_SELECTED_CARDPREVIEWS_NORMAL);
        tmpMap.put("pref_cellular",
                CwSettingsUiEvent.SETTING_SELECTED_CELLULAR);
        tmpMap.put("pref_cellularBatterySaver",
                CwSettingsUiEvent.SETTING_SELECTED_CELLULARBATTERYSAVER);
        tmpMap.put("pref_cellularToggle",
                CwSettingsUiEvent.SETTING_SELECTED_CELLULARTOGGLE);
        tmpMap.put("pref_cellularToggleV2",
                CwSettingsUiEvent.SETTING_SELECTED_CELLULARTOGGLEV2);
        tmpMap.put("pref_clearAdbKeys",
                CwSettingsUiEvent.SETTING_SELECTED_CLEARADBKEYS);
        tmpMap.put("pref_clear_cache",
                CwSettingsUiEvent.SETTING_SELECTED_CLEAR_CACHE);
        tmpMap.put("pref_clear_data",
                CwSettingsUiEvent.SETTING_SELECTED_CLEAR_DATA);
        tmpMap.put("pref_cmasTestAlerts",
                CwSettingsUiEvent.SETTING_SELECTED_CMASTESTALERTS);
        tmpMap.put("pref_connectionStatus",
                CwSettingsUiEvent.SETTING_SELECTED_CONNECTIONSTATUS);
        tmpMap.put("pref_connectivityVibrate",
                CwSettingsUiEvent.SETTING_SELECTED_CONNECTIVITYVIBRATE);
        tmpMap.put("pref_data",
                CwSettingsUiEvent.SETTING_SELECTED_DATA);
        tmpMap.put("pref_dataConnectivityEnable",
                CwSettingsUiEvent.SETTING_SELECTED_DATACONNECTIVITYENABLE);
        tmpMap.put("pref_dataRoaming",
                CwSettingsUiEvent.SETTING_SELECTED_DATAROAMING);
        tmpMap.put("pref_dataUsage",
                CwSettingsUiEvent.SETTING_SELECTED_DATAUSAGE);
        tmpMap.put("pref_dataUsageAppUsage",
                CwSettingsUiEvent.SETTING_SELECTED_DATAUSAGEAPPUSAGE);
        tmpMap.put("pref_dataUsageCycleDay",
                CwSettingsUiEvent.SETTING_SELECTED_DATAUSAGECYCLEDAY);
        tmpMap.put("pref_dataUsageLimitEnable",
                CwSettingsUiEvent.SETTING_SELECTED_DATAUSAGELIMITENABLE);
        tmpMap.put("pref_dataUsageLimitValue",
                CwSettingsUiEvent.SETTING_SELECTED_DATAUSAGELIMITVALUE);
        tmpMap.put("pref_dataUsageWarningLevel",
                CwSettingsUiEvent.SETTING_SELECTED_DATAUSAGEWARNINGLEVEL);
        tmpMap.put("pref_dateTime",
                CwSettingsUiEvent.SETTING_SELECTED_DATETIME);
        tmpMap.put("pref_debugLayout",
                CwSettingsUiEvent.SETTING_SELECTED_DEBUGLAYOUT);
        tmpMap.put("pref_debugOverBluetooth",
                CwSettingsUiEvent.SETTING_SELECTED_DEBUGOVERBLUETOOTH);
        tmpMap.put("pref_debugOverdraw",
                CwSettingsUiEvent.SETTING_SELECTED_DEBUGOVERDRAW);
        tmpMap.put("pref_debugOverWifi",
                CwSettingsUiEvent.SETTING_SELECTED_DEBUGOVERWIFI);
        tmpMap.put("pref_debugTiming",
                CwSettingsUiEvent.SETTING_SELECTED_DEBUGTIMING);
        tmpMap.put("pref_defaultSms",
                CwSettingsUiEvent.SETTING_SELECTED_DEFAULTSMS);
        tmpMap.put("pref_deviceAdministration",
                CwSettingsUiEvent.SETTING_SELECTED_DEVICEADMINISTRATION);
        tmpMap.put("pref_deviceName",
                CwSettingsUiEvent.SETTING_SELECTED_DEVICENAME);
        tmpMap.put("pref_dndOptions",
                CwSettingsUiEvent.SETTING_SELECTED_DNDOPTIONS);
        tmpMap.put("pref_dndOptions_alarms",
                CwSettingsUiEvent.SETTING_SELECTED_DNDOPTIONS_ALARMS);
        tmpMap.put("pref_dndOptions_calls",
                CwSettingsUiEvent.SETTING_SELECTED_DNDOPTIONS_CALLS);
        tmpMap.put("pref_dndOptions_events",
                CwSettingsUiEvent.SETTING_SELECTED_DNDOPTIONS_EVENTS);
        tmpMap.put("pref_dndOptions_reminders",
                CwSettingsUiEvent.SETTING_SELECTED_DNDOPTIONS_REMINDERS);
        tmpMap.put("pref_dpiSettings",
                CwSettingsUiEvent.SETTING_SELECTED_DPISETTINGS);
        tmpMap.put("pref_emergency_dialer",
                CwSettingsUiEvent.SETTING_SELECTED_EMERGENCY_DIALER);
        tmpMap.put("pref_emergencyNotifications",
                CwSettingsUiEvent.SETTING_SELECTED_EMERGENCYNOTIFICATIONS);
        tmpMap.put("pref_enableWifiWhenCharging",
                CwSettingsUiEvent.SETTING_SELECTED_ENABLEWIFIWHENCHARGING);
        tmpMap.put("pref_etwsTestAlerts",
                CwSettingsUiEvent.SETTING_SELECTED_ETWSTESTALERTS);
        tmpMap.put("pref_exerciseDetection",
                CwSettingsUiEvent.SETTING_SELECTED_EXERCISEDETECTION);
        tmpMap.put("pref_exerciseDetection_biking",
                CwSettingsUiEvent.SETTING_SELECTED_EXERCISEDETECTION_BIKING);
        tmpMap.put("pref_exerciseDetection_running",
                CwSettingsUiEvent.SETTING_SELECTED_EXERCISEDETECTION_RUNNING);
        tmpMap.put("pref_exerciseDetection_walking",
                CwSettingsUiEvent.SETTING_SELECTED_EXERCISEDETECTION_WALKING);
        tmpMap.put("pref_extremeThreats",
                CwSettingsUiEvent.SETTING_SELECTED_EXTREMETHREATS);
        tmpMap.put("pref_factoryReset",
                CwSettingsUiEvent.SETTING_SELECTED_FACTORYRESET);
        tmpMap.put("pref_forceLocationOn",
                CwSettingsUiEvent.SETTING_SELECTED_FORCELOCATIONON);
        tmpMap.put("pref_force_stop",
                CwSettingsUiEvent.SETTING_SELECTED_FORCE_STOP);
        tmpMap.put("pref_help",
                CwSettingsUiEvent.SETTING_SELECTED_HELP);
        tmpMap.put("pref_hotwordDetection",
                CwSettingsUiEvent.SETTING_SELECTED_HOTWORDDETECTION);
        tmpMap.put("pref_hourFormat",
                CwSettingsUiEvent.SETTING_SELECTED_HOURFORMAT);
        tmpMap.put("pref_imei",
                CwSettingsUiEvent.SETTING_SELECTED_IMEI);
        tmpMap.put("pref_inactive_device_admins",
                CwSettingsUiEvent.SETTING_SELECTED_INACTIVE_DEVICE_ADMINS);
        tmpMap.put("pref_inputMethods",
                CwSettingsUiEvent.SETTING_SELECTED_INPUTMETHODS);
        tmpMap.put("pref_launchTutorial",
                CwSettingsUiEvent.SETTING_SELECTED_LAUNCHTUTORIAL);
        tmpMap.put("pref_legalNotices",
                CwSettingsUiEvent.SETTING_SELECTED_LEGALNOTICES);
        tmpMap.put("pref_location",
                CwSettingsUiEvent.SETTING_SELECTED_LOCATION);
        tmpMap.put("pref_location_mode",
                CwSettingsUiEvent.SETTING_SELECTED_LOCATION_MODE);
        tmpMap.put("pref_location_mode_batterySaving",
                CwSettingsUiEvent.SETTING_SELECTED_LOCATION_MODE_BATTERYSAVING);
        tmpMap.put("pref_location_mode_highAccuracy",
                CwSettingsUiEvent.SETTING_SELECTED_LOCATION_MODE_HIGHACCURACY);
        tmpMap.put("pref_location_mode_sensorsOnly",
                CwSettingsUiEvent.SETTING_SELECTED_LOCATION_MODE_SENSORSONLY);
        tmpMap.put("pref_location_toggle",
                CwSettingsUiEvent.SETTING_SELECTED_LOCATION_TOGGLE);
        tmpMap.put("pref_locationToggle",
                CwSettingsUiEvent.SETTING_SELECTED_LOCATIONTOGGLE);
        tmpMap.put("pref_manualDate",
                CwSettingsUiEvent.SETTING_SELECTED_MANUALDATE);
        tmpMap.put("pref_manualTime",
                CwSettingsUiEvent.SETTING_SELECTED_MANUALTIME);
        tmpMap.put("pref_manualTimeZone",
                CwSettingsUiEvent.SETTING_SELECTED_MANUALTIMEZONE);
        tmpMap.put("pref_mediaVolume",
                CwSettingsUiEvent.SETTING_SELECTED_MEDIAVOLUME);
        tmpMap.put("pref_accessibilityVolume",
                CwSettingsUiEvent.SETTING_SELECTED_ACCESSIBILITYVOLUME);
        tmpMap.put("pref_model",
                CwSettingsUiEvent.SETTING_SELECTED_MODEL);
        tmpMap.put("pref_moreTips",
                CwSettingsUiEvent.SETTING_SELECTED_MORETIPS);
        tmpMap.put("pref_msn",
                CwSettingsUiEvent.SETTING_SELECTED_MSN);
        tmpMap.put("pref_networkOperators",
                CwSettingsUiEvent.SETTING_SELECTED_NETWORKOPERATORS);
        tmpMap.put("pref_nfc",
                CwSettingsUiEvent.SETTING_SELECTED_NFC);
        tmpMap.put("pref_none",
                CwSettingsUiEvent.SETTING_SELECTED_NONE);
        tmpMap.put("pref_password",
                CwSettingsUiEvent.SETTING_SELECTED_PASSWORD);
        tmpMap.put("pref_pattern",
                CwSettingsUiEvent.SETTING_SELECTED_PATTERN);
        tmpMap.put("pref_permissions",
                CwSettingsUiEvent.SETTING_SELECTED_PERMISSIONS);
        tmpMap.put("pref_phoneNumber",
                CwSettingsUiEvent.SETTING_SELECTED_PHONENUMBER);
        tmpMap.put("pref_pin",
                CwSettingsUiEvent.SETTING_SELECTED_PIN);
        tmpMap.put("pref_pointerLocation",
                CwSettingsUiEvent.SETTING_SELECTED_POINTERLOCATION);
        tmpMap.put("pref_powerOptimizations",
                CwSettingsUiEvent.SETTING_SELECTED_POWEROPTIMIZATIONS);
        // TODO: Change to CwSettingsUiEvent.SETTING_SELECTED_CELLULAR_ENHANCED4GLTE after lib drop.
        tmpMap.put("pref_enhanced_4g_lte",
                CwSettingsUiEvent.UNKNOWN);
        tmpMap.put("pref_preferredNetwork",
                CwSettingsUiEvent.SETTING_SELECTED_PREFERREDNETWORK);
        tmpMap.put("pref_regulatoryInfo",
                CwSettingsUiEvent.SETTING_SELECTED_REGULATORYINFO);
        tmpMap.put("pref_removeAccount",
                CwSettingsUiEvent.SETTING_SELECTED_REMOVEACCOUNT);
        tmpMap.put("pref_restart",
                CwSettingsUiEvent.SETTING_SELECTED_RESTART);
        tmpMap.put("pref_resumeData",
                CwSettingsUiEvent.SETTING_SELECTED_RESUMEDATA);
        tmpMap.put("pref_ringVolume",
                CwSettingsUiEvent.SETTING_SELECTED_RINGVOLUME);
        tmpMap.put("pref_secureAdbCancel",
                CwSettingsUiEvent.SETTING_SELECTED_SECUREADBCANCEL);
        tmpMap.put("pref_secureAdbFingerprint",
                CwSettingsUiEvent.SETTING_SELECTED_SECUREADBFINGERPRINT);
        tmpMap.put("pref_secureAdbOk",
                CwSettingsUiEvent.SETTING_SELECTED_SECUREADBOK);
        tmpMap.put("pref_secureAdbWhitelist",
                CwSettingsUiEvent.SETTING_SELECTED_SECUREADBWHITELIST);
        tmpMap.put("pref_serial",
                CwSettingsUiEvent.SETTING_SELECTED_SERIAL);
        tmpMap.put("pref_severeThreats",
                CwSettingsUiEvent.SETTING_SELECTED_SEVERETHREATS);
        tmpMap.put("pref_showTouches",
                CwSettingsUiEvent.SETTING_SELECTED_SHOWTOUCHES);
        tmpMap.put("pref_simStatus",
                CwSettingsUiEvent.SETTING_SELECTED_SIMSTATUS);
        tmpMap.put("pref_simStatusIccid",
                CwSettingsUiEvent.SETTING_SELECTED_SIMSTATUSICCID);
        tmpMap.put("pref_simStatusImei",
                CwSettingsUiEvent.SETTING_SELECTED_SIMSTATUSIMEI);
        tmpMap.put("pref_simStatusImeiSv",
                CwSettingsUiEvent.SETTING_SELECTED_SIMSTATUSIMEISV);
        tmpMap.put("pref_simStatusNetwork",
                CwSettingsUiEvent.SETTING_SELECTED_SIMSTATUSNETWORK);
        tmpMap.put("pref_simStatusNetworkState",
                CwSettingsUiEvent.SETTING_SELECTED_SIMSTATUSNETWORKSTATE);
        tmpMap.put("pref_simStatusNetworkType",
                CwSettingsUiEvent.SETTING_SELECTED_SIMSTATUSNETWORKTYPE);
        tmpMap.put("pref_simStatusPhoneNumber",
                CwSettingsUiEvent.SETTING_SELECTED_SIMSTATUSPHONENUMBER);
        tmpMap.put("pref_simStatusRoamingState",
                CwSettingsUiEvent.SETTING_SELECTED_SIMSTATUSROAMINGSTATE);
        tmpMap.put("pref_simStatusServiceState",
                CwSettingsUiEvent.SETTING_SELECTED_SIMSTATUSSERVICESTATE);
        tmpMap.put("pref_simStatusSignalStrength",
                CwSettingsUiEvent.SETTING_SELECTED_SIMSTATUSSIGNALSTRENGTH);
        tmpMap.put("pref_smartIlluminate",
                CwSettingsUiEvent.SETTING_SELECTED_SMARTILLUMINATE);
        tmpMap.put("pref_smartReply",
                CwSettingsUiEvent.SETTING_SELECTED_SMARTREPLY);
        tmpMap.put("pref_soundNotification",
                CwSettingsUiEvent.SETTING_SELECTED_SOUNDNOTIFICATION);
        tmpMap.put("pref_stayOnWhilePluggedIn",
                CwSettingsUiEvent.SETTING_SELECTED_STAYONWHILEPLUGGEDIN);
        tmpMap.put("pref_storage",
                CwSettingsUiEvent.SETTING_SELECTED_STORAGE);
        tmpMap.put("pref_systemUpdate",
                CwSettingsUiEvent.SETTING_SELECTED_SYSTEMUPDATE);
        tmpMap.put("pref_tap_and_pay",
                CwSettingsUiEvent.SETTING_SELECTED_TAP_AND_PAY);
        tmpMap.put("pref_text",
                CwSettingsUiEvent.SETTING_SELECTED_TEXT);
        tmpMap.put("pref_tiltToWake",
                CwSettingsUiEvent.SETTING_SELECTED_TILTTOWAKE);
        tmpMap.put("pref_touchToWake",
                CwSettingsUiEvent.SETTING_SELECTED_TOUCHTOWAKE);
        tmpMap.put("pref_turnOff",
                CwSettingsUiEvent.SETTING_SELECTED_TURNOFF);
        tmpMap.put("pref_turnOn",
                CwSettingsUiEvent.SETTING_SELECTED_TURNON);
        tmpMap.put("pref_twinning",
                CwSettingsUiEvent.SETTING_SELECTED_TWINNING);
        tmpMap.put("pref_uninstall",
                CwSettingsUiEvent.SETTING_SELECTED_UNINSTALL);
        tmpMap.put("pref_unlockSim",
                CwSettingsUiEvent.SETTING_SELECTED_UNLOCKSIM);
        tmpMap.put("pref_version",
                CwSettingsUiEvent.SETTING_SELECTED_VERSION);
        tmpMap.put("pref_vibrateForCalls",
                CwSettingsUiEvent.SETTING_SELECTED_VIBRATEFORCALLS);
        tmpMap.put("pref_muteWhenOffBody",
                CwSettingsUiEvent.SETTING_SELECTED_MUTEWHENOFFBODY);
        tmpMap.put("pref_vibrationLevel",
                CwSettingsUiEvent.SETTING_SELECTED_VIBRATIONLEVEL);
        tmpMap.put("pref_vibrationLevel_normal", CwSettingsUiEvent.UNKNOWN);
        tmpMap.put("pref_vibrationLevel_long", CwSettingsUiEvent.UNKNOWN);
        tmpMap.put("pref_vibrationLevel_double", CwSettingsUiEvent.UNKNOWN);
        tmpMap.put("pref_voiceAssistant",
                CwSettingsUiEvent.SETTING_SELECTED_VOICE_ASSISTANT);
        tmpMap.put("pref_voicemailNumber",
                CwSettingsUiEvent.SETTING_SELECTED_VOICEMAILNUMBER);
        tmpMap.put("pref_watchface",
                CwSettingsUiEvent.SETTING_SELECTED_WATCHFACE);
        tmpMap.put("pref_watchRingtone",
                CwSettingsUiEvent.SETTING_SELECTED_WATCHRINGTONE);
        tmpMap.put("pref_wearDeveloperOptions",
                CwSettingsUiEvent.SETTING_SELECTED_WEARDEVELOPEROPTIONS);
        tmpMap.put("pref_wifi",
                CwSettingsUiEvent.SETTING_SELECTED_WIFI);
        tmpMap.put("pref_wifi_about",
                CwSettingsUiEvent.SETTING_SELECTED_WIFI_ABOUT);
        tmpMap.put("pref_wifi_add_network",
                CwSettingsUiEvent.SETTING_SELECTED_WIFI_ADD_NETWORK);
        tmpMap.put("pref_wifi_current_network",
                CwSettingsUiEvent.SETTING_SELECTED_WIFI_CURRENT_NETWORK);
        tmpMap.put("pref_wifiForgetNetwork",
                CwSettingsUiEvent.SETTING_SELECTED_WIFI_FORGET_NETWORK);
        tmpMap.put("pref_wifiLogging",
                CwSettingsUiEvent.SETTING_SELECTED_WIFILOGGING);
        tmpMap.put("pref_wifi_old",
                CwSettingsUiEvent.SETTING_SELECTED_WIFI_OLD);
        tmpMap.put("pref_wifiOpenOnPhone",
                CwSettingsUiEvent.SETTING_SELECTED_WIFI_OPEN_ON_PHONE);
        tmpMap.put("pref_wifiOpenOnWatch",
                CwSettingsUiEvent.SETTING_SELECTED_WIFI_OPEN_ON_WATCH);
        tmpMap.put("pref_wifiRetry",
                CwSettingsUiEvent.SETTING_SELECTED_WIFI_RETRY);
        tmpMap.put("pref_wifi_saved_networks",
                CwSettingsUiEvent.SETTING_SELECTED_WIFI_SAVED_NETWORKS);
        tmpMap.put("pref_wifi_toggle",
                CwSettingsUiEvent.SETTING_SELECTED_WIFI_TOGGLE);
        tmpMap.put("pref_wifi_view_ip_address",
                CwSettingsUiEvent.SETTING_SELECTED_WIFI_VIEW_IP_ADDRESS);
        tmpMap.put("pref_wifi_view_mac_address",
                CwSettingsUiEvent.SETTING_SELECTED_WIFI_VIEW_MAC_ADDRESS);
        tmpMap.put("pref_wristGestures", CwSettingsUiEvent.SETTING_SELECTED_WRISTGESTURES);
        tmpMap.put("pref_yolo", CwSettingsUiEvent.SETTING_SELECTED_YOLO);

        tmpMap.put("animator_duration_scale",
                CwSettingsUiEvent.SETTING_SELECTED_ANIMATOR_DURATION_SCALE);
        tmpMap.put("pref_accessibility_service_selected", CwSettingsUiEvent.SETTING_SELECTED_ACCESSIBILITY_SERVICE);
        tmpMap.put("pref_brightness", CwSettingsUiEvent.SETTING_SELECTED_BRIGHTNESS);
        tmpMap.put("pref_developerOptions", CwSettingsUiEvent.SETTING_SELECTED_DEVELOPEROPTIONS);
        tmpMap.put("pref_fontSize", CwSettingsUiEvent.SETTING_SELECTED_FONTSIZE);
        tmpMap.put("pref_lockSimToggle", CwSettingsUiEvent.SETTING_SELECTED_LOCKSIMTOGGLE);
        tmpMap.put("pref_mainAccessibility", CwSettingsUiEvent.SETTING_SELECTED_MAINACCESSIBILITY);
        tmpMap.put("pref_mainAppStorage", CwSettingsUiEvent.SETTING_SELECTED_MAINAPPSTORAGE);
        tmpMap.put("pref_mainApps", CwSettingsUiEvent.SETTING_SELECTED_MAINAPPS);
        tmpMap.put("pref_mainConnectivity", CwSettingsUiEvent.SETTING_SELECTED_MAINCONNECTIVITY);
        tmpMap.put("pref_mainDisplay", CwSettingsUiEvent.SETTING_SELECTED_MAINDISPLAY);
        tmpMap.put("pref_mainGeneral", CwSettingsUiEvent.UNKNOWN);
        tmpMap.put("pref_mainGestures", CwSettingsUiEvent.SETTING_SELECTED_MAINGESTURES);
        tmpMap.put("pref_mainPersonal", CwSettingsUiEvent.SETTING_SELECTED_MAINPERSONAL);
        tmpMap.put("pref_mainSystem", CwSettingsUiEvent.SETTING_SELECTED_MAINSYSTEM);
        tmpMap.put("pref_powerOff", CwSettingsUiEvent.SETTING_SELECTED_POWEROFF);
        tmpMap.put("pref_prepairAccessibility",
                CwSettingsUiEvent.SETTING_SELECTED_PREPAIRACCESSIBILITY);
        tmpMap.put("pref_showChimeraModules",
                CwSettingsUiEvent.SETTING_SELECTED_SHOWCHIMERAMODULES);
        tmpMap.put("pref_screenLock", CwSettingsUiEvent.SETTING_SELECTED_SCREENLOCK);
        tmpMap.put("pref_lockScreenNow", CwSettingsUiEvent.SETTING_SELECTED_LOCKSCREENNOW);
        tmpMap.put("transition_animation_scale",
                CwSettingsUiEvent.SETTING_SELECTED_TRANSITION_ANIMATION_SCALE);
        tmpMap.put("window_animation_scale",
                CwSettingsUiEvent.SETTING_SELECTED_WINDOW_ANIMATION_SCALE);
        tmpMap.put("pref_screenOrientation", CwSettingsUiEvent.SETTING_SELECTED_SCREENORIENTATION);
        tmpMap.put("pref_screenOrientation_leftWrist",
                CwSettingsUiEvent.SETTING_SELECTED_SCREENORIENTATION_LEFTWRIST);
        tmpMap.put("pref_screenOrientation_rightWrist",
                CwSettingsUiEvent.SETTING_SELECTED_SCREENORIENTATION_RIGHTWRIST);
        tmpMap.put("add_virtual_keyboard_screen", CwSettingsUiEvent.SETTING_SELECTED_ADD_VIRTUAL_KEYBOARD_SCREEN);
        tmpMap.put("pref_batterySaver_suggested_settings", CwSettingsUiEvent.SETTING_SELECTED_BATTERYSAVER_SUGGESTED_SETTINGS);
        tmpMap.put("pref_divider", CwSettingsUiEvent.UNKNOWN);
        tmpMap.put("pref_logdSize", CwSettingsUiEvent.SETTING_SELECTED_LOGBUFFER_SIZE);
        tmpMap.put("pref_logPersist", CwSettingsUiEvent.SETTING_SELECTED_LOGPERSIST);
        tmpMap.put("pref_batterySaver", CwSettingsUiEvent.SETTING_SELECTED_BATTERYSAVER);
        tmpMap.put("pref_autoBatterySaver", CwSettingsUiEvent.UNKNOWN);
        tmpMap.put("pref_batterySaverMode", CwSettingsUiEvent.UNKNOWN);
        tmpMap.put("pref_appNotifications", CwSettingsUiEvent.UNKNOWN);
        tmpMap.put("number_enterprise_set_default_apps", CwSettingsUiEvent.UNKNOWN);
        tmpMap.put("number_enterprise_installed_packages", CwSettingsUiEvent.UNKNOWN);
        tmpMap.put("number_location_access_packages", CwSettingsUiEvent.UNKNOWN);
        tmpMap.put("number_microphone_access_packages", CwSettingsUiEvent.UNKNOWN);
        tmpMap.put("number_camera_access_packages", CwSettingsUiEvent.UNKNOWN);
        tmpMap.put("pref_changeSimPin", CwSettingsUiEvent.UNKNOWN);
        tmpMap.put("pref_enterprisePrivacy", CwSettingsUiEvent.UNKNOWN);
        tmpMap.put("pref_alternateLauncher", CwSettingsUiEvent.UNKNOWN);
        tmpMap.put("pref_vip_contacts", CwSettingsUiEvent.UNKNOWN);

        sPrefToEventMap = Collections.unmodifiableMap(tmpMap);

        sUserDebugOrEngBuild = "userdebug".equals(Build.TYPE)
                || Build.VERSION.INCREMENTAL.startsWith("eng.");
    }

    @Nullable
    public static CwSettingsUiEvent getLoggingId(String prefKey) {
        if (prefKey == null) {
            return null;
        }
        CwSettingsUiEvent event = sPrefToEventMap.get(prefKey);
        if (event == null) {
            event = CwSettingsUiEvent.UNKNOWN;
            if (prefKey.startsWith(IGNORE_SUBSTRING) ||
                    prefKey.contains(CTS_VERIFIER_DUMMY_SERVICE)) {
                return event;
            }
            // Catch unmapped preference logging identifiers for certain builds
            if (sUserDebugOrEngBuild) {
                throw new IllegalArgumentException(
                        "Preference key for logging not found:" + prefKey);
            }
        }
        return event;
    }
}
