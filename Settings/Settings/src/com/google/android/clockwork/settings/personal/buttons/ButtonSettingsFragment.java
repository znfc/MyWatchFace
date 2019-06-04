package com.google.android.clockwork.settings.personal.buttons;

import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.wearable.input.WearableButtons;
import android.support.wearable.preference.WearablePreferenceActivity;
import android.support.wearable.preference.PreferenceIconHelper;
import android.text.TextUtils;
import android.view.KeyEvent;

import com.android.internal.annotations.VisibleForTesting;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.common.content.CwPrefs;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;

/**
 * Button settings.
 */
public class ButtonSettingsFragment extends SettingsPreferenceFragment {
    private static final String TAG = "ButtonSettingsFragment";

    public static final String ACTION_BUTTON_SETTINGS
            = "com.google.android.clockwork.settings.BUTTON_SETTINGS";

    private PackageManager mPackageManager;
    @VisibleForTesting Resources mResources;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = getContext();
        mPackageManager = context.getPackageManager();
        if (mResources == null) {
            mResources = context.getResources();
        }
        initPreferences();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshUi();
    }

    private void initPreferences() {
        Context context = getContext();
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);

        for (int i = 0; i < ButtonUtils.CONFIGURABLE_BUTTON_KEYCODES.length; ++i) {
            int keycode = ButtonUtils.CONFIGURABLE_BUTTON_KEYCODES[i];
            WearableButtons.ButtonInfo info = WearableButtons.getButtonInfo(context, keycode);
            if (info != null) {
                Preference pref = new Preference(context);
                pref.setKey(keycodeToPrefName(keycode));
                pref.setPersistent(false);
                pref.setOnPreferenceClickListener((p) -> {
                    Bundle args = new Bundle();
                    args.putInt(AppsListFragment.EXTRA_STEM_KEYCODE, keycode);
                    launchFragment(AppsListFragment.class, args);
                    return true;
                });

                CharSequence title = getOemButtonTitleOverride(keycode);

                if (TextUtils.isEmpty(title)) {
                    title = WearableButtons.getButtonLabel(context, keycode);
                }
                pref.setTitle(title);
                pref.setIcon(WearableButtons.getButtonIcon(context, keycode));
                // Wrap the icon
                PreferenceIconHelper.wrapIcon(pref);
                screen.addPreference(pref);
            }
        }

        screen.setTitle(getResources().getQuantityString(R.plurals.pref_buttons,
                WearableButtons.getButtonCount(context)));

        setPreferenceScreen(screen);
    }

    private void refreshUi() {
        for (int i = 0; i < ButtonUtils.CONFIGURABLE_BUTTON_KEYCODES.length; ++i) {
            int keycode = ButtonUtils.CONFIGURABLE_BUTTON_KEYCODES[i];
            Preference pref = getPreferenceScreen().findPreference(keycodeToPrefName(keycode));
            if (pref != null) {
                Context context = getContext();
                ButtonManager bm = new ButtonManager(context);

                pref.setSummary(bm.getFriendlySummary(keycode));
            }
        }
    }

    private String keycodeToPrefName(int keycode) {
        switch (keycode) {
            case KeyEvent.KEYCODE_STEM_1:
                return "PREF_STEM_1";
            case KeyEvent.KEYCODE_STEM_2:
                return "PREF_STEM_2";
            case KeyEvent.KEYCODE_STEM_3:
                return "PREF_STEM_3";
            default:
                throw new IllegalArgumentException("Unexpected keycode");
        }
    }


    private void launchFragment(Class<?> cls, Bundle args) {
        ((WearablePreferenceActivity) getActivity()).startPreferenceFragment(
                Fragment.instantiate(getActivity(), cls.getCanonicalName(), args), true);
    }

    private String getOemButtonTitleOverride(int keycode) {
        switch (keycode) {
            case KeyEvent.KEYCODE_STEM_1:
                return mResources.getString(R.string.button_location_description_stem_1);
            case KeyEvent.KEYCODE_STEM_2:
                return mResources.getString(R.string.button_location_description_stem_2);
            case KeyEvent.KEYCODE_STEM_3:
                return mResources.getString(R.string.button_location_description_stem_3);
            default:
                throw new IllegalArgumentException("Unexpected keycode");
        }
    }
}
