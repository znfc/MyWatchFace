package com.google.android.clockwork.decomposablewatchface;

import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class ProportionalFontComponent extends AbstractComponent {

    /** @hide */
    public static final Creator<ProportionalFontComponent> CREATOR =
            new Creator<ProportionalFontComponent>() {
                @Override
                public ProportionalFontComponent createFromParcel(Parcel source) {
                    return new ProportionalFontComponent(source);
                }

                @Override
                public ProportionalFontComponent[] newArray(int size) {
                    return new ProportionalFontComponent[size];
                }
            };

    private static final String FIELD_IMAGE = "image";
    private static final String FIELD_GLYPH_INFO = "glyph_info";

    private ProportionalFontComponent(Bundle fields) {
        super(fields);
    }

    private ProportionalFontComponent(Parcel in) {
        super(in);
    }

    /** Returns the image that makes up this font. */
    /** @hide */
    public Icon getImage() {
        return fields.getParcelable(FIELD_IMAGE);
    }

    /** @hide */
    public ArrayList<GlyphDescriptor> getGlyphDescriptors() {
        return fields.getParcelableArrayList(FIELD_GLYPH_INFO);
    }

    /** Builder for {@link ProportionalFontComponent} objects. */
    public static class Builder extends AbstractComponent.Builder<Builder> {
        /** Sets the image that makes up this font. */
        public Builder setImage(Icon image) {
            fields.putParcelable(FIELD_IMAGE, image);
            return this;
        }

        public Builder setGlyphDescriptors(ArrayList<GlyphDescriptor> glyphDescriptorList) {
            fields.putParcelableArrayList(FIELD_GLYPH_INFO, glyphDescriptorList);
            return this;
        }

        public ProportionalFontComponent build() {
            if (!fields.containsKey(FIELD_COMPONENT_ID)) {
                throw new IllegalStateException("Component id must be provided");
            }

            if (fields.getParcelable(FIELD_IMAGE) == null) {
                throw new IllegalStateException("Image must be provided");
            }

            if (!fields.containsKey(FIELD_GLYPH_INFO)) {
                throw new IllegalStateException("Glyph info must be provided");
            }

            return new ProportionalFontComponent(fields);
        }
    }
}
