package com.google.android.clockwork.settings.personal.buttons;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.host.GKeys;
import com.google.android.clockwork.settings.common.SettingsPreferenceFragment;
import com.google.android.clockwork.settings.SettingsIntents;
import com.google.android.clockwork.settings.utils.FeatureManager;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles setting the default launch behavior for a button with an app launch
 */
public class AppsListFragment extends SettingsPreferenceFragment {
    private static final String TAG = "AppsListFragment";

    public static final String EXTRA_STEM_KEYCODE = "stem_keycode";

    private PackageManager mPackageManager;
    private FeatureManager mFeatureManager;
    private PreferenceScreen mPreferenceScreen;
    private GoogleApiClient mApiClient;
    private ButtonManager mButtonManager;
    private int mStemKeycode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getContext();
        mPackageManager = context.getPackageManager();
        mFeatureManager = FeatureManager.INSTANCE.get(context);
        mPreferenceScreen = getPreferenceManager().createPreferenceScreen(context);
        mPreferenceScreen.setTitle(R.string.pref_buttons_launch_app);

        Bundle args = getArguments();
        if (args != null && args.containsKey(EXTRA_STEM_KEYCODE)) {
            mStemKeycode = args.getInt(EXTRA_STEM_KEYCODE);
        } else {
            throw new IllegalArgumentException("Missing associated stem button");
        }

        mButtonManager = new ButtonManager(context);

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> appList = mPackageManager.queryIntentActivities(mainIntent, 0);
        List<Preference> prefList = new ArrayList<>(appList.size());
        for (int i = 0; i < appList.size(); ++i) {
            ActivityInfo info = appList.get(i).activityInfo;
            if (shouldHideFromList(info, context)) {
                continue;
            }

            Preference pref = new Preference(context);
            pref.setKey(info.getComponentName().flattenToString());
            pref.setPersistent(false);
            pref.setTitle(info.loadLabel(mPackageManager));
            pref.setIcon(info.loadIcon(mPackageManager));
            pref.setOnPreferenceClickListener((p) -> {
                // Set the pref and finish the fragment
                savePref(p);
                endFragment();
                return true;
            });
            prefList.add(pref);
        }

        // Sort by title
        Collections.sort(prefList);
        for (int i = 0; i < prefList.size(); ++i) {
            mPreferenceScreen.addPreference(prefList.get(i));
        }

        setPreferenceScreen(mPreferenceScreen);

        mApiClient = new GoogleApiClient.Builder(getContext()).addApi(Wearable.API).build();
        mApiClient.connect();
    }

    private boolean shouldHideFromList(ActivityInfo info, Context context) {
        ComponentName componentName = new ComponentName(info.packageName, info.name);

        return mFeatureManager.shouldHideComponent(info)
                || mFeatureManager.isAppPackageBlacklisted(context, info.packageName)
                || mFeatureManager.isAppComponentNameBlacklisted(context, componentName);
    }

    private void endFragment() {
        getActivity().finish();
    }

    private void savePref(Preference p) {
        mButtonManager.saveButtonSettings(mStemKeycode, Constants.STEM_TYPE_APP_LAUNCH, p.getKey());

        // Tell Settings to update its value for "Pay on stem".
        getContext().startService(SettingsIntents.getPayOnStemIntent(
                mButtonManager.isPayConfiguredOnStem()));
    }
}
