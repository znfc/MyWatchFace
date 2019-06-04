package com.google.android.clockwork.settings.cellular;

/**
 * cellular settings constants
 */
public final class Constants {
    private Constants() {
        throw new RuntimeException("Constants should not be instantiated");
    }

    public static final String EXTRA_NEW_LOCK_SIM_STATE = "new_lock_sim_state";
    public static final String EXTRA_RESULT_RECEIVER = "result_receiver";
    public static final String EXTRA_IS_PUK_PIN = "is_puk_pin";
    public static final int MIN_SIM_PIN_LENGTH = 4;
    public static final int MAX_SIM_PIN_LENGTH = 8;
    public static final int PUK_PIN_LENGTH = 8;
}