package com.google.android.clockwork.settings.personal.buttons;

/**
 * Stem button constants
 */
public final class Constants {
    private Constants() {
        throw new RuntimeException("Constants should not be instantiated");
    }

    // Stem pref names
    public static final String PREF_STEM_1_TYPE = "STEM_1_TYPE";
    public static final String PREF_STEM_1_DATA = "STEM_1_DATA";
    public static final String PREF_STEM_1_DEFAULT_DATA = "STEM_1_DEFAULT_DATA";
    public static final String PREF_STEM_2_TYPE = "STEM_2_TYPE";
    public static final String PREF_STEM_2_DATA = "STEM_2_DATA";
    public static final String PREF_STEM_2_DEFAULT_DATA = "STEM_2_DEFAULT_DATA";
    public static final String PREF_STEM_3_TYPE = "STEM_3_TYPE";
    public static final String PREF_STEM_3_DATA = "STEM_3_DATA";
    public static final String PREF_STEM_3_DEFAULT_DATA = "STEM_3_DEFAULT_DATA";


    // Stem types
    public static final int STEM_TYPE_UNKNOWN = -1;
    public static final int STEM_TYPE_APP_LAUNCH = 0;
    public static final int STEM_TYPE_CONTACT_LAUNCH = 1;
}
