package com.google.android.clockwork.settings.connectivity;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.provider.Settings;

import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.Utils;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.common.emulator.EmulatorUtil;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;
import com.google.android.clockwork.settings.DefaultLocationConfig;
import com.google.android.clockwork.settings.LocationConfig;
import com.google.android.clockwork.settings.SettingsIntents;

import java.util.List;

/**
 * Connectivity settings.
 */
public class ConnectivitySettingsFragment extends SettingsPreferenceFragment {
    private static final String KEY_PREF_BLUETOOTH = "pref_bluetooth";
    private static final String KEY_PREF_WIFI = "pref_wifi";
    private static final String KEY_PREF_WIFI_OLD = "pref_wifi_old";
    private static final String KEY_PREF_CELLULAR = "pref_cellular";
    private static final String KEY_PREF_TWINNING = "pref_twinning";
    private static final String KEY_PREF_AIRPLANE_MODE = "pref_airplaneMode";
    private static final String KEY_PREF_LOCATION_TOGGLE = "pref_locationToggle";
    private static final String KEY_PREF_NFC = "pref_nfc";

    private static final String KEY_OPEN_LOCATION_MODE = "open_location_mode";

    public static ConnectivitySettingsFragment newInstance() {
        return new ConnectivitySettingsFragment();
    }

    public static ConnectivitySettingsFragment newInstance(boolean openLocationMode) {
        ConnectivitySettingsFragment f = newInstance();

        Bundle args = new Bundle();
        if (openLocationMode) {
            args.putInt(KEY_OPEN_LOCATION_MODE, 1);
        }
        f.setArguments(args);

        return f;
    }

    private ContentResolver mContentResolver;
    private PackageManager mPackageManager;
    private LocationConfig mLocationConfig;
    private LocationManager mLocationManager;

    private Preference mLocation;
    private SwitchPreference mLocationTogglePreference;

    private boolean mGps;
    private boolean mNetwork;

    private static final String TWINNING_INTENT =
        "com.google.android.clockwork.intent.TWINNING_SETTINGS";

    // Work-in-progress new wifi menu
    private static final boolean SHOW_NEW_WIFI_MENU = true;

    // Time to delay re-enabling APM preference after toggle
    private static final long APM_PREF_REENABLE_DELAY_MS = 2000;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_connectivity);
        addPreferencesFromResource(R.xml.prefs_connectivity_customization);

        mContentResolver = getActivity().getContentResolver();
        mPackageManager = getActivity().getPackageManager();
        mLocationManager = getActivity().getSystemService(LocationManager.class);
        mGps = mPackageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
        mNetwork = mPackageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_NETWORK);
        mLocationConfig = DefaultLocationConfig.getInstance(getContext());

        initBluetooth(findPreference(KEY_PREF_BLUETOOTH));
        initWifi(findPreference(KEY_PREF_WIFI), SHOW_NEW_WIFI_MENU);
        initWifi(findPreference(KEY_PREF_WIFI_OLD), false);
        initCellular(findPreference(KEY_PREF_CELLULAR));
        initTwinning(findPreference(KEY_PREF_TWINNING));
        initAirplaneMode((SwitchPreference) findPreference(KEY_PREF_AIRPLANE_MODE),
            new ApmCallback() {
                @Override
                public void callback(boolean checked) {
                    final Intent intent = new Intent(SettingsIntents.ACTION_CHANGE_AIRPLANE_MODE)
                            .setComponent(SettingsIntents.AIRPLANE_MODE_SERVICE_COMPONENT)
                            .putExtra(SettingsIntents.EXTRA_IS_AIRPLANE_MODE_ENABLED, checked);
                    getActivity().startServiceAsUser(intent, UserHandle.CURRENT_OR_SELF);
                }
            });
        initLocationToggle((SwitchPreference) findPreference(KEY_PREF_LOCATION_TOGGLE));
        initNfc(findPreference(KEY_PREF_NFC));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle args = getArguments();
        if (mLocationTogglePreference != null
                && args != null
                && args.getInt(KEY_OPEN_LOCATION_MODE, 0) == 1) {
            getListView().setSelection(mLocationTogglePreference.getOrder() - 1);
        }
    }

    protected void initBluetooth(Preference pref) {
        if (EmulatorUtil.inEmulator()) {
            getPreferenceScreen().removePreference(pref);
        }
    }

    protected void initWifi(Preference pref, boolean enable) {
        if (!enable || !mPackageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)) {
            getPreferenceScreen().removePreference(pref);
        }
    }

    protected void initCellular(Preference pref) {
        if (!com.google.android.clockwork.phone.Utils.isCurrentDeviceCellCapable(getActivity())) {
            getPreferenceScreen().removePreference(pref);
        }
    }

    /**
     * A callback to be used when the APM switch preference settings button has been changed.
      */
    public interface ApmCallback{
        void callback(final boolean checked);
    }

    protected void initAirplaneMode(SwitchPreference pref, ApmCallback apmCallback) {
        pref.setChecked(Settings.Global.getInt(
                mContentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1);
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            if (ActivityManager.isUserAMonkey()) {
                return true;
            }
            p.setEnabled(false);
            boolean checked = (Boolean) newVal;
            if (apmCallback != null) {
                apmCallback.callback(checked);
            }
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    pref.setEnabled(true);
                }
            }, APM_PREF_REENABLE_DELAY_MS);
            return true;
        });
    }

    protected void initNfc(Preference pref) {
        // Note: Using FEATURE_NFC_HOST_CARD_EMULATION not FEATURE_NFC - see b/24532713.
        if (!mPackageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)) {
            getPreferenceScreen().removePreference(pref);
        }
    }

    protected void initLocationToggle(SwitchPreference pref) {
        pref.setSummary((mGps || mNetwork) ? R.string.pref_location_summary_hasGps
                : R.string.pref_location_summary_noGps);
        pref.setChecked(mLocationManager.isLocationEnabled());
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            boolean enabled = (Boolean) newVal;
            if (enabled && checkLocationDisallowed()) {
                // Schedule uncheck as it doesn't work if done inline.
                new Handler().post(() -> {
                    pref.setChecked(false);
                });
                return true;
            }
            Utils.updateLocationEnabled(getActivity(), enabled, UserHandle.myUserId(),
                    Settings.Secure.LOCATION_CHANGER_SYSTEM_SETTINGS);
            mLocationConfig.setObtainPairedDeviceLocationEnabled(enabled);
            return true;
        });

        mLocationTogglePreference = pref;
    }

    private boolean checkLocationDisallowed() {
        final EnforcedAdmin admin = RestrictedLockUtils.checkIfRestrictionEnforced(getActivity(),
                UserManager.DISALLOW_SHARE_LOCATION, UserHandle.myUserId());
        if (admin != null) {
            RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getActivity(), admin);
            return true;
        }
        return false;
    }

    private void initTwinning(Preference pref) {
        List<ResolveInfo> targets = mPackageManager.queryIntentActivities(
                new Intent(TWINNING_INTENT), 0);
        // TODO: be smarter than just picking the first one
        final ResolveInfo twinningTarget = targets.size() > 0 ? targets.get(0) : null;
        if (twinningTarget != null) {
            twinningTarget.resolvePackageName = null;
            pref.setTitle(twinningTarget.loadLabel(mPackageManager));
            pref.setIntent(new Intent(TWINNING_INTENT)
                    .setComponent(new ComponentName(
                            twinningTarget.activityInfo.packageName,
                            twinningTarget.activityInfo.name)));
        } else {
            getPreferenceScreen().removePreference(pref);
        }
    }
}
