package com.google.android.clockwork.settings.developer;

import android.app.Activity;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.hardware.usb.IUsbManager;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.LinkProperties;
import android.net.Network;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IDeviceIdleController;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.support.wearable.preference.AcceptDenyDialogPreference;
import android.support.wearable.preference.AcceptDenySwitchPreference;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.text.TextUtils;

import java.util.List;

import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.common.concurrent.AbstractCwRunnable;
import com.google.android.clockwork.common.concurrent.Executors;
import com.google.android.clockwork.host.GKeys;
import com.google.android.clockwork.settings.AdbUtil;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;
import com.google.android.clockwork.settings.SettingsIntents;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.SmartIlluminateConfig;
import com.google.android.clockwork.settings.mobilesignaldetector.DetectorSetting;
import com.google.android.clockwork.settings.wifi.WifiAutoModeUtil;
import com.google.android.clockwork.common.os.BuildUtils;
import com.google.android.clockwork.common.content.CwPrefs;

import com.android.settingslib.display.DisplayDensityUtils;

/**
 * Developer options settings.
 */
public class DeveloperOptionsFragment extends SettingsPreferenceFragment {
    /** Action for an intent to launch an OEM debug action. */
    public static final String OEM_DEBUG_ACTION = "com.google.android.apps.wearable.oem.DEBUG";

    private static final String TAG = "DeveloperOptions";

    private static final String DEBUG_LAYOUT_KEY = "debug.layout";
    private static final String DEBUG_OVERDRAW_KEY = "debug.hwui.overdraw";
    private static final String DEBUG_PROFILE_KEY = "debug.hwui.profile";

    private static final String KEY_PREF_STAY_ON_WHILE_PLUGGED_IN = "pref_stayOnWhilePluggedIn";
    private static final String KEY_PREF_BT_SNOOP_LOG = "pref_btSnoopLog";
    private static final String KEY_PREF_CONNECTIVITY_VIBRATE = "pref_connectivityVibrate";
    private static final String KEY_PREF_OEM_DEBUG = "pref_oemDebug";
    private static final String KEY_PREF_ADB_DEBUGGING = "pref_adbDebugging";
    private static final String KEY_PREF_SMART_ILLUMINATE = "pref_smartIlluminate";
    private static final String KEY_PREF_DEBUG_OVER_BLUETOOTH = "pref_debugOverBluetooth";
    private static final String KEY_PREF_DEBUG_OVER_WIFI = "pref_debugOverWifi";
    private static final String KEY_PREF_WEAR_DEVELOPER_OPTIONS = "pref_wearDeveloperOptions";
    private static final String KEY_PREF_CLEAR_ADB_KEYS = "pref_clearAdbKeys";
    private static final String KEY_PREF_ALLOW_MOCK_LOCATIONS = "pref_allowMockLocations";
    private static final String KEY_PREF_DEBUG_LAYOUT = "pref_debugLayout";
    private static final String KEY_PREF_DEBUG_OVERDRAW = "pref_debugOverdraw";
    private static final String KEY_PREF_DEBUG_TIMING = "pref_debugTiming";
    private static final String KEY_PREF_POINTER_LOCATION = "pref_pointerLocation";
    private static final String KEY_PREF_SHOW_TOUCHES = "pref_showTouches";
    private static final String KEY_PREF_BUG_REPORT = "pref_bugReport";
    private static final String KEY_PREF_ENABLE_WIFI_CHARGING = "pref_enableWifiWhenCharging";
    private static final String KEY_PREF_WIFI_LOGGING = "pref_wifiLogging";
    private static final String KEY_PREF_CELLULAR_BATTERY_SAVER = "pref_cellularBatterySaver";
    private static final String KEY_PREF_POWER_OPTIMIZATIONS = "pref_powerOptimizations";
    private static final String KEY_PREF_DPI_SETTINGS = "pref_dpiSettings";
    private static final String KEY_PREF_WINDOW_ANIMATION_SCALE = "window_animation_scale";
    private static final String KEY_PREF_TRANSITION_ANIMATION_SCALE = "transition_animation_scale";
    private static final String KEY_PREF_ANIMATOR_DURATION_SCALE = "animator_duration_scale";

    private static final String BLUETOOTH_BTSNOOP_ENABLE_PROPERTY = "persist.bluetooth.btsnoopenable";

    private static final String VIBRATE_PREF = "cw_debug_connectivity_vibrate";

    // These prefs are owned by WearWifiMediatorService.
    private static final String ENABLE_WIFI_WHEN_CHARGING_PREF = "cw_enable_wifi_when_charging";

    private static final int STAY_ON_SETTING =
            BatteryManager.BATTERY_PLUGGED_AC |
            BatteryManager.BATTERY_PLUGGED_USB |
            BatteryManager.BATTERY_PLUGGED_WIRELESS;

    private static final String DEVICE_IDLE_SERVICE = "deviceidle";

    public static DeveloperOptionsFragment newInstance() {
        return new DeveloperOptionsFragment();
    }

    private ContentResolver mContentResolver;
    private PackageManager mPackageManager;
    private ComponentName mOemDebugOptionComponent;
    private SmartIlluminateConfig mSmartIlluminateConfig;
    private boolean mWifi;
    private IWindowManager mWindowManager;

