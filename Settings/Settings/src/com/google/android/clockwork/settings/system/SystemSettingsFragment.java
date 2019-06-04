package com.google.android.clockwork.settings.system;

import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IPowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.support.wearable.preference.AcceptDenyDialogPreference;
import android.support.wearable.preference.AcceptDenySwitchPreference;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.clockwork.settings.BatterySaverUtil;
import java.io.File;

import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;
import com.google.android.clockwork.settings.RegulatoryInformationActivity;

import static android.os.UserManager.DISALLOW_FACTORY_RESET;

/**
 * System settings.
 */
public class SystemSettingsFragment extends SettingsPreferenceFragment {
    private static final String TAG = "SystemSettingsFragment";

    private static final String KEY_PREF_FACTORY_RESET = "pref_factoryReset";
    private static final String KEY_PREF_BATTERY_STATE = "pref_batterySaverMode";
    private static final String KEY_PREF_REGULATORY_INFO = "pref_regulatoryInfo";

    BatteryManager mBatteryManager;
    private Toast mToast;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_system);
        addPreferencesFromResource(R.xml.prefs_system_customization);

        if (mBatteryManager == null) {
            mBatteryManager =
                    (BatteryManager) getActivity().getSystemService(BatteryManager.class);
        }
        initBatterySaver(findPreference(KEY_PREF_BATTERY_STATE), mBatteryManager);
        initRegulatoryInfo(findPreference(KEY_PREF_REGULATORY_INFO));
    }

    /**
     * Intercepts clicks on disallowed preferences, and brings up status-message dialog.
     * OnPreferenceClickListener() doesn't bypass preference click actions, so this is
     * necessary.
     */
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        String key = preference.getKey();
        if (KEY_PREF_FACTORY_RESET.equals(key)) {
            final EnforcedAdmin factoryResetEnforcedAdmin =
                    RestrictedLockUtils.checkIfRestrictionEnforced(getActivity(),
                            DISALLOW_FACTORY_RESET, UserHandle.myUserId());
            if (factoryResetEnforcedAdmin != null) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getActivity(),
                        factoryResetEnforcedAdmin);
                return true;
            }
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    protected void initBatterySaver(Preference pref, BatteryManager batteryManager) {
        pref.setSummary(getString(
                R.string.pref_batterySaverMode_summary,
                batteryManager.getIntProperty(batteryManager.BATTERY_PROPERTY_CAPACITY)));

        // Remove battery saver setting if low power mode trigger level set to 0 and not using twm
        int defaultLevel = getResources().getInteger(R.integer.config_lowPowerModeTriggerLevel);
        if (defaultLevel == 0 && !BatterySaverUtil.useTwm(getContext())) {
            getPreferenceScreen().removePreference(pref);
        }
    }

    private void initRegulatoryInfo(Preference pref) {
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

    private void startActivitySafely(Intent intent) {
        if (isAdded()) {
            try {
                getActivity().startActivity(intent);
            } catch (ActivityNotFoundException e) {
                showToast(getString(R.string.error_could_not_launch));
            }
        }
    }

    private void showToast(String toastString) {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(getContext(), toastString, Toast.LENGTH_SHORT);
        mToast.show();
    }
}
