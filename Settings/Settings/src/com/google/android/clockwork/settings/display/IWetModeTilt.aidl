package com.google.android.clockwork.settings.display;

/**
 * Callback interface for Ambient TiltToWakeController to notify the WetModeService not to
 * End wet mode when the watch wakes up due to a tilt.
 */
interface IWetModeTilt {

    /** Tells the WetModeService to ignore the upcoming ACTION_SCREEN_ON */
    void incomingTiltToWake() = 0;

    /** Returns whether wet mode is on */
    boolean isWetModeEnabled() = 1;

}