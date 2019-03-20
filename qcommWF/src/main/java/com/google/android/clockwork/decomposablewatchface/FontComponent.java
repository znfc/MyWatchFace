package com.google.android.clockwork.decomposablewatchface;

import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class FontComponent extends AbstractComponent {

    /** @hide */
    public static final Parcelable.Creator<FontComponent> CREATOR =
            new Parcelable.Creator<FontComponent>() {
                @Override
                public FontComponent createFromParcel(Parcel source) {
                    return new FontComponent(source);
                }

                @Override
                public FontComponent[] newArray(int size) {
                    return new FontComponent[size];
                }
            };

    private static final String FIELD_IMAGE = "image";
    private static final String FIELD_DIGIT_COUNT = "digit_count";
    private static final String FIELD_DIMENSIONS = "dimensions";

    private FontComponent(Bundle fields) {
        super(fields);
    }

    private FontComponent(Parcel in) {
        super(in);
    }

    /** Returns the image that makes up this font. */
    /** @hide */
    public Icon getImage() {
        return fields.getParcelable(FIELD_IMAGE);
    }

    /**
     * Returns the number of digits contained in the font image. It is assumed that the digits will be
     * stacked vertically, with the smallest number at the top, and that each digit will have the same
     * height.
     */
    /** @hide */
    public int getDigitCount() {
        return fields.getInt(FIELD_DIGIT_COUNT);
    }

    /** @hide */
    public android.graphics.PointF getDigitDimensions() {
        return fields.getParcelable(FIELD_DIMENSIONS);
    }

    /** Builder for {@link FontComponent} objects. */
    public static class Builder extends AbstractComponent.Builder<Builder> {
        /** Sets the image that makes up this font. */
        public Builder setImage(Icon image) {
            fields.putParcelable(FIELD_IMAGE, image);
            return this;
        }

        /**
         * Sets the number of digits contained in the font image. It is assumed that the digits will be
         * stacked vertically, with the smallest number "0" at the top, and that each digit will have the
         * same height.
         */
        public Builder setDigitCount(int digitCount) {
            fields.putInt(FIELD_DIGIT_COUNT, digitCount);
            return this;
        }

        /**
         * Sets the (x, y) size of a single digit, as a fraction of the
         * corresponding screen dimension.
         *
         * TODO olilan: Is a PointF really appropriate given that it's a size,
         * not a location? It's more like pair<float, float> or just (float x, float y).
         */
        public Builder setDigitDimensions(android.graphics.PointF size) {
            fields.putParcelable(FIELD_DIMENSIONS, size);
            return this;
        }

        public FontComponent build() {
            if (!fields.containsKey(FIELD_COMPONENT_ID)) {
                throw new IllegalStateException("Component id must be provided");
            }

            if (fields.getParcelable(FIELD_IMAGE) == null) {
                throw new IllegalStateException("Image must be provided");
            }

            return new FontComponent(fields);
        }
    }
}
