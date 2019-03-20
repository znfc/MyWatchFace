package com.google.android.clockwork.sidekick;

import com.google.android.clockwork.decomposablewatchface.WatchFaceDecomposition;

interface ISidekickService
{
    /**
     * Tells Sidekick to reset its internal state.
     * @return Status (@see SidekickServiceConstants)
     */
    int clearWatchFace();

    /**
     * Sends a completely new watch face or updating components of a valid,
     * previously sent watchface.  When updating, all components in the
     * Decomposition must have IDs equal to the existing components that
     * they will replace.
     * @param shouldReplace Indicates WF completely replaces prior WF
     * @return Status (@see SidekickServiceConstants)
     */
    int sendWatchFace(in WatchFaceDecomposition watchFace, in boolean shouldReplace);

    /**
     * Sends a complete watchface for use in traditional watch mode.
     * @return Status (@see SidekickServiceConstants)
     */
    int sendWatchFaceForTWM(in WatchFaceDecomposition watchFace);

    /**
     * Returns true iff Sidekick exists and initialized without errors.
     */
    boolean sidekickExists();

    /**
     * Returns true iff Sidekick has a valid watch face (which is currently
     * visible) and is ready to take control.
     */
    boolean readyToDisplay();

    /**
     * Whether Sidekick should eventually take control of the display
     */
    void setShouldControlDisplay(in boolean visible);

    /**
     * Whether AOD is enabled.
     */
    void setAmbientEnabled(in boolean enabled);
}
