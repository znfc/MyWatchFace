package com.google.android.clockwork.settings.display;

import android.content.res.Resources;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.support.annotation.VisibleForTesting;
import android.support.wearable.input.WearableButtons;
import android.support.wearable.preference.AcceptDenySwitchPreference;
import android.support.wearable.preference.PreferenceIconHelper;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.IWindowManager;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.WindowManagerGlobal;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.AmbientConfig;
import com.google.android.clockwork.settings.AmbientModeUtil;
import com.google.android.clockwork.settings.common.RadioButtonPreference;
import com.google.android.clockwork.settings.common.RadioGroupPreferenceScreenHelper;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;
import com.google.android.clockwork.settings.DefaultAmbientConfig;
import com.google.android.clockwork.settings.SettingsContract;

/**
 * Display settings.
 */
public class DisplaySettingsFragment extends SettingsPreferenceFragment {

    private static final String TAG = "DisplaySettingsFragment";
    private static final String KEY_PREF_WATCHFACE = "pref_watchface";
    private static final String KEY_PREF_TOUCH_DISABLE = "pref_wetMode";
    private static final String KEY_PREF_SCREEN_ORIENTATION = "pref_screenOrientation";
    private static final String KEY_PREF_SCREEN_ORIENTATION_LEFT_WRIST =
            "pref_screenOrientation_leftWrist";
    private static final String KEY_PREF_SCREEN_ORIENTATION_RIGHT_WRIST =
            "pref_screenOrientation_rightWrist";
    private static final String KEY_PREF_ALWAYS_ON_SCREEN = "pref_alwaysOnScreen";
    private static final String EXTRA_FROM_HOME_KEY = "android.intent.extra.FROM_HOME_KEY";

    private boolean mLeftyModeSupported;
    private AmbientConfig mAmbientConfig;
    private AcceptDenySwitchPreference mAlwaysOnScreenPref;
    private final AmbientConfig.AmbientConfigListener mAmbientConfigListener =
            this::updateAlwaysOnScreen;

    public DisplaySettingsFragment() {
        mLeftyModeSupported = (SystemProperties.getInt("config.enable_lefty", 0) != 0);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_display);
        addPreferencesFromResource(R.xml.prefs_display_customization);

        initWatchface(findPreference(KEY_PREF_WATCHFACE));
        initScreenOrientation((PreferenceScreen) findPreference(KEY_PREF_SCREEN_ORIENTATION));
        initAlwaysOnScreen((AcceptDenySwitchPreference) findPreference(KEY_PREF_ALWAYS_ON_SCREEN));
        if (WetModeReceiver.isWetModeEnabled(getContext())) {
            initTouchDisable(findPreference(KEY_PREF_TOUCH_DISABLE));
        } else {
            getPreferenceScreen().removePreference(findPreference(KEY_PREF_TOUCH_DISABLE));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAmbientConfig != null) { // Because Robolectric...
            mAmbientConfig.addListener(mAmbientConfigListener);
        }
    }

    @Override
    public void onPause() {
        if (mAmbientConfig != null) { // Because Robolectric...
            mAmbientConfig.removeListener(mAmbientConfigListener);
        }
        super.onPause();
    }

    protected void initWatchface(final Preference pref) {
        pref.setOnPreferenceClickListener((p) -> {
            getActivity().finishAffinity();
            getContext().startActivity(
                    new Intent(Intent.ACTION_MAIN)
                            .addCategory(Intent.CATEGORY_HOME)
                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            getContext().sendBroadcast(new Intent(SettingsContract.ACTION_ENTER_WFP));
            return true;
        });
    }

    @VisibleForTesting
    void setLeftyModeSupportedForTest(boolean supported) {
        mLeftyModeSupported = supported;
    }

    void updateScreenOrientationIcon(final PreferenceScreen pref) {
        Context context = getContext();
        try {
            pref.setIcon(
                    PreferenceIconHelper.wrapIcon(
                            context,
                            WearableButtons.getButtonIcon(context, KeyEvent.KEYCODE_STEM_PRIMARY)));
        } catch (Resources.NotFoundException e) {
            // Because Robolectric can't lookup resources from the Wearable Support Library
            Log.e(TAG, "Failed to update the icon", e);
        }
    }

    @VisibleForTesting
    void initScreenOrientation(final PreferenceScreen pref) {
        if (pref == null) {
            return;
        }

        if (mLeftyModeSupported) {
            ((RadioButtonPreference) pref.findPreference(KEY_PREF_SCREEN_ORIENTATION_LEFT_WRIST))
                    .setEntryValue(Surface.ROTATION_0);
            ((RadioButtonPreference) pref.findPreference(KEY_PREF_SCREEN_ORIENTATION_RIGHT_WRIST))
                    .setEntryValue(Surface.ROTATION_180);
            updateScreenOrientationIcon(pref);

            RadioGroupPreferenceScreenHelper helper = new RadioGroupPreferenceScreenHelper(pref);
            helper.enableAutoSummary(pref, null);
            helper.checkByEntryValue(
                    Settings.System.getInt(
                            getContext().getContentResolver(),
                            Settings.System.USER_ROTATION,
                            Surface.ROTATION_0));

            helper.setOnCheckedChangedListener(
                    (group, p) -> {
                        int rotation = p.getEntryValueInt();
                        IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
                        try {
                            wm.freezeRotation(rotation);
                        } catch (RemoteException e) {
                            Log.e(TAG, "Caught exception while rotating the screen", e);
                        }
                        updateScreenOrientationIcon(pref);
                    });
        } else {
            getPreferenceScreen().removePreference(pref);
        }
    }

    protected void initAlwaysOnScreen(final AcceptDenySwitchPreference pref) {

        if(getContext().getResources().getBoolean(R.bool.config_remove_aod_toggle)) {
            getPreferenceScreen().removePreference(pref);
            return;
        }

        try {
            mAmbientConfig = DefaultAmbientConfig.getInstance(getActivity());
        } catch (Exception e) {
            // Because Robolectric sets all values to "false" by default
            return;
        }

        if (pref == null) {
            return;
        }

        mAlwaysOnScreenPref = pref;
        updateAlwaysOnScreen();

        pref.setOnPreferenceChangeListener((p, newVal) -> {
            boolean checked = (Boolean) newVal;
            mAmbientConfig.setAmbientEnabled(checked);

            // Sync to wearable peers.
            AmbientModeUtil.syncAmbientEnabled(getActivity(), checked);
            return true;
        });
    }

    protected void initTouchDisable(final Preference pref) {

        pref.setOnPreferenceClickListener((p) -> {
            if (getContext() != null) {
                getActivity().finishAffinity();

                // This specific intent is used in order to ensure the Home App returns to the
                // watchface regardless of how settings was opened
                // (quick settings shade vs launcher), see onNewIntent() in HomeActivityBase.java
                getContext().startActivity(new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_HOME)
                        .putExtra(EXTRA_FROM_HOME_KEY, true)
                        .addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT));

                getContext().sendBroadcast(
                        new Intent(WetModeReceiver.ACTION_ENABLE_WET_MODE));
            }
            return true;
        });
    }

    private void updateAlwaysOnScreen() {
        if (mAlwaysOnScreenPref != null) {
            mAlwaysOnScreenPref.setChecked(mAmbientConfig.isAmbientEnabled());
        }
    }
}
