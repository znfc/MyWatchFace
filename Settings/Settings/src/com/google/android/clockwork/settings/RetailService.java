package com.google.android.clockwork.settings;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.utils.FeatureManager;
import java.util.List;

/**
  * A service that, upon receipt of ACTION_ENTER_RETAIL, sets up all the necessary retail
  * components.
  */
public class RetailService extends IntentService {
    private static final String TAG = "Clockwork.RetailService";

    private static final String ACTION_OEM_SETUP
            = "com.google.android.clockwork.action.RETAIL_OEM_SETUP";

    private static final String[] RETAIL_VOICE_ACTIONS = new String[] {
            "android.intent.action.VOICE_ASSIST",
            "android.intent.action.ASSIST",
            "android.speech.action.RECOGNIZE_SPEECH"
    };

    /**
     * Priority of voice provider activities that needs to be disabled to activiate retail
     * activities.
     */
    private static final int DEFAULT_VOICE_PROVIDER_PRIORITY = -1;

    private static final ComponentName HOME_ACTIVITY_COMPONENT = new ComponentName(
            "com.google.android.wearable.app",
            "com.google.android.clockwork.home2.activity.HomeActivity2");

    private static final ComponentName VOICE_ACTIVITY_COMPONENT_RETAIL = new ComponentName(
            "com.google.android.googlequicksearchbox",
            "com.google.android.apps.gsa.binaries.clockwork.retail.RetailVoicePlateActivity");

    private static final ComponentName REMOTE_SPEECH_ACTIVITY_COMPONENT = new ComponentName(
            "com.google.android.googlequicksearchbox",
            "com.google.android.apps.gsa.binaries.clockwork.remote.RemoteInputSpeechActivity");

    private static FeatureManager mFeatureManager;

