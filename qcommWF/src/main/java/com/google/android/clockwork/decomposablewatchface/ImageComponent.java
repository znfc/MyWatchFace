package com.google.android.clockwork.decomposablewatchface;

import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
//import android.annotation.Nullable;

public class ImageComponent extends DrawableComponent {

    /** @hide */
    public static final Creator<ImageComponent> CREATOR =
            new Creator<ImageComponent>() {
                @Override
                public ImageComponent createFromParcel(Parcel source) {
                    return new ImageComponent(source);
                }

                @Override
                public ImageComponent[] newArray(int size) {
                    return new ImageComponent[size];
                }
            };

    private static final String FIELD_IMAGE = "image";
    private static final String FIELD_BOUNDS = "bounds";
    private static final String FIELD_PIVOT = "pivot";
    private static final String FIELD_DEGREES_PER_DAY = "degrees_per_day";
    private static final String FIELD_OFFSET_DEGREES = "offset_degrees";
    private static final String FIELD_DEGREES_PER_STEP = "degrees_per_step";

    private ImageComponent(Bundle fields) {
        super(fields);
    }

    private ImageComponent(Parcel in) {
        super(in);
    }

    /** Returns the image to be displayed. */
    /** @hide */
    public Icon getImage() {
        return fields.getParcelable(FIELD_IMAGE);
    }

    /**
     * Returns the bounds within which the image should be displayed, expressed in proportion to the
     * bounds of the watch face, with co-ordinates ranging from 0 to 1, where (0, 0) is the top left
     * corner of the image.
     */
    /** @hide */
    public RectF getBounds() {
        return fields.getParcelable(FIELD_BOUNDS);
    }

    /**
     * Returns the number of degrees clockwise through which the component should be rotated in a full
     * day (i.e. 24 hours), or 0 if the component does not rotate.
     */
    /** @hide */
    public float getDegreesPerDay() {
        return fields.getFloat(FIELD_DEGREES_PER_DAY);
    }

    /**
     * If the component rotates, returns the point around which the component should be rotated,
     * expressed in proportion to the bounds of the component, with co-ordinates  where (0, 0) is the
     * top-left-most point of the component, and (1, 1) is the bottom-right-most point. For example,
     * (0.5, 0.5) is the center of the component.
     *
     * <p>May return null if the component does not rotate.
     */
    /** @hide */
//    @Nullable
    public PointF getPivot() {
        PointF pivot = fields.getParcelable(FIELD_PIVOT);
        if (pivot == null) {
            return null;
        }
        return new PointF(pivot.x, pivot.y);
    }

    /** Returns the clockwise rotation in degrees that should be applied to the image at time 0. */
    /** @hide */
    public float getOffsetDegrees() {
        return fields.getFloat(FIELD_OFFSET_DEGREES);
    }

    /**
     * Causes rotation to animate in discrete steps by given angle (Read: No smoother than this). So
     * for a ticking second hand this should be 6.0. The default of 0 means "as smooth as possible."
     * Not required if the component does not rotate and is meaningless without degreesPerDay.
     */
    /** @hide */
    public float getDegreesPerStep() {
        return fields.getFloat(FIELD_DEGREES_PER_STEP);
    }

    /** Builder for {@link ImageComponent} objects. */
    public static class Builder extends DrawableComponent.Builder<Builder> {
        /** Sets the image to be displayed. This field is required. */
        public Builder setImage(Icon image) {
            fields.putParcelable(FIELD_IMAGE, image);
            return this;
        }

        /**
         * Sets the bounds within which the image should be displayed, expressed in proportion to the
         * bounds of the watch face, with co-ordinates ranging from 0 to 1, where (0, 0) is the top left
         * corner of the image.
         *
         * <p>If not specified, the image will fill the bounds of the watch face.
         */
        public Builder setBounds(RectF bounds) {
            fields.putParcelable(FIELD_BOUNDS, new RectF(bounds));
            return this;
        }

        /**
         * Sets the number of degrees clockwise through which the component should be rotated in a full
         * day (i.e. 24 hours).
         */
        public Builder setDegreesPerDay(float degreesPerDay) {
            fields.putFloat(FIELD_DEGREES_PER_DAY, degreesPerDay);
            return this;
        }

        /**
         * Sets the point around which the component should be rotated, expressed in proportion to the
         * screen, with co-ordinates ranging from 0 to 1, where (0, 0) is the top left
         * corner of the screen. For example, (0.5, 0.5) is the center of the screen.
         * Will handle null pivot (should only be null on non-rotating objects).
         */
        public Builder setPivot(PointF pivot) {
            if (pivot == null) {
                fields.putParcelable(FIELD_PIVOT, null);
                return this;
            }
            fields.putParcelable(FIELD_PIVOT, new PointF(pivot.x, pivot.y));
            return this;
        }

        /** Sets the clockwise rotation in degrees that should be applied to the image at time 0. */
        public Builder setOffsetDegrees(float offset) {
            fields.putFloat(FIELD_OFFSET_DEGREES, offset);
            return this;
        }

        /**
         * Causes rotation to animate in discrete steps by given angle (Read: No smoother than
         * this). So for a ticking second hand this should be 6.0. The default of 0 means "as smooth
         * as possible." Not required if the component does not rotate and is meaningless without
         * degreesPerDay.
         */
        public Builder setDegreesPerStep(float degreesPerStep) {
            fields.putFloat(FIELD_DEGREES_PER_STEP, degreesPerStep);
            return this;
        }

        public ImageComponent build() {
            if (!fields.containsKey(FIELD_COMPONENT_ID)) {
                throw new IllegalStateException("Component id must be provided");
            }

            if (fields.getParcelable(FIELD_IMAGE) == null) {
                throw new IllegalStateException("Image must be provided");
            }

            if (!fields.containsKey(FIELD_BOUNDS)) {
                throw new IllegalStateException("Bounds must be provided");
            }

            return new ImageComponent(fields);
        }
    }
}