    private SwitchPreference mDebugOverBluetoothSetting;
    private SwitchPreference mDebugOverWifiSetting;

    // CONSTANTS FOR LOG BUFFER SIZE
    private static final String SELECT_LOGD_SIZE_KEY = "pref_logdSize";
    private static final String SELECT_LOGD_SIZE_PROPERTY = "persist.logd.size";
    private static final String SELECT_LOGD_TAG_PROPERTY = "persist.log.tag";
    // Tricky, isLoggable only checks for first character, assumes silence
    private static final String SELECT_LOGD_TAG_SILENCE = "Settings";
    private static final String SELECT_LOGD_SNET_TAG_PROPERTY = "persist.log.tag.snet_event_log";
    private static final String SELECT_LOGD_RUNTIME_SNET_TAG_PROPERTY = "log.tag.snet_event_log";
    private static final String SELECT_LOGD_DEFAULT_SIZE_PROPERTY = "ro.logd.size";
    private static final String SELECT_LOGD_DEFAULT_SIZE_VALUE = "262144";
    private static final String SELECT_LOGD_SVELTE_DEFAULT_SIZE_VALUE = "65536";
    // 32768 is merely a menu marker, 64K is our lowest log buffer size we replace it with.
    private static final String SELECT_LOGD_MINIMUM_SIZE_VALUE = "65536";
    private static final String SELECT_LOGD_OFF_SIZE_MARKER_VALUE = "32768";
    private ListPreference mLogdSize;
    private static Object sLogSizeVal = null; // Used to save log buffer size value after closing.

    // CONSTANTS FOR LOG PERSIST
    private static final String SELECT_LOGPERSIST_KEY = "pref_logPersist";
    private static final String SELECT_LOGPERSIST_PROPERTY = "persist.logd.logpersistd";
    private static final String ACTUAL_LOGPERSIST_PROPERTY = "logd.logpersistd";
    private static final String SELECT_LOGPERSIST_PROPERTY_SERVICE = "logcatd";
    private static final String SELECT_LOGPERSIST_PROPERTY_CLEAR = "clear";
    private static final String SELECT_LOGPERSIST_PROPERTY_STOP = "stop";
    private static final String SELECT_LOGPERSIST_PROPERTY_BUFFER =
            "persist.logd.logpersistd.buffer";
    private static final String ACTUAL_LOGPERSIST_PROPERTY_BUFFER = "logd.logpersistd.buffer";
    private static final String ACTUAL_LOGPERSIST_PROPERTY_ENABLE = "logd.logpersistd.enable";
    private ListPreference mLogpersist;
    private boolean mLogpersistCleared;
    private Dialog mLogpersistClearDialog;

