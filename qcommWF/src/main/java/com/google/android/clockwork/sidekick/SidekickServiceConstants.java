package com.google.android.clockwork.sidekick;

public class SidekickServiceConstants {
    private SidekickServiceConstants() {}

    /** @hide */
    public static final String NAME = "SystemSidekickService";

    /** @hide */
    public static final String ACTION_FINISH_TWM =
            "com.google.android.clockwork.sidekick.ACTION_FINISH_TWM";

    public static final int RESULT_OK                   = 0;
    public static final int RESULT_UNKNOWN_ERROR        = 1;
    public static final int RESULT_INVALID_ARGUMENT     = 2;
    public static final int RESULT_INSUFFICIENT_SPACE   = 3;

    /** @hide */
    public static final String ACTION_DECOMPOSITION_PACKAGE =
            "com.google.android.clockwork.sidekick.ACTION_DECOMPOSITION_PACKAGE";

    public static final String EXTRA_OLD_PACKAGE = "old_package";
    public static final String EXTRA_NEW_PACKAGE = "new_package";
}
