package com.google.android.clockwork.settings.wifi;

import com.google.android.apps.wearable.settings.R;

/**
 * TODO: Refactor SettingsAdapter to take Drawable as icon resource so that we can use
 * LevelListDrawable instead of this class.
 */
class WifiLevelIcons {
    static final int[][] WIFI_LEVEL_ICONS = {
        { R.drawable.ic_cc_settings_wifi_0,
          R.drawable.ic_cc_settings_wifi_1,
          R.drawable.ic_cc_settings_wifi_2,
          R.drawable.ic_cc_settings_wifi_3,
          R.drawable.ic_cc_settings_wifi_4, },
        { R.drawable.ic_cc_settings_wifi_secure_0,
          R.drawable.ic_cc_settings_wifi_secure_1,
          R.drawable.ic_cc_settings_wifi_secure_2,
          R.drawable.ic_cc_settings_wifi_secure_3,
          R.drawable.ic_cc_settings_wifi_secure_4, },
    };

    static final int WIFI_LEVEL_COUNT = WIFI_LEVEL_ICONS[0].length;
}
