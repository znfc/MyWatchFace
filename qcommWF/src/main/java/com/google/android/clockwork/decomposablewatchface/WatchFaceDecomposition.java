package com.google.android.clockwork.decomposablewatchface;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseBooleanArray;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WatchFaceDecomposition implements Parcelable {

    public interface Component {
        /** @hide */
        int getComponentId();
    }

    /** @hide */
    public static final Creator<WatchFaceDecomposition> CREATOR =
            new Creator<WatchFaceDecomposition>() {
                @Override
                public WatchFaceDecomposition createFromParcel(Parcel source) {
                    return new WatchFaceDecomposition(source);
                }

                @Override
                public WatchFaceDecomposition[] newArray(int size) {
                    return new WatchFaceDecomposition[size];
                }
            };

    private static final String FIELD_IMAGES = "images";
    private static final String FIELD_NUMBERS = "numbers";
    private static final String FIELD_FONTS = "fonts";
    private static final String FIELD_STRINGS = "strings";
    private static final String FIELD_PROPORTIONAL_FONTS = "proportional-fonts";

    // TODO(olilan): Add "ids to delete", "clear all" flag

    private final Bundle fields;

    private WatchFaceDecomposition(Bundle fields) {
        this.fields = fields;
    }

    private WatchFaceDecomposition(Parcel in) {
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

    /** Returns a list of all the image components in this decomposition. */
    /** @hide */
    public List<ImageComponent> getImageComponents() {
        return getUnmodifiableComponentList(FIELD_IMAGES);
    }

    /** Returns a list of all the number components in this decomposition. */
    /** @hide */
    public List<NumberComponent> getNumberComponents() {
        return getUnmodifiableComponentList(FIELD_NUMBERS);
    }

    /** Returns a list of all the font components in this decomposition. */
    /** @hide */
    public List<FontComponent> getFontComponents() {
        return getUnmodifiableComponentList(FIELD_FONTS);
    }

    /** @hide */
    public List<StringComponent> getStringComponents() {
        return getUnmodifiableComponentList(FIELD_STRINGS);
    }

    /** @hide */
    public List<ProportionalFontComponent> getProportionalFontComponents() {
        return getUnmodifiableComponentList(FIELD_PROPORTIONAL_FONTS);
    }

    private <T extends Parcelable> List<T> getUnmodifiableComponentList(String field) {
        List<T> components = fields.getParcelableArrayList(field);
        if (components == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(components);
    }

    /** Builder for {@link WatchFaceDecomposition} objects. */
    public static class Builder {
        private Bundle fields = new Bundle();
        private ArrayList<ImageComponent> images = new ArrayList<>();
        private ArrayList<NumberComponent> numbers = new ArrayList<>();
        private ArrayList<FontComponent> fonts = new ArrayList<>();
        private ArrayList<StringComponent> strings = new ArrayList<>();
        private ArrayList<ProportionalFontComponent> proportionalFonts = new ArrayList<>();

        /**
         * Adds all the provided image components to the decomposition. Each component added should have
         * a component id that is unique across all components in the decomposition.
         */
        public Builder addImageComponents(ImageComponent... imageComponents) {
            Collections.addAll(images, imageComponents);
            return this;
        }

        /**
         * Adds all the provided number components to the decomposition. Each component added should
         * have a component id that is unique across all components in the decomposition.
         */
        public Builder addNumberComponents(NumberComponent... numberComponents) {
            Collections.addAll(numbers, numberComponents);
            return this;
        }

        /**
         * Adds all the provided font components to the decomposition. Each component added should have
         * a component id that is unique across all components in the decomposition.
         */
        public Builder addFontComponents(FontComponent... fontComponents) {
            Collections.addAll(fonts, fontComponents);
            return this;
        }

        public Builder addStringComponents(StringComponent... stringComponents) {
            Collections.addAll(strings, stringComponents);
            return this;
        }

        public Builder addProportionalFontComponents(ProportionalFontComponent... proportionalFontComponents) {
            Collections.addAll(proportionalFonts, proportionalFontComponents);
            return this;
        }

        public WatchFaceDecomposition build() {
            if (!allComponentIdsAreUnique()) {
                throw new IllegalStateException("Duplicate component ids found.");
            }

            fields.putParcelableArrayList(FIELD_IMAGES, images);
            fields.putParcelableArrayList(FIELD_NUMBERS, numbers);
            fields.putParcelableArrayList(FIELD_FONTS, fonts);
            fields.putParcelableArrayList(FIELD_STRINGS, strings);
            fields.putParcelableArrayList(FIELD_PROPORTIONAL_FONTS, proportionalFonts);
            return new WatchFaceDecomposition(fields);
        }

        private boolean allComponentIdsAreUnique() {
            SparseBooleanArray ids =
                    new SparseBooleanArray(images.size() + numbers.size() + fonts.size() + strings.size() + proportionalFonts.size());

            if (!allNewIds(images, ids)) {
                return false;
            }

            if (!allNewIds(numbers, ids)) {
                return false;
            }

            if (!allNewIds(fonts, ids)) {
                return false;
            }

            if (!allNewIds(strings, ids)) {
                return false;
            }

            if (!allNewIds(proportionalFonts, ids)) {
                return false;
            }

            return true;
        }

        /**
         * Returns true if the componentId of every {@link Component} in {@code componentList} is unique
         * and is not present in {@code currentIds}. Also adds all the componentIds from the list into
         * currentIds.
         */
        private <T extends Component> boolean allNewIds(
            List<T> componentList, SparseBooleanArray currentIds) {
            for (T component : componentList) {
                int id = component.getComponentId();
                if (currentIds.get(id)) {
                    return false;
                }
                currentIds.put(id, true);
            }
            return true;
        }
    }
}
