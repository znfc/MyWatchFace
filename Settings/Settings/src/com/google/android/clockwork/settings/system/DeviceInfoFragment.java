package com.google.android.clockwork.settings.system;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.common.concurrent.AbstractCwRunnable;
import com.google.android.clockwork.common.concurrent.Executors;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;
import com.google.android.clockwork.settings.RegulatoryInformationActivity;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

/**
 * About screen.
 */
public class DeviceInfoFragment extends SettingsPreferenceFragment {
    private static final String TAG = "DeviceInfo";

    public static final String OEM_OPEN_SOURCE_ACTION =
            "com.google.android.apps.wearable.oem.OPENSOURCEINFO";

    private static final String GMS_PACKAGE_NAME = "com.google.android.gms";
    private static final String WEAR_PACKAGE_NAME = "com.google.android.wearable.app";

    /** Number of clicks it takes to be a developer. */
    private static final int NUM_DEVELOPER_CLICKS = 7;

    private static final String KEY_PREF_MODEL = "pref_model";
    private static final String KEY_PREF_DEVICE_NAME = "pref_deviceName";
    private static final String KEY_PREF_VERSION = "pref_version";
    private static final String KEY_PREF_SERIAL = "pref_serial";
    private static final String KEY_PREF_MSN = "pref_msn";
    private static final String KEY_PREF_BUILD = "pref_build";
    private static final String KEY_PREF_CONNECTION_STATUS = "pref_connectionStatus";
    private static final String KEY_PREF_BATTERY_INFO = "pref_batteryInfo";
    private static final String KEY_PREF_SYSTEM_UPDATE = "pref_systemUpdate";
    private static final String KEY_PREF_REGULATORY_INFO = "pref_regulatoryInfo";
    private static final String KEY_PREF_LEGAL_NOTICES = "pref_legalNotices";
    private static final String KEY_PREF_OPEN_SOURCE_NOTICES = "pref_openSourceNotices";
    private static final String KEY_PREF_IMEI = "pref_imei";

    private Toast mToast;
    private PackageManager mPackageManager;
    private int mDeveloperClickCount;
    private GoogleApiClient mApiClient;
    private Preference mBatteryInfo;

