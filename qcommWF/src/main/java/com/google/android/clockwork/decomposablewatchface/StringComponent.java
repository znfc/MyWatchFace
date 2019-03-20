package com.google.android.clockwork.decomposablewatchface;

import android.graphics.PointF;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class StringComponent extends DrawableComponent {

    public enum Alignment {
        LEFT, CENTER, RIGHT;
    }

    /** @hide */
    public static final Creator<StringComponent> CREATOR =
            new Creator<StringComponent>() {
                @Override
                public StringComponent createFromParcel(Parcel source) {
                    return new StringComponent(source);
                }

                @Override
                public StringComponent[] newArray(int size) {
                    return new StringComponent[size];
                }
            };

    private static final String FIELD_POSITION = "position";
    private static final String FIELD_FONT_COMPONENT_ID = "font_component_id";
    private static final String FIELD_SOURCE_ID = "string_source_id";
    private static final String FIELD_ALIGNMENT = "alignment";

    private StringComponent(Bundle fields) {
        super(fields);
    }

    private StringComponent(Parcel in) {
        super(in);
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

    /** @hide */
    public int getStringSourceId() {
        return fields.getInt(FIELD_SOURCE_ID);
    }

    /** @hide */
    public Alignment getAlignment() {
        return Alignment.values()[fields.getInt(FIELD_ALIGNMENT)];
    }

    /** Builder for {@link StringComponent} objects. */
    public static class Builder extends DrawableComponent.Builder<Builder> {

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

        public Builder setStringSourceId(int sourceId) {
            fields.putInt(FIELD_SOURCE_ID, sourceId);
            return this;
        }

        public Builder setAlignment(Alignment alignment) {
            fields.putInt(FIELD_ALIGNMENT, alignment.ordinal());
            return this;
        }

        public StringComponent build() {
            if (!fields.containsKey(FIELD_COMPONENT_ID)) {
                throw new IllegalStateException("Component id must be provided");
            }

            if (!fields.containsKey(FIELD_SOURCE_ID)) {
                throw new IllegalStateException("Source id must be specified");
            }

            if (!fields.containsKey(FIELD_ALIGNMENT)) {
                throw new IllegalStateException("Alignment must be specified");
            }

            return new StringComponent(fields);
        }
    }
}
