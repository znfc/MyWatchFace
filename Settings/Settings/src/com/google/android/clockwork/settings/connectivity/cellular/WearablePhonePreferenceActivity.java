package com.google.android.clockwork.settings.connectivity.cellular;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.wearable.preference.WearablePreferenceActivity;

/**
 * Wearable phone preference activity
 */
public class WearablePhonePreferenceActivity extends WearablePreferenceActivity {
    /**
     * Build an Intent to launch a new activity showing the selected fragment. The default
     * implementation constructs an Intent that re-launches the current activity with the
     * appropriate arguments to display the fragment.
     *
     * @param fragmentName The name of the fragment to display.
     * @param args Optional arguments to supply to the fragment.
     * @param titleRes Optional resource ID of title to show for this item.
     * @return Returns an Intent that can be launched to display the given fragment.
     */
    @Override
    public Intent onBuildStartFragmentIntent(String fragmentName, Bundle args, int titleRes) {
        return new Intent(Intent.ACTION_MAIN)
                .setClass(this, WearablePhonePreferenceActivity.class)
                .putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, fragmentName)
                .putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS, args)
                .putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_TITLE, titleRes)
                .putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
    }
}
