package com.google.android.clockwork.settings.provider;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.clockwork.settings.utils.SettingsCursor;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.List;

/**
 * A set of settings properties, usually backed by a SharedPreference. For most simple custom
 * preferences this what should be used.
 */
public class PreferencesProperties extends SettingProperties {
    /** Use to persist properties on disk. */
    protected final SharedPreferences mPrefs;
    /** Set of properties. Should be unique, but this is not enforced. */
    protected final List<Property> mProperties;

    /**
     * Constructs a PreferenceProperties backed by the given SharedPreferences and accessible via
     * the given path.
     *
     * @param prefs the SharedPreferences to store the persisted information
     * @param path path to the set of properties in the URI
     */
    public PreferencesProperties(SharedPreferences prefs, String path) {
        super(path);
        mPrefs = Preconditions.checkNotNull(prefs);
        mProperties = new ArrayList<>();
    }

    /**
     * Adds the given property to the set of properties.
     *
     * @param property the property to add to the set of properties
     */
    public PreferencesProperties add(Property property) {
        mProperties.add(property);
        return this;
    }

    /**
     * Add a generic mutable boolean property to the property set.
     *
     * @param key the key of the property
     * @param defaultValue the defaultValue to initialize the value when it is not persisted
     */
    public PreferencesProperties addBoolean(String key, boolean defaultValue) {
        return add(new BooleanProperty(key, defaultValue));
    }

    /**
     * Add a generic mutable boolean property to the property set.
     * <p>
     * Used when the initialization value is expensive to obtain.
     *
     * @param key the key of the property
     * @param defaultValueSupplier only used to initialize the value when it is not persisted,
     *         otherwise it is not called
     */
    public PreferencesProperties addBoolean(String key, BooleanSupplier defaultValueSupplier) {
        return add(new BooleanProperty(key, defaultValueSupplier));
    }

    /**
     * Add a generic immutable property to the property set. Any updates with the key will throw an
     * IllegalArgumentException.
     *
     * @param key the key of the property
     * @param val the value of the property
     */
    public PreferencesProperties addImmutable(String key, Object val) {
        return add(new ImmutableProperty(key, val));
    }

    /**
     * Add a generic mutable integer property to the property set.
     *
     * @param key the key of the property
     * @param defaultValueSupplier only used to initialize the value when it is not persisted,
     *         otherwise it is not called
     * @param validVals a set of valid values for this property. If null or empty, all values will
     *         be permitted. Otherwise, only the given values will be permitted during
     *         updates to the value. Note that the default value is not under these
     *         restrictions.
     */
    public PreferencesProperties addInt(String key, int defaultValue, int... validVals) {
        return add(new IntProperty(key, defaultValue, validVals));
    }

    /**
     * Add a generic mutable integer property to the property set.
     * <p>
     * Used when the initialization value is expensive to obtain.
     *
     * @param key the key of the property
     * @param defaultValueSupplier only used to initialize the value when it is not persisted,
     *         otherwise it is not called
     * @param validVals a set of valid values for this property. If null or empty, all values will
     *         be permitted. Otherwise, only the given values will be permitted during
     *         updates to the value. Note that the default value is not under these
     *         restrictions.
     */
    public PreferencesProperties addInt(String key, IntSupplier defaultValueSupplier,
            int... validVals) {
        return add(new IntProperty(key, defaultValueSupplier, validVals));
    }

    /**
     * Add a generic mutable long property to the property set.
     *
     * @param key the key of the property
     * @param defaultValue the defaultValue to initialize the value when it is not persisted
     */
    public PreferencesProperties addLong(String key, long defaultValue) {
        return add(new LongProperty(key, defaultValue));
    }

    /**
     * Add a generic mutable String property to the property set.
     *
     * @param key the key of the property
     * @param defaultValue the defaultValue to initialize the value when it is not persisted
     */
    public PreferencesProperties addString(String key, String defaultValue) {
        return add(new StringProperty(key, defaultValue));
    }

    @Override
    public SettingsCursor query() {
        SettingsCursor c = new SettingsCursor();
        for (Property property : mProperties) {
            property.populateQuery(c);
        }
        return c;
    }

    @Override
    public int update(ContentValues values) {
        int rowsUpdated = 0;
        SharedPreferences.Editor editor = mPrefs.edit();

        for (Property property : mProperties) {
            if (values.containsKey(property.mKey)) {
                try {
                    rowsUpdated += property.updateProperty(values, editor);
                } catch (UnsupportedOperationException e) {
                    Log.w(SettingsBackupAgent.BACKUP_DEBUG_TAG,
                            "Skipping update restore of " + property.mKey, e);
                }
            }
        }

        if (rowsUpdated > 0) {
            editor.apply();
        }
        return rowsUpdated;
    }