    private BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                final String level = NumberFormat.getPercentInstance().format(
                        intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) / 100.0);
                final int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                        BatteryManager.BATTERY_HEALTH_UNKNOWN);
                boolean plugged = status == BatteryManager.BATTERY_STATUS_CHARGING
                        || status == BatteryManager.BATTERY_STATUS_FULL;
                mBatteryInfo.setSummary(getString(
                        plugged ? R.string.info_value_battery_plugged : R.string.info_value_battery,
                        level));
            }
        }
    };

    private BroadcastReceiver mPackageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateVersion((PreferenceScreen) findPreference(KEY_PREF_VERSION));
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_device_info);
        addPreferencesFromResource(R.xml.prefs_device_info_customization);

        mPackageManager = getContext().getPackageManager();
        mApiClient = new GoogleApiClient.Builder(getContext()).addApi(Wearable.API).build();

        initModel(findPreference(KEY_PREF_MODEL));
        initDeviceName(findPreference(KEY_PREF_DEVICE_NAME));
        updateVersion((PreferenceScreen) findPreference(KEY_PREF_VERSION));
        initSerial(findPreference(KEY_PREF_SERIAL));
        initImei(findPreference(KEY_PREF_IMEI));
        initMsn(findPreference(KEY_PREF_MSN));
        initBuild(findPreference(KEY_PREF_BUILD));
        mBatteryInfo = findPreference(KEY_PREF_BATTERY_INFO);
        initSystemUpdate(findPreference(KEY_PREF_SYSTEM_UPDATE));
        initLegalNotices(findPreference(KEY_PREF_LEGAL_NOTICES));
        initRegulatoryInfo(findPreference(KEY_PREF_REGULATORY_INFO));
    }

    @Override
    public void onResume() {
        super.onResume();
        checkForOpenSource();

        // TODO: wire up a node listener so we can track this while the fragment is open.
        // Don't want to create a whole service just for this though.
        updateConnectionStatus(findPreference(KEY_PREF_CONNECTION_STATUS));

        IntentFilter batteryFilter = new IntentFilter();
        batteryFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        getContext().registerReceiver(mBatteryReceiver, batteryFilter);

        IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        getContext().registerReceiver(mPackageReceiver, packageFilter);

        mApiClient.connect();
    }

    @Override
    public void onPause() {
        mApiClient.disconnect();
        getContext().unregisterReceiver(mBatteryReceiver);
        getContext().unregisterReceiver(mPackageReceiver);
        super.onPause();
    }

    private void initModel(Preference pref) {
        String customizedName = getString(R.string.customized_model_name);
        pref.setSummary(TextUtils.isEmpty(customizedName) ? Build.MODEL : customizedName);
        pref.setOnPreferenceClickListener((p) -> {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null) {
                showToast(adapter.getAddress());
            }
            return true;
        });
    }

    private void initDeviceName(Preference pref) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        String name = adapter == null ? null : adapter.getName();
        pref.setSummary(TextUtils.isEmpty(name) ? Build.MODEL : name);
    }

    private void updateVersion(PreferenceScreen pref) {
        pref.removeAll();

        // Add "Wear OS by Google" version.
        String wearOsVersionString = getWearOsVersionString();
        if (!TextUtils.isEmpty(wearOsVersionString)) {
            Preference wearOsVersion = new Preference(getContext());
            wearOsVersion.setTitle(R.string.info_label_wear_os_version);
            wearOsVersion.setSummary(wearOsVersionString);
            pref.addPreference(wearOsVersion);
        }

        // Add "Home App" version.
        try {
            PackageInfo info = mPackageManager.getPackageInfo(WEAR_PACKAGE_NAME, 0);
            Preference homeAppVersion = new Preference(getContext());
            homeAppVersion.setTitle(R.string.info_label_home_app);
            homeAppVersion.setSummary(info.versionName);
            homeAppVersion.setOnPreferenceClickListener((p) -> {
                // This forces a checkin for non user builds as a debugging step.
                showToast(getString(R.string.checking_for_updates));
                getContext().sendBroadcast(new Intent("android.server.checkin.CHECKIN")
                        .setPackage(GMS_PACKAGE_NAME)
                        .setFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES)
                        .putExtra("force", true));
                return true;
            });
            pref.addPreference(homeAppVersion);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "can't find package", e);
        }

        // Add GmsCore version.
        try {
            PackageInfo info = mPackageManager.getPackageInfo(GMS_PACKAGE_NAME, 0);
            Preference gmsVersion = new Preference(getContext());
            gmsVersion.setTitle(info.applicationInfo.loadLabel(mPackageManager));
            gmsVersion.setSummary(info.versionName);
            pref.addPreference(gmsVersion);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "can't find package", e);
        }

        // Add Android OS version.
        Preference osVersion = new Preference(getContext());
        osVersion.setTitle(R.string.info_label_os_version);
        osVersion.setSummary("H");
        pref.addPreference(osVersion);

        // Add security patch level.
        if (!TextUtils.isEmpty(Build.VERSION.SECURITY_PATCH)) {
            Preference securityPatchVersion = new Preference(getContext());
            securityPatchVersion.setTitle(R.string.info_label_security_patch);
            try {
                SimpleDateFormat template = new SimpleDateFormat("yyyy-MM-dd");
                Date patchDate = template.parse(Build.VERSION.SECURITY_PATCH);
                String format = DateFormat.getBestDateTimePattern(Locale.getDefault(), "dMMMMyyyy");
                securityPatchVersion.setSummary(DateFormat.format(format, patchDate).toString());
                pref.addPreference(securityPatchVersion);
            } catch (ParseException e) {
                // broken parse; fall through and use the raw string
                Log.d(TAG, "Failed to parse date: " + Build.VERSION.SECURITY_PATCH);
            }
        } else {
            Log.d(TAG, "Unable to find security patch level from Build.VERSION.SECURITY_PATCH");
        }
    }

    private void initSerial(Preference pref) {
        pref.setSummary(Build.getSerial());
    }


    private void initImei(Preference pref) {
        TelephonyManager telephonyManager = getContext().getSystemService(TelephonyManager.class);
        final String imei = telephonyManager.getDeviceId();
        if (TextUtils.isEmpty(imei)) {
            getPreferenceScreen().removePreference(pref);
        } else {
            pref.setSummary(imei);
        }
    }

    private void initMsn(Preference pref) {
        String msn = SystemProperties.get("ro.boot.msn");
        if (TextUtils.isEmpty(msn)) {
            getPreferenceScreen().removePreference(pref);
        } else {
            pref.setSummary(msn);
        }
    }

    private void initBuild(Preference pref) {
        pref.setSummary(Build.DISPLAY);
        pref.setOnPreferenceClickListener((p) -> {
            if (checkDebuggingDisallowed()) {
                return true;
            }
            mDeveloperClickCount++;
            if (Settings.Global.getInt(getContext().getContentResolver(),
                    Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 0) {
                int numLeft = NUM_DEVELOPER_CLICKS - mDeveloperClickCount;
                if (numLeft < 3 && numLeft > 0) {
                    showToast(getResources().getQuantityString(
                            R.plurals.show_dev_countdown, numLeft, numLeft));
                }
                if (numLeft == 0) {
                    Settings.Global.putInt(getContext().getContentResolver(),
                            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);
                    showToast(getString(R.string.show_dev_on));
                    mDeveloperClickCount = 0;
                }
            } else {
                if (mDeveloperClickCount > 3) {
                    showToast(getString(R.string.show_dev_already));
                }
            }
            return true;
        });
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

    private void updateConnectionStatus(final Preference pref) {
        // TODO: would love to register a listener here, but currently that requires a whole service
        Wearable.NodeApi.getConnectedNodes(mApiClient).setResultCallback((result) -> {
            if (!result.getStatus().isSuccess()) {
                pref.setSummary(R.string.info_value_connection_error);
            } else if (result.getNodes().size() > 0) {
                pref.setSummary(R.string.info_value_connection_paired_connected);
            } else {
                pref.setSummary(R.string.info_value_connection_paired_disconnected);
            }
        });
    }

    private void initSystemUpdate(Preference pref) {
        pref.setOnPreferenceClickListener((p) -> {
            // Kick off a check in and show the system update activity
            getContext().sendBroadcast(new Intent("android.server.checkin.CHECKIN")
                    .setFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                    // Force checkin even if some errors occurred before.
                    .putExtra("force", true));
            startActivitySafely(new Intent(Settings.ACTION_SYSTEM_UPDATE_SETTINGS));
            return true;
        });
    }

    // TODO: remove this method once there are no longer any external dependencies on
    // regulatory info being present here. See b/112321659 for details.
    private void initRegulatoryInfo(Preference pref) {
        if (!getContext().getResources().getBoolean(
                R.bool.config_showRegulatoryInfoInAbout)) {
            getPreferenceScreen().removePreference(pref);
            return;
        }
        // Change the regulatory info file path based on SKU (if needed).
        String sku = SystemProperties.get("ro.boot.hardware.sku");
        if (!TextUtils.isEmpty(sku)) {
            sku = String.format("_%s", sku.toLowerCase());
        }
        boolean resolvedRegulatoryInfo = false;
        for (String path : RegulatoryInformationActivity.REGULATORY_INFO_PATHS) {
            final String regInfoPath = String.format(path, sku);
            File file = new File(regInfoPath);
            String regInfoComponent = getContext().getResources().getString(
                    R.string.config_regulatoryInfoComponentName);
            if (file.exists() || !TextUtils.isEmpty(regInfoComponent)) {
                pref.setOnPreferenceClickListener((p) -> {
                    Intent startRegInfo;
                    if (!TextUtils.isEmpty(regInfoComponent)) {
                        startRegInfo = new Intent();
                        startRegInfo.setComponent(
                                ComponentName.unflattenFromString(regInfoComponent));
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "Starting regulatory info component: " + regInfoComponent);
                        }
                    } else {
                        Bundle b = new Bundle();
                        b.putString("filePath", regInfoPath);
                        startRegInfo =
                                new Intent(getContext(), RegulatoryInformationActivity.class)
                                        .putExtras(b);
                    }
                    startActivitySafely(startRegInfo);
                    return true;
                });
                resolvedRegulatoryInfo = true;
                // Pick the first one found in the possible locations for regulatory information.
                break;
            }
        }
        if (!resolvedRegulatoryInfo) {
            getPreferenceScreen().removePreference(pref);
        }
    }

    private void initLegalNotices(Preference pref) {
        pref.setOnPreferenceClickListener((p) -> {
            showToast(getString(R.string.info_value_legal_notices));
            return true;
        });
    }

    private void showToast(String toastString) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(getContext(), toastString, Toast.LENGTH_SHORT);
        mToast.show();
    }

    private void startActivitySafely(Intent intent) {
        if (isAdded()) {
            try {
                getActivity().startActivity(intent);
            } catch (ActivityNotFoundException e) {
                showToast(getString(R.string.error_could_not_launch));
            }
        }
    }

    private void checkForOpenSource() {
        Executors.INSTANCE.get(getContext()).getBackgroundExecutor().submit(
            new AbstractCwRunnable("DevInfoOemOpenSource") {
                @Override
                public void run() {
                    List<ResolveInfo> infos = mPackageManager.queryIntentActivities(
                            new Intent(OEM_OPEN_SOURCE_ACTION), PackageManager.MATCH_DEFAULT_ONLY);
                    if (infos.size() > 0) {
                        ActivityInfo activityInfo = infos.get(0).activityInfo;
                        final ComponentName component =
                                new ComponentName(activityInfo.packageName, activityInfo.name);
                        Activity activity = getActivity();
                        if (activity != null) {
                            activity.runOnUiThread(() -> {
                                if (isAdded()) { // could be detached by the time we get here
                                    Preference pref = findPreference(KEY_PREF_OPEN_SOURCE_NOTICES);
                                    if (pref == null) {
                                        pref = new Preference(getContext());
                                        pref.setKey(KEY_PREF_OPEN_SOURCE_NOTICES);
                                        getPreferenceScreen().addPreference(pref);
                                    }
                                    pref.setTitle(R.string.info_label_open_source_information);
                                    pref.setOnPreferenceClickListener((p) -> {
                                        Intent intent = new Intent(OEM_OPEN_SOURCE_ACTION)
                                                .setComponent(component);
                                        startActivitySafely(intent);
                                        return true;
                                    });
                                }
                            });
                        }
                    }
                }
            });
    }

    private String getWearOsVersionString() {
        Cursor cursor = getContext().getContentResolver().query(
            SettingsContract.WEAR_OS_VERSION_URI, null, null, null, null);
        if (cursor != null) {
            try {
                int keyColumn = cursor.getColumnIndex(SettingsContract.COLUMN_KEY);
                int valueColumn = cursor.getColumnIndex(SettingsContract.COLUMN_VALUE);
                while (cursor.moveToNext()) {
                    if (SettingsContract.KEY_WEAR_OS_VERSION_STRING.equals(
                            cursor.getString(keyColumn))) {
                        return cursor.getString(valueColumn);
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }
}
