package com.google.android.clockwork.settings.display;

import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.RemoteException;
import android.preference.ListPreference;
import android.support.annotation.VisibleForTesting;
import android.util.AttributeSet;
import android.util.Log;

import java.util.Arrays;

import com.google.android.apps.wearable.settings.R;

/** Preference for adjusting the font size of the device. */
public class FontSizePreference extends ListPreference {
    private static final String TAG = "FontSizePref";

    interface FontManager {
        public Configuration getInitialConfiguration();
        public void updateConfiguration(Configuration configuration);
    }

    private static final FontManager FONT_MANAGER = new FontManager() {
        @Override
        public Configuration getInitialConfiguration() {
            Configuration currentConfig = new Configuration();
            try {
                Configuration initConfig = ActivityManagerNative.getDefault().getConfiguration();
                if (initConfig != null) {
                    currentConfig.updateFrom(initConfig);
                }
            } catch (RemoteException re) {
                Log.e(TAG, "error obtaining font size", re);
            }
            return currentConfig;
        }

        @Override
        public void updateConfiguration(Configuration currentConfig) {
            try {
                ActivityManagerNative.getDefault().updatePersistentConfiguration(currentConfig);
            } catch (RemoteException re) {
                Log.e(TAG, "error setting font size", re);
            }
        }
    };

    public FontSizePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context.getResources(), FONT_MANAGER);
    }

    public FontSizePreference(Context context) {
        super(context);
        init(context.getResources(), FONT_MANAGER);
    }

    @VisibleForTesting FontSizePreference(Context context, Resources res, FontManager manager) {
        super(context);
        init(res, manager);
    }

    @VisibleForTesting void init(Resources res, FontManager manager) {
        setKey("pref_fontSize");
        setTitle(R.string.pref_fontSize);
        setIcon(R.drawable.ic_cc_settings_textsize);

        final String[] textSizeLabels =
                res.getStringArray(R.array.text_size_labels);
        final String[] textSizeEntries =
                res.getStringArray(R.array.text_size_entries);
        int textSizeNum = Math.min(textSizeLabels.length, textSizeEntries.length);

        setDialogTitle(R.string.pref_fontSize);
        setEntries(Arrays.copyOfRange(textSizeLabels, 0, textSizeNum));
        setEntryValues(Arrays.copyOfRange(textSizeEntries, 0, textSizeNum));
        setPersistent(false);
        setNegativeButtonText(null);

        final Configuration currentConfig = manager.getInitialConfiguration();

        float closestDelta = Float.MAX_VALUE;
        int closestIndex = 0;
        for (int i = 0; i < textSizeNum; ++i) {
            float delta = Math.abs(currentConfig.fontScale - Float.parseFloat(textSizeEntries[i]));
            if (delta < closestDelta) {
                closestDelta = delta;
                closestIndex = i;
            }
        }

        setValue(textSizeEntries[closestIndex]);
        setSummary(textSizeLabels[closestIndex].toUpperCase());

        setOnPreferenceChangeListener((p, newVal) -> {
            String fontSizeString = (String) newVal;

            currentConfig.fontScale = Float.parseFloat(fontSizeString);
            setSummary(textSizeLabels[findIndexOfValue(fontSizeString)].toUpperCase());
            manager.updateConfiguration(currentConfig);
            return true;
        });
    }
}