    public RetailService() {
        super("RetailService");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mFeatureManager = FeatureManager.INSTANCE.get(this);
        mFeatureManager.initialize(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        if (SettingsIntents.ACTION_ENTER_RETAIL.equals(action)) {
            enterRetail();
        }
    }

    private void enterRetail() {
        Log.w(TAG, "device is entering retail mode");

        // set retail time
        startService(SettingsIntents.getSetTimeZoneIntent("UTC"));
        RetailTimeReceiver.setRetailTime(this);

        // turn off bluetooth
        BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
        if (bt != null) {
            bt.disable();
        }

        // Set theater mode to off
        Settings.Global.putInt(getContentResolver(), Settings.Global.THEATER_MODE_ON, 0);

        // set the retail mode bit in our settings provider
        ContentValues values = new ContentValues();
        values.put(SettingsContract.KEY_RETAIL_MODE, SettingsContract.RETAIL_MODE_RETAIL);
        getContentResolver().update(SettingsContract.RETAIL_MODE_URI, values, null, null);

        triggerOemSetup(this);

        disableHomeActivity(this);
        disableVoiceActivity(this);
        disableRemoteSpeechActivity(this);
        setUpVoiceProviderLe(this);
        disableChargingActivity(this);
        setBrightness(this);
        turnOnAirplaneMode(this);
        turnOffGestures(this);
        enableRetailStatusService(this);
    }

    /** Trigger OEM setup since we're not running settings sync. */
    private static void triggerOemSetup(Context context) {
        context.sendBroadcast(new Intent(ACTION_OEM_SETUP));
    }

    /** Disable HomeActivity to put Home into retail mode. */
    private static void disableHomeActivity(Context context) {
        PackageManager manager = context.getPackageManager();
        manager.setComponentEnabledSetting(HOME_ACTIVITY_COMPONENT,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }

    /**
     * Disabling all Activities for ACTION_VOICE_ASSIST other than RetailVoicePlateActivity
     * to put Voice into retail mode.
     */
    private static void disableVoiceActivity(Context context) {
        if (!mFeatureManager.isLocalEditionDevice()) {
            PackageManager pm = context.getPackageManager();
            for (ResolveInfo info : pm.queryIntentActivities(new Intent(Intent.ACTION_VOICE_ASSIST),
                    PackageManager.MATCH_DEFAULT_ONLY)) {
                ComponentName cn = info.getComponentInfo().getComponentName();
                if (!cn.equals(VOICE_ACTIVITY_COMPONENT_RETAIL)) {
                    pm.setComponentEnabledSetting(
                            info.getComponentInfo().getComponentName(),
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP);
                }
            }
        }
    }

    /** Disable RemoteSpeechActivity to put Speech input into retail mode. */
    private static void disableRemoteSpeechActivity(Context context) {
        if (!mFeatureManager.isLocalEditionDevice()) {
            context.getPackageManager()
                    .setComponentEnabledSetting(
                            REMOTE_SPEECH_ACTIVITY_COMPONENT,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP);
        }
    }

    /** Enable only one voice provider and activate its retail activities. */
    private static void setUpVoiceProviderLe(Context context) {
        if (!mFeatureManager.isLocalEditionDevice()) {
            return;
        }
        PackageManager pm = context.getPackageManager();
        String[] voiceProviders =
                context.getResources()
                        .getStringArray(R.array.config_le_system_voice_assistant_packages);
        disableAlternativeVoiceProvidersLe(pm, voiceProviders);
        disableActivitiesForRetailModeLe(pm);
    }

    /** Disable all pre-integrated voice providers except the first one. */
    @VisibleForTesting
    static void disableAlternativeVoiceProvidersLe(
            PackageManager pm, String[] voiceProviders) {
        String retailProvider = null;
        for (String voiceProvider : voiceProviders) {
            if (packageExists(pm, voiceProvider)) {
                if (retailProvider == null) {
                    Log.i(TAG, "Using voice provider '" + voiceProvider + "' for Retail mode");
                    retailProvider = voiceProvider;
                    pm.setApplicationEnabledSetting(
                            retailProvider,
                            pm.COMPONENT_ENABLED_STATE_ENABLED,
                            PackageManager.DONT_KILL_APP);
                } else {
                    Log.i(TAG, "Disabling voice provider '" + voiceProvider + "'");
                    pm.setApplicationEnabledSetting(
                            voiceProvider,
                            pm.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP);
                }
            } else {
                Log.e(TAG, "Pre-integrated voice provider '" + voiceProvider + "' not installed");
            }
        }
    }

    /** Disable high priority voice activities to activate retail activities. */
    @VisibleForTesting
    static void disableActivitiesForRetailModeLe(PackageManager pm) {
        for (String voiceAction : RETAIL_VOICE_ACTIONS) {
            Intent intent = new Intent(voiceAction);
            for (ResolveInfo resolveInfo :
                    pm.queryIntentActivities(intent, pm.MATCH_DEFAULT_ONLY)) {
                if (resolveInfo.priority == DEFAULT_VOICE_PROVIDER_PRIORITY) {
                    Log.i(TAG, "Disabling voice activity: " + resolveInfo.toString());
                    ComponentName componentName =
                            new ComponentName(
                                    resolveInfo.activityInfo.packageName,
                                    resolveInfo.activityInfo.name);
                    pm.setComponentEnabledSetting(
                            componentName,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP);
                }
            }
        }
    }

    private static boolean packageExists(PackageManager pm, String packageName) {
        try {
            pm.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /** Find and disable charging activities for retail mode. */
    private static void disableChargingActivity(Context context) {
        PackageManager manager = context.getPackageManager();
        List<ResolveInfo> activities = manager.queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_DESK_DOCK),
                PackageManager.MATCH_ALL);
        for (ResolveInfo activity : activities) {
            ActivityInfo activityInfo = activity.activityInfo;
            String packageName = activityInfo.packageName;
            if (!packageName.equals(HOME_ACTIVITY_COMPONENT.getPackageName())) {
                manager.setComponentEnabledSetting(
                        new ComponentName(packageName, activityInfo.name),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
            }
        }
    }

    /** Enable RetailStatusService for external clients to check if the device is in retail mode. */
    private static void enableRetailStatusService(Context context) {
        PackageManager manager = context.getPackageManager();
        manager.setComponentEnabledSetting(
                new ComponentName(context, RetailStatusService.class),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }

    /** Set brightness in retail mode. */
    private static void setBrightness(Context context) {
        Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS,
                context.getResources().getInteger(R.integer.config_retailModeBrightness));
    }

    /** Enable airplane mode. */
    private static void turnOnAirplaneMode(Context context) {
        Settings.System.putInt(context. getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 1);
    }

    /** Turn off gestures. */
    private static void turnOffGestures(Context context) {
        ContentValues values = new ContentValues();
        values.put(SettingsContract.KEY_WRIST_GESTURES_ENABLED, 0);
        context.getContentResolver().update(
                SettingsContract.WRIST_GESTURES_ENABLED_URI, values, null, null);
    }
}
