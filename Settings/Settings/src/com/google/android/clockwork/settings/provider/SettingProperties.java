package com.google.android.clockwork.settings.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;
import android.util.Log;

import com.google.android.clockwork.settings.SettingsContract;
import com.google.common.base.Preconditions;

import com.google.android.clockwork.settings.utils.SettingsCursor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

/** Encapsulates a property to be used in SettingsProvider. */
public abstract class SettingProperties {
    /** The path of the URI to the property. */
    private final String mPath;

    /**
     * Constructs a SettingProperties to use the given path. The path is immutable and cannot change
     * after initialization.
     *
     * @param path path to this property in the URI of the content resolver
     */
    protected SettingProperties(String path) {
        Preconditions.checkArgument(!TextUtils.isEmpty(path),
                "path \"%s\" cannot be null or empty", path);
        mPath = path;
    }

    /**
     * Returns the given path that was given during initialization, used for the UriMatcher to match
     * the property to the URI.
     */
    public String getPath() {
        return mPath;
    }

    /**
     * Used by SettingsProvider to notify listeners after update returns with rows changes (i.e.
     * more than zero).
     *
     * @param resolver the ContentResolver to notify the changes
     * @param uri      the URI that was updated to be notified
     */
    protected void notifyChange(ContentResolver resolver, Uri uri) {
        resolver.notifyChange(uri, null);
    }

    /**
     * Handles a query request for the information contained by the SettingProperties.
     * <p>
     * Default implementation just calls {@link #query()}.
     *
     * @param projection         the list of columns to put into the cursor. If {@code null} all
     *                           columns are
     *                           included.
     * @param queryArgs          a Bundle containing all additional information necessary for the
     *                           query.
     *                           Values in the Bundle may include SQL style arguments.
     * @param cancellationSignal a signal to cancel the operation in progress, or {@code null}.
     * @return a SettingsCursor with the data encapsulated in the property
     * @see ContentResolver#query(Uri, String[], String, String[], String, CancellationSignal) for
     * implementation details.
     */
    public SettingsCursor query(String[] projection, Bundle queryArgs,
            CancellationSignal cancellationSignal) {
        return query();
    }

    /**
     * Handles a query request for the information contained by the SettingProperties.
     * <p>
     * If {@link #query(String[], Bundle, CancellationSignal)} is overridden, a stub implementation
     * of this method should be provided.
     *
     * @return a SettingsCursor with the data encapsulated in the property
     */
    public abstract SettingsCursor query();

    /**
     * Handles an update request to change information in the SettingProperties.
     *
     * @param values the values from the update request
     * @return the number of rows changed
     * @throws UnsupportedOperationException if the operation is not supported, often thrown if the
     *                                       property is immutable
     */
    public abstract int update(ContentValues values);

    byte[] getBackupData() {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try {
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(byteOut, "UTF-8"));
            SettingsCursor cursor = query();
            int keyColumn = cursor.getColumnIndex(SettingsContract.COLUMN_KEY);
            int valueColumn = cursor.getColumnIndex(SettingsContract.COLUMN_VALUE);
            writer.beginObject();
            if (cursor.moveToFirst()) {
                do {
                    String name = cursor.getString(keyColumn);
                    String value = cursor.getString(valueColumn);
                    if (Log.isLoggable(SettingsBackupAgent.BACKUP_DEBUG_TAG, Log.DEBUG)) {
                        Log.d(SettingsBackupAgent.BACKUP_DEBUG_TAG,
                                "Saving item " + name + " with value " + value);
                    }
                    writer.name(name).value(value);
                } while (cursor.moveToNext());
            }
            writer.endObject();
            cursor.close();
            writer.close();
        } catch (IOException e) {
            Log.e(SettingsBackupAgent.BACKUP_DEBUG_TAG, "Error creating backup stream");
        }
        return byteOut.toByteArray();
    }

    void restore(byte[] data) {
        ContentValues restoredValues = new ContentValues();
        try {
            ByteArrayInputStream byteIn = new ByteArrayInputStream(data);
            JsonReader reader = new JsonReader(new InputStreamReader(byteIn, "UTF-8"));
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull();
                    restoredValues.put(name, String.valueOf((Object) null));
                } else {
                    restoredValues.put(name, reader.nextString());
                }
            }
            reader.endObject();
            reader.close();
        } catch (EOFException e) {
            // This just means nothing was saved
        } catch (IOException e) {
            Log.e(SettingsBackupAgent.BACKUP_DEBUG_TAG, "error deserializing restore data", e);
        }

        if (Log.isLoggable(SettingsBackupAgent.BACKUP_DEBUG_TAG, Log.DEBUG)) {
            Log.d(SettingsBackupAgent.BACKUP_DEBUG_TAG,
                    "Restoring " + restoredValues.toString());
        }
        try {
            int rows = update(restoredValues);
            if (Log.isLoggable(SettingsBackupAgent.BACKUP_DEBUG_TAG, Log.DEBUG)) {
                Log.d(SettingsBackupAgent.BACKUP_DEBUG_TAG, "Updated rows: " + rows);
            }
        } catch (UnsupportedOperationException | IllegalArgumentException e) {
            Log.w(SettingsBackupAgent.BACKUP_DEBUG_TAG, "Unable to restore property " + getPath(),
                    e);
        }
    }
}