    /** A single encapsulated property to be used in the set of properties. */
    public abstract class Property {
        protected final String mKey;

        /** @param key the key used to reference this property. */
        public Property(String key) {
            mKey = key;
        }

        /**
         * Adds the information encapsulated by this property to the given cursor.
         *
         * @param c the cursor to populate with this property's information
         */
        public abstract void populateQuery(SettingsCursor c);

        /**
         * Add updates to given editor based on the update request. The property key is guaranteed
         * to exist, though the value can still be null.
         *
         * @param values the values to be used in the update request
         * @param editor the editor to set all the updated values to. In most cases, do not call
         *         apply() or commit().
         * @return the number of rows updated (usually 1 if there are changes, 0 if none)
         */
        public abstract int updateProperty(ContentValues values, SharedPreferences.Editor editor);
    }

    protected class BooleanProperty extends Property {
        protected boolean mVal;

        protected BooleanProperty(String key, boolean defaultValue) {
            super(key);
            mVal = mPrefs.getBoolean(key, defaultValue);
        }

        protected BooleanProperty(String key, BooleanSupplier defaultValueSupplier) {
            super(key);
            // ternary operator used to call defaultValueSupplier.getAsBoolean() only if needed
            mVal = mPrefs.contains(key)
                    ? mPrefs.getBoolean(key, false) : defaultValueSupplier.getAsBoolean();
        }

        @Override
        public void populateQuery(SettingsCursor c) {
            c.addRow(mKey, mVal ? 1 : 0);
        }

        @Override
        public int updateProperty(ContentValues values, SharedPreferences.Editor editor) {
            boolean newVal = PropertiesPreconditions.checkBoolean(values, mKey);
            if (newVal == mVal && mPrefs.contains(mKey)) {
                return 0;
            } else {
                mVal = newVal;
                editor.putBoolean(mKey, mVal);
                return 1;
            }
        }
    }

    protected class IntProperty extends Property {
        protected int mVal;
        private final int[] mValidVals;

        protected IntProperty(String key, IntSupplier defaultValueSupplier,
                int... validVals) {
            super(key);
            // ternary operator used to call defaultValueSupplier.getAsInt() only if needed
            mVal = mPrefs.contains(key) ? mPrefs.getInt(key, 0) : defaultValueSupplier.getAsInt();
            mValidVals = validVals;
        }

        protected IntProperty(String key, int defaultValue, int[] validVals) {
            super(key);
            mVal = mPrefs.getInt(key, defaultValue);
            mValidVals = validVals;
        }

        @Override
        public void populateQuery(SettingsCursor c) {
            c.addRow(mKey, mVal);
        }

        @Override
        public int updateProperty(ContentValues values, SharedPreferences.Editor editor) {
            int newVal = PropertiesPreconditions.checkInt(values, mKey, mValidVals);
            if (newVal == mVal && mPrefs.contains(mKey)) {
                return 0;
            } else {
                mVal = newVal;
                editor.putInt(mKey, mVal);
                return 1;
            }
        }
    }

    protected class LongProperty extends Property {
        protected long mVal;

        protected LongProperty(String key, long defaultValue) {
            super(key);
            mVal = mPrefs.getLong(key, defaultValue);
        }

        @Override
        public void populateQuery(SettingsCursor c) {
            c.addRow(mKey, mVal);
        }

        @Override
        public int updateProperty(ContentValues values, SharedPreferences.Editor editor) {
            long newVal = PropertiesPreconditions.checkLong(values, mKey);
            if (newVal == mVal && mPrefs.contains(mKey)) {
                return 0;
            } else {
                mVal = newVal;
                editor.putLong(mKey, mVal);
                return 1;
            }
        }
    }

    protected class StringProperty extends Property {
        protected String mVal;

        protected StringProperty(String key, String defaultValue) {
            super(key);
            mVal = mPrefs.getString(key, defaultValue);
        }

        @Override
        public void populateQuery(SettingsCursor c) {
            c.addRow(mKey, mVal);
        }

        @Override
        public int updateProperty(ContentValues values, SharedPreferences.Editor editor) {
            String newVal = values.getAsString(mKey);
            if (newVal == mVal && mPrefs.contains(mKey)) {
                return 0;
            } else {
                mVal = newVal;
                editor.putString(mKey, mVal);
                return 1;
            }
        }
    }

    protected class ImmutableProperty extends Property {
        protected Object mVal;

        protected ImmutableProperty(String key, Object val) {
            super(key);
            mVal = val;
        }

        @Override
        public void populateQuery(SettingsCursor c) {
            c.addRow(mKey, mVal);
        }

        @Override
        public int updateProperty(ContentValues values, SharedPreferences.Editor editor) {
            throw new UnsupportedOperationException(
                String.format("key \"%s\" in path \"%s\" is not mutable", mKey, getPath()));
        }
    }
}
