package com.google.android.clockwork.decomposablewatchface;

import android.os.Bundle;
import android.os.Parcel;

public abstract class DrawableComponent extends AbstractComponent {
    static final String FIELD_Z_ORDER = "z_order";
    static final String FIELD_BLINK_ENABLED = "blink_enabled";
    static final String FIELD_BLINK_PERIOD_ON_MS = "blink_period_on_ms";
    static final String FIELD_BLINK_PERIOD_OFF_MS = "blink_period_off_ms";
    static final String FIELD_BLINK_START_TIME_DAYS_SINCE_LOCAL_EPOCH =
            "blink_start_time_days_since_local_epoch";
    static final String FIELD_BLINK_START_TIME_MS_SINCE_MIDNIGHT =
            "blink_start_time_ms_since_midnight";

    DrawableComponent(Bundle fields) {
        super(fields);
    }

    DrawableComponent(Parcel in) {
        super(in);
    }

    /**
     * Returns the Z order of the component, which determines the order of drawing. Components with lower
     * order will be drawn before (i.e. below) components with higher order. This applies across both
     * image and number components.
     */
    /** @hide */
    public int getZOrder() {
        return fields.getInt(FIELD_Z_ORDER);
    }

    /**
     * Returns whether this component blinks.
     */
    /** @hide */
    public boolean getBlinkEnabled() {
        return fields.getBoolean(FIELD_BLINK_ENABLED);
    }

    /**
     * Returns how long this component should be visible when blinking "on"
     */
    /** @hide */
    public float getBlinkPeriodOnMs() {
        return fields.getFloat(FIELD_BLINK_PERIOD_ON_MS);
    }

    /**
     * Returns how long this component should be invisible when blinking "off"
     */
    /** @hide */
    public float getBlinkPeriodOffMs() {
        return fields.getFloat(FIELD_BLINK_PERIOD_OFF_MS);
    }

    /**
     * Returns time whence blinking should start "on" (relative to local 1-Jan-1970)
     */
    /** @hide */
    public int getBlinkStartTimeDaysSinceLocalEpoch() {
        return fields.getInt(FIELD_BLINK_START_TIME_DAYS_SINCE_LOCAL_EPOCH);
    }

    /**
     * Returns time whence blinking should start "on" (relative to local 1-Jan-1970)
     */
    /** @hide */
    public int getBlinkStartTimeMsSinceMidnight() {
        return fields.getInt(FIELD_BLINK_START_TIME_MS_SINCE_MIDNIGHT);
    }

    /** Builder for {@link DrawableComponent} objects. */
    public static abstract class Builder<T extends Builder> extends AbstractComponent.Builder<T> {
        /**
         * Sets the Z order of the component, which determines the order of drawing. Components with lower
         * order will be drawn before (i.e. below) components with higher order. This applies across both
         * image and number components.
         */
        public T setZOrder(int zOrder) {
            fields.putInt(FIELD_Z_ORDER, zOrder);
            return (T) this;
        }

        /**
         * Sets whether blinking is enabled, when, and periodicity.
         * @param isBlink Whether this component blinks
         * @param periodOnMs How long this component should be visible when blinking "on"
         * @param periodOffMs How long this component should be invisible when blinking "off"
         * @param startTimeDaysSinceLocalEpoch Whence blinking should start "on" (relative to
         *        1-Jan-1970 local time)
         * @param startTimeMsSinceMidnight Whence blinking should start "on" (relative to midnight)
         */
        public T setBlink(boolean enabled, float periodOnMs, float periodOffMs,
                int startTimeDaysSinceLocalEpoch, int startTimeMsSinceMidnight) {
            fields.putBoolean(FIELD_BLINK_ENABLED, enabled);
            fields.putFloat(FIELD_BLINK_PERIOD_ON_MS, periodOnMs);
            fields.putFloat(FIELD_BLINK_PERIOD_OFF_MS, periodOffMs);
            fields.putInt(FIELD_BLINK_START_TIME_DAYS_SINCE_LOCAL_EPOCH,
                    startTimeDaysSinceLocalEpoch);
            fields.putInt(FIELD_BLINK_START_TIME_MS_SINCE_MIDNIGHT, startTimeMsSinceMidnight);
            return (T) this;
        }
    }
}