    private final NetworkCallback mNetworkCallback = new NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            doUpdate();
        }

        @Override
        public void onLost(Network network) {
            doUpdate();
        }

        @Override
        public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
            doUpdate();
        }

        private void doUpdate() {
            getActivity().runOnUiThread(() -> {
                updateDebugOverWifiSummary();
            });
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_developer_options);
        addPreferencesFromResource(R.xml.prefs_developer_options_customization);

        mContentResolver = getActivity().getContentResolver();
        mPackageManager = getActivity().getPackageManager();
        mWifi = mPackageManager.hasSystemFeature(PackageManager.FEATURE_WIFI);
        mWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));

        mDebugOverBluetoothSetting =
                (SwitchPreference) findPreference(KEY_PREF_DEBUG_OVER_BLUETOOTH);
        mDebugOverWifiSetting = (SwitchPreference) findPreference(KEY_PREF_DEBUG_OVER_WIFI);

        mLogdSize = (ListPreference) findPreference(SELECT_LOGD_SIZE_KEY);
        initLogBufferSize(mLogdSize);
        mLogpersist = (ListPreference) findPreference(SELECT_LOGPERSIST_KEY);
        initLogPersist(mLogpersist);

        initStayOnWhilePluggedIn(
                (SwitchPreference) findPreference(KEY_PREF_STAY_ON_WHILE_PLUGGED_IN));
        initBtSnoopLog((SwitchPreference) findPreference(KEY_PREF_BT_SNOOP_LOG));
        initConnectivityVibrate((SwitchPreference) findPreference(KEY_PREF_CONNECTIVITY_VIBRATE));
        initAdbDebugging((AcceptDenySwitchPreference) findPreference(KEY_PREF_ADB_DEBUGGING));
        initSmartIlluminate((SwitchPreference) findPreference(KEY_PREF_SMART_ILLUMINATE));
        initDebugOverBluetooth(mDebugOverBluetoothSetting);
        initDebugOverWifi(mDebugOverWifiSetting);
        initWearDeveloperOptions(findPreference(KEY_PREF_WEAR_DEVELOPER_OPTIONS));
        initClearAdbKeys((AcceptDenyDialogPreference) findPreference(KEY_PREF_CLEAR_ADB_KEYS));
        initAllowMockLocations((SwitchPreference) findPreference(KEY_PREF_ALLOW_MOCK_LOCATIONS));
        initDebugLayout((SwitchPreference) findPreference(KEY_PREF_DEBUG_LAYOUT));
        initDebugOverdraw((SwitchPreference) findPreference(KEY_PREF_DEBUG_OVERDRAW));
        initDebugTiming((SwitchPreference) findPreference(KEY_PREF_DEBUG_TIMING));
        initPointerLocation((SwitchPreference) findPreference(KEY_PREF_POINTER_LOCATION));
        initShowTouches((SwitchPreference) findPreference(KEY_PREF_SHOW_TOUCHES));
        initBugReport((SwitchPreference) findPreference(KEY_PREF_BUG_REPORT));
        initEnableWifiWhileCharging((SwitchPreference) findPreference(KEY_PREF_ENABLE_WIFI_CHARGING));
        initWifiLogging((SwitchPreference) findPreference(KEY_PREF_WIFI_LOGGING));
        initCellularBatterySaver(
                (SwitchPreference) findPreference(KEY_PREF_CELLULAR_BATTERY_SAVER));
        initPowerOptimizationsSettings(
                (PreferenceScreen) findPreference(KEY_PREF_POWER_OPTIMIZATIONS));
        initDpiSettings((ListPreference) findPreference(KEY_PREF_DPI_SETTINGS));

        initWindowAnimationScale((ListPreference) findPreference(KEY_PREF_WINDOW_ANIMATION_SCALE));
        initTransitionAnimationScale(
                (ListPreference) findPreference(KEY_PREF_TRANSITION_ANIMATION_SCALE));
        initAnimatorDurationScale(
                (ListPreference) findPreference(KEY_PREF_ANIMATOR_DURATION_SCALE));

        // If debugging not allowed, close.
        if (checkDebuggingDisallowed()) {
            getActivity().finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkForOemDebug();

        ConnectivityManager cm = getContext().getSystemService(ConnectivityManager.class);
        cm.registerNetworkCallback(AdbUtil.buildWifiNetworkRequest(), mNetworkCallback);
        updateDebugOverWifiSummary();
    }

    @Override
    public void onPause() {
        ConnectivityManager cm = getContext().getSystemService(ConnectivityManager.class);
        cm.unregisterNetworkCallback(mNetworkCallback);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mSmartIlluminateConfig != null) {
            mSmartIlluminateConfig.unregister();
        }
        super.onDestroy();
    }

    // To allow users to control log buffer size.
    private void initLogBufferSize(ListPreference pref) {
        mLogdSize.setSummary("%s");
        writeLogdSizeOption(sLogSizeVal);
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            writeLogdSizeOption(newVal);
            sLogSizeVal = newVal;
            return true;
        });
    }

    // To allow users to control logging persistence.
    private void initLogPersist(ListPreference pref) {
         if (!("1".equals(SystemProperties.get("ro.debuggable", "0")))) {
            if (pref != null) {
                pref.setEnabled(false);
                getPreferenceScreen().removePreference(pref);
            }
            mLogpersist = null;
            return;
        }

        mLogpersist.setSummary("%s");
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            writeLogpersistOption(newVal, false);
            return true;
        });
    }

    // Clears the logs.
    private void dismissLogPersistDialog() {
        if (mLogpersistClearDialog != null) {
            mLogpersistClearDialog.dismiss();
            mLogpersistClearDialog = null;
        }
    }

    // Update log persist values within the preference.
    private void updateLogpersistValues() {
        if (mLogpersist == null) {
            return;
        }
        String currentValue = SystemProperties.get(ACTUAL_LOGPERSIST_PROPERTY);
        if (currentValue == null) {
            currentValue = "";
        }
        String currentBuffers = SystemProperties.get(ACTUAL_LOGPERSIST_PROPERTY_BUFFER);
        if ((currentBuffers == null) || (currentBuffers.length() == 0)) {
            currentBuffers = "all";
        }
        int index = 0;
        if (currentValue.equals(SELECT_LOGPERSIST_PROPERTY_SERVICE)) {
            index = 1; // "all"
            if (currentBuffers.equals("kernel")) {
                index = 3; // "kernel"
            } else if (!currentBuffers.equals("all") &&
                    !currentBuffers.contains("radio") &&
                    currentBuffers.contains("security") &&
                    currentBuffers.contains("kernel")) {
                index = 2; // "default,events,security,kernel" or "main,events,system,crash,security,kernel"
                if (!currentBuffers.contains("default")) {
                    String[] contains = {"main", "events", "system", "crash"};
                    for (int i = 0; i < contains.length; i++) {
                        if (!currentBuffers.contains(contains[i])) {
                            index = 1;
                            break;
                        }
                    }
                }
            }
        }
        mLogpersist.setValue(
                getResources().getStringArray(R.array.select_logpersist_values)[index]);
        if (index != 0) {
            mLogpersistCleared = false;
        } else if (!mLogpersistCleared) {
            // Would File.delete() directly but need to switch uid/gid to access.
            SystemProperties.set(ACTUAL_LOGPERSIST_PROPERTY, SELECT_LOGPERSIST_PROPERTY_CLEAR);
            mLogpersistCleared = true;
        }
    }

    /* If log persist is set to be 'off', then set all the log persist properties to be ""
       so a logcat file is not created */
    private void setLogpersistOff(boolean update) {
        SystemProperties.set(SELECT_LOGPERSIST_PROPERTY_BUFFER, "");
        // Deal with trampoline of empty properties.
        SystemProperties.set(ACTUAL_LOGPERSIST_PROPERTY_BUFFER, "");
        SystemProperties.set(SELECT_LOGPERSIST_PROPERTY, "");
        SystemProperties.set(ACTUAL_LOGPERSIST_PROPERTY,
                update ? "" : SELECT_LOGPERSIST_PROPERTY_STOP);
        if (update) {
            updateLogpersistValues();
        } else {
            // Waiting for the logpersist service to respond.
            for (int i = 0; i < 3; i++) {
                String currentValue = SystemProperties.get(ACTUAL_LOGPERSIST_PROPERTY);
                if ((currentValue == null) || currentValue.equals("")) {
                    break;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    // Write the log persist option to SystemProperties.
    private void writeLogpersistOption(Object newValue, boolean skipWarning) {
        if (mLogpersist == null) {
            return;
        }

        String currentTag = SystemProperties.get(SELECT_LOGD_TAG_PROPERTY);
        if ((currentTag != null) && currentTag.startsWith(SELECT_LOGD_TAG_SILENCE)) {
            newValue = null;
            skipWarning = true;
        }

        if ((newValue == null) || newValue.toString().equals("")) {
            if (skipWarning) {
                mLogpersistCleared = false;
            } else if (!mLogpersistCleared) {
                // If transitioning from on to off, pop up an are you sure?
                String currentValue = SystemProperties.get(ACTUAL_LOGPERSIST_PROPERTY);
                if ((currentValue != null) &&
                        currentValue.equals(SELECT_LOGPERSIST_PROPERTY_SERVICE)) {
                    if (mLogpersistClearDialog != null) dismissLogPersistDialog();
                        mLogpersistClearDialog = new AlertDialog.Builder(getActivity()).setMessage(
                                getActivity().getResources().getString(
                                        R.string.dev_logpersist_clear_warning_message))
                                .setTitle(R.string.dev_logpersist_clear_warning_title)
                                .setPositiveButton(android.R.string.yes, new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        setLogpersistOff(true);
                                    }
                                })
                                .setNegativeButton(android.R.string.no, new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        updateLogpersistValues();
                                    }
                                })
                                .show();

                    mLogpersistClearDialog.setOnDismissListener(new OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            mLogpersistClearDialog = null;
                        }
                    });
                    return;
                }
            }
            setLogpersistOff(true);
            return;
        }

        String currentBuffer = SystemProperties.get(ACTUAL_LOGPERSIST_PROPERTY_BUFFER);
        if ((currentBuffer != null) && !currentBuffer.equals(newValue.toString())) {
            setLogpersistOff(false); // Causes SELECT_LOGPERSIST_PROPERTY_BUFFER to be set to "".
        }
        SystemProperties.set(SELECT_LOGPERSIST_PROPERTY_BUFFER, newValue.toString());
        SystemProperties.set(SELECT_LOGPERSIST_PROPERTY, SELECT_LOGPERSIST_PROPERTY_SERVICE);

        // Wait for the logpersist service to start up with parameters.
        for (int i = 0; i < 3; i++) {
            String currentValue = SystemProperties.get(ACTUAL_LOGPERSIST_PROPERTY);
            if ((currentValue != null)
                    && currentValue.equals(SELECT_LOGPERSIST_PROPERTY_SERVICE)) {
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
        updateLogpersistValues();
    }

    // Returns the default log buffer size, which is dependent upon ram info.
    private String defaultLogdSizeValue() {
        String defaultValue = SystemProperties.get(SELECT_LOGD_DEFAULT_SIZE_PROPERTY);
        if (TextUtils.isEmpty(defaultValue)) {
            if (SystemProperties.get("ro.config.low_ram").equals("true")) {
                defaultValue = SELECT_LOGD_SVELTE_DEFAULT_SIZE_VALUE;
            } else {
                defaultValue = SELECT_LOGD_DEFAULT_SIZE_VALUE;
            }
        }
        return defaultValue;
    }

    // Write the selected log size value to SystemProperties.
    private void writeLogdSizeOption(Object newValue) {
        boolean disable = SELECT_LOGD_OFF_SIZE_MARKER_VALUE.equals(newValue);
        String currentTag = SystemProperties.get(SELECT_LOGD_TAG_PROPERTY);
        if (currentTag == null) {
            currentTag = "";
        }
        // Filter clean and unstack all references to our setting.
        String newTag = currentTag.replaceAll(
                ",+" + SELECT_LOGD_TAG_SILENCE, "").replaceFirst(
                "^" + SELECT_LOGD_TAG_SILENCE + ",*", "").replaceAll(
                ",+", ",").replaceFirst(
                ",+$", "");
        if (disable) {
            newValue = SELECT_LOGD_MINIMUM_SIZE_VALUE;
            // Make sure snet_event_log get through first, but do not override.
            String snetValue = SystemProperties.get(SELECT_LOGD_SNET_TAG_PROPERTY);
            if (TextUtils.isEmpty(snetValue)) {
                snetValue = SystemProperties.get(SELECT_LOGD_RUNTIME_SNET_TAG_PROPERTY);
                if (TextUtils.isEmpty(snetValue)) {
                    SystemProperties.set(SELECT_LOGD_SNET_TAG_PROPERTY, "I");
                }
            }
            // Silence all log sources, security logs notwithstanding.
            if (newTag.length() != 0) {
                newTag = "," + newTag;
            }
            // Stack settings, stack to help preserve original value.
            newTag = SELECT_LOGD_TAG_SILENCE + newTag;
        }
        if (!newTag.equals(currentTag)) {
            SystemProperties.set(SELECT_LOGD_TAG_PROPERTY, newTag);
        }
        String defaultValue = defaultLogdSizeValue();
        final String size = ((newValue != null) && (newValue.toString().length() != 0))
                ? newValue.toString() : defaultValue;
        SystemProperties.set(SELECT_LOGD_SIZE_PROPERTY, defaultValue.equals(size) ? "" : size);
        SystemProperties.set("ctl.start", "logd-reinit");
        updateLogdSizeValues();
    }

    // Update the selected log size value within the preference.
    private void updateLogdSizeValues() {
        if(mLogdSize != null) {
            String currentTag = SystemProperties.get(SELECT_LOGD_TAG_PROPERTY);
            String currentValue = SystemProperties.get(SELECT_LOGD_SIZE_PROPERTY);
            if ((currentTag != null) && currentTag.startsWith(SELECT_LOGD_TAG_SILENCE)) {
                currentValue = SELECT_LOGD_OFF_SIZE_MARKER_VALUE;
            }
            if (mLogpersist != null) {
                String currentLogpersistEnable
                        = SystemProperties.get(ACTUAL_LOGPERSIST_PROPERTY_ENABLE);
                mLogpersist.setEnabled(true);
                if ((currentLogpersistEnable == null)
                        || !currentLogpersistEnable.equals("true")
                        || currentValue.equals(SELECT_LOGD_OFF_SIZE_MARKER_VALUE)) {
                    writeLogpersistOption(null, true);
                    mLogpersist.setEnabled(false);
                }
            }
            if ((currentValue == null) || (currentValue.length() == 0)) {
                currentValue = defaultLogdSizeValue();
            }
            String[] values = getResources().getStringArray(R.array.select_logd_size_values);
            String[] titles = getResources().getStringArray(R.array.select_logd_size_titles);
            int index = 2; // Punt to second entry if not found.
            if (SystemProperties.get("ro.config.low_ram").equals("true")) {
                mLogdSize.setEntries(R.array.select_logd_size_lowram_titles);
                titles = getResources().getStringArray(R.array.select_logd_size_lowram_titles);
                index = 1;
            }
            String[] summaries = getResources().getStringArray(R.array.select_logd_size_summaries);
            for (int i = 0; i < titles.length; i++) {
                if (currentValue.equals(values[i])
                        || currentValue.equals(titles[i])) {
                    index = i;
                    break;
                }
            }
            mLogdSize.setValue(values[index]);
        }
    }

    private void initStayOnWhilePluggedIn(SwitchPreference pref) {
        pref.setChecked(Settings.Global.getInt(mContentResolver,
                Settings.Global.STAY_ON_WHILE_PLUGGED_IN, 0) != 0);
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            Settings.Global.putInt(
                    mContentResolver,
                    Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                    ((Boolean) newVal) ? STAY_ON_SETTING : 0);
            return true;
        });
    }

    private void initBtSnoopLog(SwitchPreference pref) {
        pref.setChecked(SystemProperties.getBoolean(BLUETOOTH_BTSNOOP_ENABLE_PROPERTY, false));
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            boolean enabled = (Boolean) newVal;
            SystemProperties.set(BLUETOOTH_BTSNOOP_ENABLE_PROPERTY,
                    Boolean.toString(enabled));
            return true;
        });
    }

    private void initConnectivityVibrate(SwitchPreference pref) {
        pref.setChecked(Settings.Global.getInt(mContentResolver, VIBRATE_PREF, 0) != 0);
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            Settings.Global.putInt(mContentResolver, VIBRATE_PREF, ((Boolean) newVal) ? 1 : 0);
            return true;
        });
    }

    private void initAdbDebugging(AcceptDenySwitchPreference pref) {
        pref.setChecked(Settings.Global.getInt(mContentResolver,
                Settings.Global.ADB_ENABLED, 0) != 0);
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            // don't disconnect if monkey so it doesn't break a monkey test
            if (!ActivityManager.isUserAMonkey()) {
                AdbUtil.setAdbDebugging(getActivity(), (Boolean) newVal);
            }
            return true;
        });
    }

    private void initSmartIlluminate(SwitchPreference pref) {
        if (GKeys.SMART_ILLUMINATE_ENABLED.get() && GKeys.SMART_ILLUMINATE_SETTING_VISIBLE.get()) {
            mSmartIlluminateConfig = new SmartIlluminateConfig(getActivity());
            mSmartIlluminateConfig.register();

            pref.setChecked(mSmartIlluminateConfig.isSmartIlluminateEnabled());
            pref.setOnPreferenceChangeListener((p, newVal) -> {
                SmartIlluminateConfig.setSmartIlluminateEnabled(getActivity(), (Boolean) newVal);
                return true;
            });
        } else {
            getPreferenceScreen().removePreference(pref);
        }
    }

    private void initDebugOverBluetooth(SwitchPreference pref) {
        pref.setChecked(
                AdbUtil.getWirelessDebugSetting(getActivity()) == AdbUtil.WIRELESS_DEBUG_BLUETOOTH);
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            // Toggle the setting
            boolean enabled = (Boolean) newVal;
            AdbUtil.toggleBluetoothDebugging(getActivity(), enabled);
            if (enabled) {
                mDebugOverWifiSetting.setChecked(false);
            }
            return true;
        });
    }

    private void initDebugOverWifi(SwitchPreference pref) {
        pref.setChecked(
                AdbUtil.getWirelessDebugSetting(getActivity()) == AdbUtil.WIRELESS_DEBUG_WIFI);
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            // Toggle the setting
            boolean enabled = (Boolean) newVal;
            AdbUtil.toggleWifiDebugging(getActivity(), enabled);
            if (enabled) {
                mDebugOverBluetoothSetting.setChecked(false);
                updateDebugOverWifiSummary();
            }
            return true;
        });
        updateDebugOverWifiSummary();
    }

    private void updateDebugOverWifiSummary() {
        mDebugOverWifiSetting.setSummaryOn(AdbUtil.getWirelessDebuggingAddresses(getActivity()));
    }

    private void initWearDeveloperOptions(Preference pref) {
        pref.setOnPreferenceClickListener((p) -> {
            getActivity().startActivity(SettingsIntents.getDeveloperOptionsIntent());
            return true;
        });
    }

    private void initClearAdbKeys(AcceptDenyDialogPreference pref) {
        pref.setOnDialogClosedListener((positiveResult) -> {
            // don't run if monkey so it doesn't break monkey test
            if (positiveResult && !ActivityManager.isUserAMonkey()) {
                try {
                    IBinder binder = ServiceManager.getService(Context.USB_SERVICE);
                    IUsbManager service = IUsbManager.Stub.asInterface(binder);
                    service.clearUsbDebuggingKeys();
                    Log.i(TAG, "cleared debugging keys");
                } catch (Exception e) {
                    Log.e(TAG, "Unable to notify Usb service", e);
                }
            }
        });
    }

    private void initAllowMockLocations(SwitchPreference pref) {
        pref.setChecked(Settings.Secure.getInt(mContentResolver,
                Settings.Secure.ALLOW_MOCK_LOCATION, 0) == 1);
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            Settings.Secure.putInt(mContentResolver,
                    Settings.Secure.ALLOW_MOCK_LOCATION, ((Boolean) newVal) ? 1 : 0);
            return true;
        });
    }

    private void initDebugLayout(SwitchPreference pref) {
        pref.setChecked(SystemProperties.getBoolean(DEBUG_LAYOUT_KEY, false));
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            SystemProperties.set(DEBUG_LAYOUT_KEY, ((Boolean) newVal) ? "true" : "false");
            pokeSystemProperties();
            return true;
        });
    }

    private void initDebugOverdraw(SwitchPreference pref) {
        pref.setChecked("show".equals(SystemProperties.get(DEBUG_OVERDRAW_KEY)));
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            SystemProperties.set(DEBUG_OVERDRAW_KEY, ((Boolean) newVal) ? "show" : "false");
            pokeSystemProperties();
            return true;
        });
    }

    private void initDebugTiming(SwitchPreference pref) {
        pref.setChecked("visual_bars".equals(SystemProperties.get(DEBUG_PROFILE_KEY)));
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            SystemProperties.set(DEBUG_PROFILE_KEY, ((Boolean) newVal) ? "visual_bars" : "false");
            pokeSystemProperties();
            return true;
        });
    }

    private void initPointerLocation(SwitchPreference pref) {
        pref.setChecked(Settings.System.getInt(mContentResolver,
                Settings.System.POINTER_LOCATION, 0) == 1);
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            Settings.System.putInt(mContentResolver,
                    Settings.System.POINTER_LOCATION, ((Boolean) newVal) ? 1 : 0);
            return true;
        });
    }

    private void initShowTouches(SwitchPreference pref) {
        pref.setChecked(Settings.System.getInt(mContentResolver,
                Settings.System.SHOW_TOUCHES, 0) == 1);
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            Settings.System.putInt(mContentResolver, Settings.System.SHOW_TOUCHES,
                    ((Boolean) newVal) ? 1 : 0);
            return true;
        });
    }

    private void initBugReport(SwitchPreference pref) {
        boolean enabled = false;
        Cursor cursor = mContentResolver.query(
                SettingsContract.BUG_REPORT_URI, null, null, null, null);
        if (cursor != null) {
            try {
                while(cursor.moveToNext()) {
                    if (SettingsContract.KEY_BUG_REPORT.equals(cursor.getString(0))) {
                        enabled = cursor.getInt(1) == SettingsContract.BUG_REPORT_ENABLED;
                    }
                }
            } finally {
                cursor.close();
            }
        }

        pref.setChecked(enabled);
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            ContentValues values = new ContentValues();
            values.put(SettingsContract.KEY_BUG_REPORT, ((Boolean) newVal) ?
                    SettingsContract.BUG_REPORT_ENABLED : SettingsContract.BUG_REPORT_DISABLED);
            mContentResolver.update(SettingsContract.BUG_REPORT_URI, values, null, null);
            return true;
        });
    }

    /**
     * Determines whether WifiMediator should turn WiFi on when the device is placed on the charger.
     *
     * Defaults to on.
     */
    private void initEnableWifiWhileCharging(SwitchPreference pref) {
        if (mWifi) {
            pref.setChecked(Settings.System.getInt(
                    mContentResolver, ENABLE_WIFI_WHEN_CHARGING_PREF, 1) != 0);
            pref.setOnPreferenceChangeListener((p, newVal) -> {
                Settings.System.putInt(mContentResolver, ENABLE_WIFI_WHEN_CHARGING_PREF,
                        ((Boolean) newVal) ? 1 : 0);
                return true;
            });
        } else {
            getPreferenceScreen().removePreference(pref);
        }
    }

    private void initWifiLogging(SwitchPreference pref) {
        if (mWifi) {
            final WifiManager wifiManager = getActivity().getSystemService(WifiManager.class);
            pref.setChecked(wifiManager.getVerboseLoggingLevel() > 0);
            pref.setOnPreferenceChangeListener((p, newVal) -> {
                wifiManager.enableVerboseLogging((Boolean) newVal ? 1 : 0);
                return true;
            });
        } else {
            getPreferenceScreen().removePreference(pref);
        }
    }

    private void initCellularBatterySaver(SwitchPreference pref) {
        pref.setChecked(DetectorSetting.isMobileSignalDetectorAllowed(getContext()));
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            DetectorSetting.setMobileSignalDetectorAllowed(getContext(), (Boolean) newVal);
            return true;
        });
    }

    private void initPowerOptimizationsSettings(final PreferenceScreen pref) {
        IDeviceIdleController deviceIdleService = IDeviceIdleController.Stub.asInterface(
                ServiceManager.getService(DEVICE_IDLE_SERVICE));

        String[] sysWhitelistedApps;
        String[] whitelistedApps;
        try {
            sysWhitelistedApps = deviceIdleService.getSystemPowerWhitelist();
            whitelistedApps = deviceIdleService.getFullPowerWhitelist();
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to reach IDeviceIdleController", e);
            return;
        }

        for (String app : sysWhitelistedApps) {
            Preference appPref = new Preference(getActivity());
            appPref.setPersistent(false);
            try {
                ApplicationInfo applicationInfo =
                        mPackageManager.getApplicationInfo(app, 0 /* flags */);
                appPref.setTitle(applicationInfo != null
                        ? mPackageManager.getApplicationLabel(applicationInfo)
                        : app);
                appPref.setKey(applicationInfo != null
                        ? "ignore_" + mPackageManager.getApplicationLabel(applicationInfo)
                        : "ignore_" + app);
            } catch (PackageManager.NameNotFoundException e) {
                appPref.setTitle(app);
            }
            appPref.setSummary(R.string.sim_status_field_not_available);
            pref.addPreference(appPref);
        }
        for (String app : whitelistedApps) {
            Preference appPref = new Preference(getActivity());
            appPref.setPersistent(false);
            appPref.setKey("ignore_" + app);
            appPref.setTitle(app);
            appPref.setSummary(R.string.sim_status_field_not_available);
            pref.addPreference(appPref);
        }
    }

    private void initDpiSettings(ListPreference pref) {
        if (!BuildUtils.IS_USER_BUILD) {
            final int defaultDensity = getDefaultDisplayDensity(Display.DEFAULT_DISPLAY);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Default Density: " + defaultDensity);
            }

            int currentDensity = getResources().getDisplayMetrics().densityDpi;
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Current Density: " + currentDensity);
            }

            CharSequence[] values = pref.getEntryValues();
            CharSequence closetValue = values[0];
            if (currentDensity != defaultDensity) {
                int closest = Integer.MAX_VALUE;
                for (int i = 1; i < values.length; ++i) {
                    int density = Integer.parseInt(values[i].toString());
                    if (Math.abs(currentDensity - density) < closest) {
                        closest = Math.abs(currentDensity - density);
                        closetValue = values[i];
                    }
                }
            }

            pref.setValue(closetValue.toString());

            pref.setOnPreferenceChangeListener((p, newVal) -> {
                CharSequence value = (CharSequence) newVal;
                if (TextUtils.isEmpty(value)) {
                    // When the user selects "Default"
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Clearing forced display density.");
                    }
                    DisplayDensityUtils.clearForcedDisplayDensity(Display.DEFAULT_DISPLAY);
                    return true;
                }

                int density = Integer.parseInt(value.toString());
                if (density == defaultDensity) {
                    // When the user selects a number that is the default.
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Clearing forced display density, user selected explicit default");
                    }
                    DisplayDensityUtils.clearForcedDisplayDensity(Display.DEFAULT_DISPLAY);
                    return true;
                }

                // Otherwise, this is one of the adjustable options.
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "Setting forced display density to " + density);
                }
                DisplayDensityUtils.setForcedDisplayDensity(Display.DEFAULT_DISPLAY, density);
                return true;
            });
        } else {
            getPreferenceScreen().removePreference(pref);
        }
    }

    private void initWindowAnimationScale(ListPreference pref) {
        updateAnimationScaleValue(0, pref);
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            writeAnimationScaleOption(0, pref, newVal);
            return true;
        });
    }
    private void initTransitionAnimationScale(ListPreference pref) {
        updateAnimationScaleValue(1, pref);
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            writeAnimationScaleOption(1, pref, newVal);
            return true;
        });
    }
    private void initAnimatorDurationScale(ListPreference pref) {
        updateAnimationScaleValue(2, pref);
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            writeAnimationScaleOption(2, pref, newVal);
            return true;
        });
    }

    private void updateAnimationScaleValue(int which, ListPreference pref) {
        try {
            float scale = mWindowManager.getAnimationScale(which);
            CharSequence[] values = pref.getEntryValues();
            for (int i = 0; i < values.length; i++) {
                if (scale <= Float.parseFloat(values[i].toString())) {
                    pref.setValueIndex(i);
                    return;
                }
            }
            pref.setValueIndex(values.length - 1);
        } catch (RemoteException e) {
        }
    }

    private void writeAnimationScaleOption(int which, ListPreference pref, Object newValue) {
        try {
            float scale = newValue != null ? Float.parseFloat(newValue.toString()) : 1;
            mWindowManager.setAnimationScale(which, scale);
        } catch (RemoteException e) {
        }
    }

    public void checkForOemDebug() {
        Executors.INSTANCE.get(getActivity()).getBackgroundExecutor().submit(
            new AbstractCwRunnable("DevOptOemDebug") {
                @Override
                public void run() {
                    PackageManager manager = mPackageManager;
                    List<ResolveInfo> infos = manager.queryIntentActivities(
                            new Intent(OEM_DEBUG_ACTION),
                            PackageManager.MATCH_DEFAULT_ONLY);
                    if (infos.size() > 0) {
                        ActivityInfo activityInfo = infos.get(0).activityInfo;
                        final String summary = activityInfo.loadLabel(manager).toString();
                        final ComponentName component =
                                new ComponentName(activityInfo.packageName, activityInfo.name);
                        Activity activity = getActivity();
                        if (activity != null) {
                            activity.runOnUiThread(() -> {
                                if (isAdded()) { // could be detached by the time we get here
                                    Preference pref = findPreference(KEY_PREF_OEM_DEBUG);
                                    if (pref == null) {
                                        pref = new Preference(getContext());
                                        pref.setKey(KEY_PREF_OEM_DEBUG);
                                        getPreferenceScreen().addPreference(pref);
                                    }
                                    pref.setTitle(R.string.pref_oemDebug);
                                    pref.setSummary(TextUtils.isEmpty(summary)
                                            ? getString(R.string.pref_oemDebug_summary)
                                            : summary);
                                    pref.setIcon(R.drawable.ic_cc_settings_morehorizontal);
                                    pref.setOnPreferenceClickListener((p) -> {
                                        Intent intent = new Intent(OEM_DEBUG_ACTION)
                                                .setComponent(component);
                                        try {
                                            startActivity(intent);
                                        } catch (ActivityNotFoundException e) {
                                            // Should never get here: we did a package manager query
                                            // to decide to show this.
                                            Log.e(TAG, "Could not launch OEM Debug app");
                                        }
                                        return true;
                                    });
                                }
                            });
                        }
                    }
                }
            });
    }

    public void pokeSystemProperties() {
        Executors.INSTANCE.get(getActivity()).getBackgroundExecutor().submit(
            new AbstractCwRunnable("DevOptSysPropPoker") {
                @Override
                public void run() {
                    String[] services = ServiceManager.listServices();
                    if (services == null) {
                        return;
                    }
                    for (String service : services) {
                        IBinder obj = ServiceManager.checkService(service);
                        if (obj != null) {
                            Parcel data = Parcel.obtain();
                            try {
                                obj.transact(IBinder.SYSPROPS_TRANSACTION, data, null, 0);
                            } catch (RemoteException e) {
                            } catch (Exception e) {
                                Log.i(TAG, "Someone wrote a bad service '" + service
                                        + "' that doesn't like to be poked: " + e);
                            }
                            data.recycle();
                        }
                    }
                }
            });
    }

    /**
     * Returns the default density for the specified display.
     *
     * @param displayId the identifier of the display
     * @return the default density of the specified display, or {@code -1} if
     *         the display does not exist or the density could not be obtained
     */
    private static int getDefaultDisplayDensity(int displayId) {
        try {
            final IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
            return wm.getInitialDisplayDensity(displayId);
        } catch (RemoteException exc) {
            throw new IllegalStateException("Cannot communicate with WindowManager?!", exc);
        }
    }

    private boolean checkDebuggingDisallowed() {
        final EnforcedAdmin admin = RestrictedLockUtils.checkIfRestrictionEnforced(getActivity(),
                UserManager.DISALLOW_DEBUGGING_FEATURES, UserHandle.myUserId());
        if (admin != null) {
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getActivity(), admin);
            return true;
        }
        return false;
    }
}
