package com.google.android.clockwork.settings;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IPowerManager;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.support.wearable.activity.WearableActivity;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AlignmentSpan;
import android.util.Log;

import android.view.accessibility.AccessibilityManager;

import com.google.android.apps.wearable.settings.R;

public final class BatterySaverUtil {
    private static final String TAG = "BatterySaverUtil";

    private static final String TWM_HARDWARE_FEATURE =
            "com.google.clockwork.hardware.traditional_watch_mode";

    /**
     * Command low battery shutdown to enter Traditional Watch Mode.
     */
    static void triggerLowBatteryShutdown(Context context) {
        if (!useTwm(context)) {
            return;
        }
        Log.d(TAG, "Requesting low battery shutdown to start TWM");
        IBinder b = ServiceManager.getService("power");
        IPowerManager service = IPowerManager.Stub.asInterface(b);
        try {
            service.shutdown(false, PowerManager.SHUTDOWN_LOW_BATTERY, false);
        } catch (RemoteException e) {
            // Couldn't do anything.
        }
    }

    /**
     * Whether or not the TWM hardware feature is present.
     */
    public static boolean useTwm(Context context) {
        return context.getPackageManager().hasSystemFeature(TWM_HARDWARE_FEATURE);
    }

    /**
     * Enter Battery Saver Mode and start TWM if possible.
     */
    public static void startBatterySaver(boolean enable, Context context,
            PowerManager powerManager) {
        if (enable && useTwm(context)) {
            triggerLowBatteryShutdown(context);
        } else {
            powerManager.setPowerSaveMode(enable);
        }
    }

    /**
     * Invoked when TalkBack is toggled. Disables automatic power saver when TalkBack is enabled on
     * watches without a speaker. Enables automatic power saver when TalkBack is disabled on these
     * watches.  If using TWM then don't change the setting.
     *
     * @param talkbackEnabled the new state of TalkBack
     */
    public static void onTalkbackChanged(Context context, boolean talkbackEnabled) {
        if (!hasSpeaker(context) && !BatterySaverUtil.useTwm(context)) {
            // Disable automatic power saver when TalkBack is enabled and vice versa.
            boolean powerSaveEnabled = !talkbackEnabled;
            configurePowerSaverMode(context, powerSaveEnabled, talkbackEnabled);
        }
    }

    /**
     * Enables or disables automatic power saver. When automatic power saver is enabled, the watch
     * enters power saver mode when the battery level reaches some threshold. If TalkBack is enabled
     * and the watch doesn't have a speaker, automatic power saver is always disabled.
     */
    public static void configurePowerSaverMode(Context context, boolean powerSaveEnabled) {
        configurePowerSaverMode(context, powerSaveEnabled, isTalkbackEnabled(context));
    }

    private static void configurePowerSaverMode(Context context, boolean powerSaveEnabled,
            boolean talkbackEnabled) {
        powerSaveEnabled &= !talkbackEnabled || hasSpeaker(context);
        int defaultLevel =
                context.getResources().getInteger(R.integer.config_lowPowerModeTriggerLevel);
        int triggerLevel = powerSaveEnabled ? defaultLevel : 0;
        Log.d(TAG, "Changing automatic battery save trigger level to " + triggerLevel + "%");
        Settings.Global.putInt(
                context.getContentResolver(),
                Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL,
                // Used instead of GKeys.BATTERY_NOTIFICATION_LOW_THRESHOLD which is deprecated
                triggerLevel);
    }

    public static void disablePowerSaverModeTwm(Context context) {
        Log.d(TAG, "Disabling power saver mode in favor of twm");
        Settings.Global.putInt(
            context.getContentResolver(),
            Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL,
            0);
    }

    public static boolean isPowerSaverModeInitialized(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL, Integer.MIN_VALUE)
                != Integer.MIN_VALUE;
    }

    /**
     * Returns whether TalkBack is currently enabled. Note that there is some latency after TalkBack
     * is toggled before this method's result changes.
     */
    public static boolean isTalkbackEnabled(Context context) {
        AccessibilityManager accessibilityManager =
                context.getSystemService(AccessibilityManager.class);
        return accessibilityManager != null
                && accessibilityManager.isEnabled()
                && accessibilityManager.isTouchExplorationEnabled();
    }

    /** Returns whether the watch has a speaker. */
    public static boolean hasSpeaker(Context context) {
        PackageManager packageManager = context.getPackageManager();
        // FEATURE_AUDIO_OUTPUT indicates a speaker or headphone jack, not Bluetooth audio.
        return packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_OUTPUT);
    }

    public static SpannableString getSaverDialogMessage(Context context) {
        String messageText = context.getString(BatterySaverUtil.useTwm(context) ?
            R.string.pref_batterySaverScr_dialogMessage
            : R.string.pref_batterySaver_dialogMessage);
        SpannableString dialogMessage = new SpannableString(messageText);
        dialogMessage.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_NORMAL),
            0, dialogMessage.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return dialogMessage;
    }

    static public class SpinnerOverlayActivity extends WearableActivity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.spinner_overlay_activity);
        }
    }
}
