package com.google.android.clockwork.decomposablewatchface;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public abstract class AbstractComponent implements Parcelable, WatchFaceDecomposition.Component {
    static final String FIELD_COMPONENT_ID = "component_id";

    final Bundle fields;

    AbstractComponent(Bundle fields) {
        this.fields = fields;
    }

    AbstractComponent(Parcel in) {
        fields = in.readBundle(getClass().getClassLoader());
    }


    /** @hide */
    @Override
    public int describeContents() {
        return 0;
    }

    /** @hide */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBundle(fields);
    }

    /**
     * Returns the id for this component. Should be unique across all components in any given {@link
     * WatchFaceDecomposition}.
     */
    /** @hide */
    @Override
    public int getComponentId() {
        return fields.getInt(FIELD_COMPONENT_ID);
    }

    /** Builder for {@link AbstractComponent} objects. */
    public static abstract class Builder<T extends Builder> {
        Bundle fields = new Bundle();

        /**
         * Sets the id for the component. This field is required, and the id must be unique across all
         * components within a {@link WatchFaceDecomposition}.
         */
        public T setComponentId(int componentId) {
            fields.putInt(FIELD_COMPONENT_ID, componentId);
            return (T) this;
        }
    }
}
