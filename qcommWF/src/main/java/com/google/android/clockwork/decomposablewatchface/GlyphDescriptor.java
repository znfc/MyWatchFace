package com.google.android.clockwork.decomposablewatchface;

import android.os.Parcel;
import android.os.Parcelable;

public class GlyphDescriptor implements Parcelable{
    public short width;
    public byte glyphCode;

    public GlyphDescriptor(short width, byte glyphCode) {
        this.width = width;
        this.glyphCode = glyphCode;
    }

    protected GlyphDescriptor(Parcel in) {
        width = (short) in.readInt();
        glyphCode = in.readByte();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt((int) width);
        dest.writeByte(glyphCode);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<GlyphDescriptor> CREATOR = new Creator<GlyphDescriptor>() {
        @Override
        public GlyphDescriptor createFromParcel(Parcel in) {
            return new GlyphDescriptor(in);
        }

        @Override
        public GlyphDescriptor[] newArray(int size) {
            return new GlyphDescriptor[size];
        }
    };
}
