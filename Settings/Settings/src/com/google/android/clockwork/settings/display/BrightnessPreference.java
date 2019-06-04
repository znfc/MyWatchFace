package com.google.android.clockwork.settings.display;

import android.content.ContentResolver;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.preference.ListPreference;
import android.provider.Settings;
import android.util.AttributeSet;

import com.google.android.apps.wearable.settings.R;

import java.util.ArrayList;
import java.util.List;

/** Preference for adjusting brightness of the device. */
public class BrightnessPreference extends ListPreference {
    private static String AUTO_BRIGHTNESS_VALUE = "auto";

    public BrightnessPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BrightnessPreference(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        ContentResolver resolver = context.getContentResolver();

        setKey("pref_brightness");
        setTitle(R.string.pref_brightness);
        setIcon(R.drawable.ic_cc_settings_brightness);
        setDialogTitle(R.string.pref_brightness);
        setPersistent(false);
        setNegativeButtonText(null);

        // Set entries list.
        List<CharSequence> entries = new ArrayList<>();
        List<CharSequence> entryValues = new ArrayList<>();

        int[] brightnessLevels = context.getResources().getIntArray(R.array.brightness_levels);

        SensorManager sensorManager =
                (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        boolean hasAmbientLightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null;

        if (hasAmbientLightSensor) {
            entries.add(context.getText(R.string.adaptive_brightness));
            entryValues.add(AUTO_BRIGHTNESS_VALUE);
        }

        for (int i = 0; i < brightnessLevels.length; i++) {
            entries.add(String.valueOf(brightnessLevels.length - i));
            entryValues.add(String.valueOf(brightnessLevels[i]));
        }

        setEntries(entries.toArray(new String[entries.size()]));
        setEntryValues(entryValues.toArray(new String[entryValues.size()]));

        if (hasAmbientLightSensor
                && Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 0)
                        == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
            setValue(AUTO_BRIGHTNESS_VALUE);
        } else {
            // Get the closest value to the currently set brightness value and set as default.
            int brightness = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS, 0);
            int closestDelta = Integer.MAX_VALUE;
            int closestBrightness = 0;
            for (int i = 0; i < brightnessLevels.length; ++i) {
                int delta = Math.abs(brightness - brightnessLevels[i]);
                if (delta < closestDelta) {
                    closestDelta = delta;
                    closestBrightness = brightnessLevels[i];
                }
            }

            setValue(Integer.toString(closestBrightness));
        }

        // Add listener to change brightness in system settings.
        setOnPreferenceChangeListener((p, newVal) -> {
            if (AUTO_BRIGHTNESS_VALUE.equals(newVal)) {
                Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
            } else {
                Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS,
                        Integer.parseInt((String) newVal));
            }
            return true;
        });
    }
}
