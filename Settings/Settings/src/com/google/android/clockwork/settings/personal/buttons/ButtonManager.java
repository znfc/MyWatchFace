package com.google.android.clockwork.settings.personal.buttons;

import com.google.android.clockwork.settings.MainSettingsActivity;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.view.KeyEvent;

import com.google.android.clockwork.host.GKeys;
import com.google.android.clockwork.settings.DefaultSettingsContentResolver;
import com.google.android.clockwork.settings.SettingsContentResolver;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.apps.wearable.settings.R;

import java.util.List;

/**
 * Provides information about the intent set for stem buttons.
 */
public class ButtonManager {
    private static final String TAG = "ButtonManager";

    public static final String BUTTON_MANAGER_CONFIG_PATH = "button_manager_config";

    private final Context mContext;
    private final Resources mResources;
    private final PackageManager mPackageManager;
    private final ContentResolver mContentResolver;

    public ButtonManager(Context context) {
        this(context, context.getContentResolver(), context.getResources(),
                context.getPackageManager());
    }

    @VisibleForTesting ButtonManager(Context context, ContentResolver resolver, Resources resources,
            PackageManager packageManager) {
        mContentResolver = resolver;
        mContext = context;
        mResources = resources;
        mPackageManager = packageManager;
    }

    public Intent getIntentForButton(int keycode, @Nullable String oldComponent,
            @Nullable String newComponent) {
        Intent intent = getIntentForButton(keycode);
        if (oldComponent != null && newComponent != null
                && doesMatchComponent(intent, oldComponent)) {
            Intent newIntent = getIntentFromComponentName(newComponent);
            if (newIntent != null) {
                Log.d(TAG, "Launching new intent instead: " + newIntent);
                intent = newIntent;
            }
        }
        return intent;
    }

    private boolean doesMatchComponent(Intent intent, String component) {
        return component.equals(intent.getComponent().flattenToString());
    }

    private Intent getIntentForButton(int keycode) {
        Intent savedIntent = getSavedIntent(mContext, keycode);
        if (isCallable(savedIntent)) {
            return savedIntent;
        } else {
            // Only try default intents if there is no saved intent.
            if (savedIntent == null) {
                Intent defaultIntent = getDefaultIntent(keycode);
                if (isCallable(defaultIntent)) {
                    Log.w(TAG, "Saved intent is not callable.  Using default.");
                    return defaultIntent;
                }
            }

            // Fallback is to go to button customization screen
            if (savedIntent == null) {
                Log.w(TAG, "Saved and default intents are not callable.  Using fallback.");
            } else {
                Log.w(TAG, "Saved intent is not callable.  Using fallback.");
            }
            Intent buttonSettingsIntent = new Intent(mContext, MainSettingsActivity.class);
            buttonSettingsIntent.setAction(ButtonSettingsFragment.ACTION_BUTTON_SETTINGS);
            return buttonSettingsIntent;
        }
    }

    // TODO(29424310): Update API to allow all apps to see if they're configured.  For now,
    //                 special case Pay.
    public boolean isPayConfiguredOnStem() {
        for (int keycode : ButtonUtils.CONFIGURABLE_BUTTON_KEYCODES) {
            Intent intent = getIntentForButton(keycode);
            if (intent != null
                    && intent.getComponent().flattenToString().equals(
                    GKeys.PAY_COMPONENT_NAME.get())) {
                return true;
            }
        }
        return false;
    }

    public String getFriendlySummary(int buttonKeycode) {
        int type = getButtonTypeFromPref(buttonKeycode);
        switch (type) {
            case Constants.STEM_TYPE_CONTACT_LAUNCH:
                throw new IllegalArgumentException("Contact launch not implemented yet");
            case Constants.STEM_TYPE_APP_LAUNCH:
                // Fall through to default
            default:
                Intent intent = getIntentForButton(buttonKeycode);
                ComponentName componentName  = intent.getComponent();

                if (componentName != null
                        && !ButtonSettingsFragment
                        .ACTION_BUTTON_SETTINGS.equals(intent.getAction())) {

                    ActivityInfo info = null;
                    try {
                        info = mPackageManager.getActivityInfo(
                                componentName, PackageManager.GET_META_DATA);
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.w(TAG, e.toString());
                    }
                    return info != null ?
                            String.valueOf(info.loadLabel(mPackageManager))
                            : componentName.getPackageName();
                }
        }

        return "";
    }

    public void saveButtonSettings(int buttonKeycode, int buttonType, String buttonData) {
        Uri settingsPath = getSettingsPath();
        SettingsContentResolver resolver =
                new DefaultSettingsContentResolver(mContentResolver);
        resolver.putIntValueForKey(
                settingsPath, ButtonUtils.getStemTypeKey(buttonKeycode), buttonType);
        resolver.putStringValueForKey(
                settingsPath, ButtonUtils.getStemDataKey(buttonKeycode), buttonData);
    }

    private boolean isCallable(Intent intent) {
        if (intent == null) {
            return false;
        }

        List<ResolveInfo> list = mPackageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    private Intent getSavedIntent(Context context, int buttonKeycode) {
        int type = getButtonTypeFromPref(buttonKeycode);
        switch (type) {
            case Constants.STEM_TYPE_APP_LAUNCH:
                String componentName = new DefaultSettingsContentResolver(
                        context.getContentResolver()).getStringValueForKey(
                                getSettingsPath(),
                                ButtonUtils.getStemDataKey(buttonKeycode),
                                null);
                return getIntentFromComponentName(componentName);
            case Constants.STEM_TYPE_CONTACT_LAUNCH:
                throw new IllegalArgumentException("Contact launch not implemented yet");
            default:
                return null;
        }
    }

    private Intent getDefaultIntent(int buttonKeycode) {
        String componentName;

        switch (buttonKeycode) {
            case KeyEvent.KEYCODE_STEM_1:
                componentName = mResources.getString(R.string.config_defaultStem1ComponentName);
                break;
            case KeyEvent.KEYCODE_STEM_2:
                componentName = mResources.getString(R.string.config_defaultStem2ComponentName);
                break;
            case KeyEvent.KEYCODE_STEM_3:
                componentName = mResources.getString(R.string.config_defaultStem3ComponentName);
                break;
            default:
                throw new IllegalArgumentException("Invalid stem id: " + buttonKeycode);
        }

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Default component name: " + componentName);
        }

        return getIntentFromComponentName(componentName);
    }

    private Intent getIntentFromComponentName(String componentName) {
        Intent intent = null;

        if (componentName != null) {
            ComponentName launcher = ComponentName.unflattenFromString(componentName);

            ActivityInfo info = null;
            try {
                info = mPackageManager.getActivityInfo(launcher, PackageManager.GET_META_DATA);
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, e.toString());
            }

            if (info != null) {
                intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                intent.setComponent(launcher);
            }
        }
        return intent;
    }

    private static Uri getSettingsPath() {
        return new Uri.Builder().scheme("content").authority(SettingsContract.SETTINGS_AUTHORITY)
                .path(BUTTON_MANAGER_CONFIG_PATH).build();
    }

    private int getButtonTypeFromPref(int buttonKeycode) {
        return new DefaultSettingsContentResolver(mContentResolver)
                .getIntValueForKey(
                        getSettingsPath(),
                        ButtonUtils.getStemTypeKey(buttonKeycode),
                        Constants.STEM_TYPE_UNKNOWN);
    }
}

