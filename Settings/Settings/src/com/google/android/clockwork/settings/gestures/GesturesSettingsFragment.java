package com.google.android.clockwork.settings.gestures;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.SwitchPreference;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.common.concurrent.Executors;
import com.google.android.clockwork.common.concurrent.WrappedCwRunnable;
import com.google.android.clockwork.common.emulator.EmulatorUtil;
import com.google.android.clockwork.remoteintent.RemoteIntentUtils;
import com.google.android.clockwork.settings.AmbientConfig;
import com.google.android.clockwork.settings.TiltToWakeUtil;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;
import com.google.android.clockwork.settings.DefaultAmbientConfig;
import com.google.android.clockwork.settings.WristGesturesConfig;
import com.google.android.clockwork.settings.WristGesturesTutorialContract;
import com.google.android.clockwork.settings.utils.FeatureManager;
import com.google.android.clockwork.tutorial.WristGestureTutorialConstants;

/**
 * Gestures settings.
 */
public class GesturesSettingsFragment extends SettingsPreferenceFragment {
    private static final String KEY_PREF_TOUCH_TO_WAKE = "pref_touchToWake";
    private static final String KEY_PREF_TILT_TO_WAKE = "pref_tiltToWake";
    private static final String KEY_PREF_WRIST_GESTURES = "pref_wristGestures";
    private static final String KEY_PREF_LAUNCH_TUTORIAL = "pref_launchTutorial";
    private static final String KEY_PREF_MORE_TIPS = "pref_moreTips";

    private static final String[] HIDDEN_PREFERENCES_LE = {KEY_PREF_MORE_TIPS};

    private AmbientConfig mAmbientConfig;
    private SwitchPreference mTiltToWakePref;
    private SwitchPreference mTouchToWakePref;
    private final AmbientConfig.AmbientConfigListener mAmbientConfigListener =
            this::updateTiltAndTouch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_gestures);
        addPreferencesFromResource(R.xml.prefs_gestures_customization);

        if (FeatureManager.INSTANCE.get(getContext()).isLocalEditionDevice()) {
            for (String key : HIDDEN_PREFERENCES_LE) {
                Preference pref = findPreference(key);
                if (pref != null) {
                    getPreferenceScreen().removePreference(pref);
                }
            }
        }

        initTiltToWake((SwitchPreference) findPreference(KEY_PREF_TILT_TO_WAKE));
        initTouchToWake((SwitchPreference) findPreference(KEY_PREF_TOUCH_TO_WAKE));
        initWristGestures((SwitchPreference) findPreference(KEY_PREF_WRIST_GESTURES));
        initLaunchTutorial((Preference) findPreference(KEY_PREF_LAUNCH_TUTORIAL));
        initMoreTips((Preference) findPreference(KEY_PREF_MORE_TIPS));
    }

    @Override
    public void onResume() {
        super.onResume();
        mAmbientConfig.addListener(mAmbientConfigListener);
    }

    @Override
    public void onPause() {
        mAmbientConfig.removeListener(mAmbientConfigListener);
        super.onPause();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        getActivity().finish();
    }

    protected void initTiltToWake(SwitchPreference pref) {
        if (pref == null) {
            return;
        }

        mAmbientConfig = DefaultAmbientConfig.getInstance(getActivity());
        mTiltToWakePref = pref;
        updateTiltToWake();

        pref.setOnPreferenceChangeListener((p, newVal) -> {
            final Boolean enabled = (Boolean) newVal;
            mAmbientConfig.setTiltToWake(enabled);
            // Sync to wearable peers.
            TiltToWakeUtil.syncTiltToWakeEnabled(getActivity(), enabled);
            return true;
        });
    }

    protected void initTouchToWake(SwitchPreference pref) {
        if (pref == null) {
            return;
        }

        if (EmulatorUtil.inEmulator()) {
            // Disabling touch not supported yet due to b/72399634.
            getPreferenceScreen().removePreference(pref);
            return;
        }

        mTouchToWakePref = pref;
        updateTouchToWake();

        pref.setOnPreferenceChangeListener((p, newVal) -> {
            final Boolean enabled = (Boolean) newVal;
            mAmbientConfig.setTouchToWake(enabled);
            return true;
        });
    }

    protected void initWristGestures(SwitchPreference pref) {
        if (pref == null) {
            return;
        }

        pref.setChecked(WristGesturesConfig.isWristGesturesEnabled(getContext()));
        pref.setOnPreferenceChangeListener((p, newVal) -> {
            final boolean enabled = (Boolean) newVal;
            Executors.INSTANCE.get(getContext()).getBackgroundExecutor().submit(
                    new WrappedCwRunnable("WristGesturesUpdate", () ->
                            WristGesturesConfig.setWristGesturesEnabled(getContext(), enabled)));
            return true;
        });
    }

    protected void initLaunchTutorial(Preference pref) {
        if (pref == null) {
            return;
        }

        pref.setOnPreferenceClickListener((p) -> {
            getActivity().sendBroadcastAsUser(
                    new Intent(WristGesturesTutorialContract.ACTION_LAUNCH_WRIST_GESTURE_TUTORIAL),
                    new UserHandle(UserHandle.USER_CURRENT));
            return true;
        });
    }

    protected void initMoreTips(Preference pref) {
        if (pref == null) {
            return;
        }

        pref.setOnPreferenceClickListener((p) -> {
            Intent openOnPhoneIntent = RemoteIntentUtils
                    .intentToOpenUriOnPhone(WristGestureTutorialConstants.TARGET_LINK);
            startActivityForResult(openOnPhoneIntent, 0);
            return true;
        });
    }

    private void updateTiltAndTouch() {
        updateTiltToWake();
        updateTouchToWake();
    }

    private void updateTiltToWake() {
        if (mTiltToWakePref != null) {
            mTiltToWakePref.setChecked(mAmbientConfig.isTiltToWake());
        }
    }

    private void updateTouchToWake() {
        if (mTouchToWakePref != null) {
            mTouchToWakePref.setChecked(mAmbientConfig.isTouchToWake());
        }
    }
}
