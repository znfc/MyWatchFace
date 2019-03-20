package com.google.android.clockwork.decomposablewatchface;

import android.graphics.PointF;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class NumberComponent extends DrawableComponent {

    /** @hide */
    public static final Creator<NumberComponent> CREATOR =
            new Creator<NumberComponent>() {
                @Override
                public NumberComponent createFromParcel(Parcel source) {
                    return new NumberComponent(source);
                }

                @Override
                public NumberComponent[] newArray(int size) {
                    return new NumberComponent[size];
                }
            };

    private static final String FIELD_MS_PER_INCREMENT = "ms_per_increment";
    private static final String FIELD_LOWEST_VALUE = "lowest_value";
    private static final String FIELD_HIGHEST_VALUE = "highest_value";
    private static final String FIELD_TIME_OFFSET_MS = "time_offset_ms";
    private static final String FIELD_MIN_DIGITS_SHOWN = "min_digits_shown";
    private static final String FIELD_POSITION = "position";
    private static final String FIELD_FONT_COMPONENT_ID = "font_component_id";

    private NumberComponent(Bundle fields) {
        super(fields);
    }

    private NumberComponent(Parcel in) {
        super(in);
    }

    /**
     * Returns the unit of time represented by the number, i.e. the amount of time after which the
     * number will be incremented, in milliseconds. For example, if the component should show
     * the minutes in the current time, this should be 60000.
     */
    /** @hide */
    public long getMsPerIncrement() {
        return fields.getLong(FIELD_MS_PER_INCREMENT);
    }

    /**
     * Returns the lowest value the number will take. So for a standard seconds or
     * minutes display, this should be 0; for 12-hour clock, 1; for 24-hour clock, 0.
     */
    /** @hide */
    public long getLowestValue() {
        return fields.getLong(FIELD_LOWEST_VALUE);
    }

    /**
     * Returns the highest value the number will take. So for a standard seconds or
     * minutes display, this should be 59; for 12-hour clock, 12; for 24-hour clock, 23.
     */
    /** @hide */
    public long getHighestValue() {
        return fields.getLong(FIELD_HIGHEST_VALUE);
    }

    /**
     * Returns a fixed amount of offset time that will be added to the
     * current time when calculating the
     * number to display. The number that will be displayed is
     * ((current_time + timeOffsetMs) / msPerIncrement) %
     *    (highestValue-lowestValue+1) + lowestValue.
     */
    /** @hide */
    public long getTimeOffsetMs() {
        return fields.getLong(FIELD_TIME_OFFSET_MS);
    }

    /**
     * Returns the maximum number of leading zeroes that should be shown when displaying the number.
     * This is number of leading zeroes shown when the number consists of a single digit, so if
     * this is 3 then the number could display as 0004, 0015, 0123, or 1234.
     */
    /** @hide */
    public int getMinDigitsShown() {
        return fields.getInt(FIELD_MIN_DIGITS_SHOWN);
    }

    /**
     * Returns the component id of the {@link FontComponent} that should be used to render the
     * numbers.
     */
    /** @hide */
    public int getFontComponentId() {
        return fields.getInt(FIELD_FONT_COMPONENT_ID);
    }

    /**
     * Returns the position at which the number should be displayed. This is the co-ordinates of the
     * top left corner of the first digit of the number, given in proportion to the bounds of the
     * watch face, with co-ordinates ranging from 0 to 1 with (0, 0) at the top left corner of the
     * watch face.
     */
    /** @hide */
    public PointF getPosition() {
        PointF position = fields.getParcelable(FIELD_POSITION);
        if (position == null) {
            return null;
        }
        return new PointF(position.x, position.y);
    }

    /** Builder for {@link NumberComponent} objects. */
    public static class Builder extends DrawableComponent.Builder<Builder> {
        /**
         * Sets the unit of time represented by the number. For example, if the component should show
         * the minutes in the current time, this should be one minute in milliseconds (60*1000).
         *
         * <p>This field is required.
         */
        public Builder setMsPerIncrement(long msPerIncrement) {
            fields.putLong(FIELD_MS_PER_INCREMENT, msPerIncrement);
            return this;
        }

        /**
         * Sets the lowest value the number will take. So for a standard seconds or
         * minutes display, this should be 0; for 12-hour clock, 1; for 24-hour clock, 0.
         *
         * <p>This field is required.
         */
        public Builder setLowestValue(long lowestValue) {
            fields.putLong(FIELD_LOWEST_VALUE, lowestValue);
            return this;
        }

        /**
         * Sets the highest value the number will take. So for a standard seconds or
         * minutes display, this should be 59; for 12-hour clock, 12; for 24-hour clock, 23.
         *
         * <p>This field is required.
         */
        public Builder setHighestValue(long highestValue) {
            fields.putLong(FIELD_HIGHEST_VALUE, highestValue);
            return this;
        }

        /**
         * Sets a fixed amount of offset time that will be added to the
         * current time when calculating the
         * number to display. The number that will be displayed is
         * ((current_time + timeOffsetMs) / msPerIncrement) %
         *    (highestValue-lowestValue+1) + lowestValue.
         *
         * <p>This field is optional.
         */
        public Builder setTimeOffsetMs(long timeOffsetMs) {
            fields.putLong(FIELD_TIME_OFFSET_MS, timeOffsetMs);
            return this;
        }

        /**
         * Sets how many leading zeroes should be dispayed. For example:
         *
         *                minDigitsShown
         *               0        1       2
         *            ---------------------
         * value  0  | (blank)     0      00
         *        3  |    3        3      03
         *        25 |   25       25      25

         * <p>Defaults to 1.
         */
        public Builder setMinDigitsShown(int minDigitsShown) {
            fields.putInt(FIELD_MIN_DIGITS_SHOWN, minDigitsShown);
            return this;
        }

        /**
         * Sets the component id of the {@link FontComponent} that should be used to render the numbers.
         */
        public Builder setFontComponentId(int fontComponentId) {
            fields.putInt(FIELD_FONT_COMPONENT_ID, fontComponentId);
            return this;
        }

        /**
         * Sets the position at which the number should be displayed. This is the co-ordinates of the
         * top left corner of the first digit of the number, given in proportion to the bounds of the
         * watch face, with co-ordinates ranging from 0 to 1 with (0, 0) at the top left corner of the
         * watch face.
         */
        public Builder setPosition(PointF position) {
            fields.putParcelable(FIELD_POSITION, position);
            return this;
        }

        public NumberComponent build() {
            if (!fields.containsKey(FIELD_COMPONENT_ID)) {
                throw new IllegalStateException("Component id must be provided");
            }

            if (!fields.containsKey(FIELD_MS_PER_INCREMENT)) {
                throw new IllegalStateException("Time unit must be specified");
            }

            if (!fields.containsKey(FIELD_LOWEST_VALUE)) {
                throw new IllegalStateException("Lowest value must be specified");
            }

            if (!fields.containsKey(FIELD_HIGHEST_VALUE)) {
                throw new IllegalStateException("Highest value must be specified");
            }

            if (!fields.containsKey(FIELD_MIN_DIGITS_SHOWN)) {
                fields.putBoolean(FIELD_MIN_DIGITS_SHOWN, true);
            }

            return new NumberComponent(fields);
        }
    }
}
