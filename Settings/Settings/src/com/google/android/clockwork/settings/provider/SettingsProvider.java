package com.google.android.clockwork.settings.provider;

import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.StrictMode;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import com.google.android.clockwork.common.concurrent.CwStrictMode;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.utils.SettingsCursor;
import com.google.android.gsf.Gservices;
import com.google.android.gsf.GservicesValue;
import com.google.common.base.Suppliers;

/**
 * A content provider for the clockwork settings.
 *
 * This ContentProvider runs in the system process. Do NOT access default shared preferences outside
 * of this class.
 */
public class SettingsProvider extends ContentProvider {

    static final String PERMISSION_PROVIDER_BACKUP =
            "com.google.android.clockwork.settings.permission.PROVIDER_BACKUP";

    static final String ACTION_BACKUP =
            "com.google.android.clockwork.settings.ACTION_BACKUP";

    static final String ACTION_RESTORE =
            "com.google.android.clockwork.settings.ACTION_RESTORE";

    /**
     * Holds the uriMatcher and its corresponding set of settings properties. Both should not be
     * altered after they have been loaded into this object. The wrapper is used to hold both
     * objects as both objects are generated at the same time.
     */
    @VisibleForTesting static class PropertiesWrapper {
        final UriMatcher uriMatcher;
        final SettingProperties[] properties;

        PropertiesWrapper(UriMatcher uriMatcher, SettingProperties[] properties) {
            this.uriMatcher = uriMatcher;
            this.properties = properties;
        }
    }

    private static final String TAG = "SettingsProvider";

    private int hasUpdates;

    @VisibleForTesting com.google.common.base.Supplier<PropertiesWrapper> mPropertiesSupplier;

    private final BroadcastReceiver mGflagsChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onGservicesFlagsChange(context);
        }
    };

    private final BroadcastReceiver mBackupAndRestoreReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            switch (intent.getAction()) {
                case ACTION_BACKUP:
                    if (Log.isLoggable(SettingsBackupAgent.BACKUP_DEBUG_TAG, Log.DEBUG)) {
                        Log.d(SettingsBackupAgent.BACKUP_DEBUG_TAG, "Backup done, reset flag");
                    }
                    hasUpdates = 0;
                    break;

                case ACTION_RESTORE:
                    if (Log.isLoggable(SettingsBackupAgent.BACKUP_DEBUG_TAG, Log.DEBUG)) {
                        Log.d(SettingsBackupAgent.BACKUP_DEBUG_TAG,
                                "Resetting provider after restore");
                    }
                    mPropertiesSupplier = Suppliers.memoize(SettingsProvider.this::loadProperties);
                    for (SettingProperties property : mPropertiesSupplier.get().properties) {
                        if (Log.isLoggable(SettingsBackupAgent.BACKUP_DEBUG_TAG, Log.DEBUG)) {
                            Log.d(SettingsBackupAgent.BACKUP_DEBUG_TAG,
                                    "Notifying " + property.getPath());
                        }
                        property.notifyChange(getContext().getContentResolver(),
                                new Uri.Builder()
                                        .scheme("content")
                                        .authority(SettingsContract.SETTINGS_AUTHORITY)
                                        .path(property.getPath())
                                        .build());
                    }
                    break;

                default:
                    break;
            }
        }
    };

    @Override
    public boolean onCreate() {
        mPropertiesSupplier = Suppliers.memoize(this::loadProperties);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_BACKUP);
        filter.addAction(ACTION_RESTORE);

        // Provider remains active for as long as the process lives.
        // Unregistration of receivers are not required.
        getContext().registerReceiver(mBackupAndRestoreReceiver, filter, PERMISSION_PROVIDER_BACKUP,
                null);
        getContext().registerReceiver(mGflagsChangeReceiver,
                new IntentFilter(Gservices.CHANGED_ACTION));

        hasUpdates = 0;
        return true;
    }

    /**
     * For unit tests only! See {@link ContentProvider#shutdown()}
     */
    @Override
    public void shutdown() {
        getContext().unregisterReceiver(mBackupAndRestoreReceiver);
        getContext().unregisterReceiver(mGflagsChangeReceiver);
        super.shutdown();
    }

    /**
     * Called whenever Gservices flags are updated.
     */
    private void onGservicesFlagsChange(Context context) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "received Gservices.CHANGED_ACTION.");
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, Bundle queryArgs,
            CancellationSignal cancellationSignal) {
        PropertiesWrapper wrapper = mPropertiesSupplier.get();
        int code = wrapper.uriMatcher.match(uri);
        if (code == UriMatcher.NO_MATCH) {
            Log.w(TAG, "Unknown uri: " + uri);
            return null;
        } else if (code == wrapper.properties.length) {
            if (Log.isLoggable(SettingsBackupAgent.BACKUP_DEBUG_TAG, Log.DEBUG)) {
                Log.d(SettingsBackupAgent.BACKUP_DEBUG_TAG, "Querying provider updates");
            }
            return new SettingsCursor(SettingsBackupAgent.BACKUP_UPDATES_KEY, hasUpdates);
        } else {
            return wrapper.properties[code].query(projection, queryArgs, cancellationSignal);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        Log.w(TAG, "using deprecated query method");
        Bundle queryArgs = new Bundle(3);
        queryArgs.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection);
        queryArgs.putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs);
        queryArgs.putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortOrder);
        return query(uri, projection, queryArgs, null);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        PropertiesWrapper wrapper = mPropertiesSupplier.get();
        int code = wrapper.uriMatcher.match(uri);
        if (code == UriMatcher.NO_MATCH) {
            Log.w(TAG, "Unknown uri: " + uri);
            return -1;
        } else if (code == wrapper.properties.length) {
            // The provider updates the dirty flag internally
            return 0;
        } else {
            SettingProperties prop = wrapper.properties[code];
            int rowsChanged = prop.update(values);
            if (rowsChanged > 0) {
                // Notify any listeners that the data backing the content provider has changed
                // Also mark this property as being changed for the next backup run
                wrapper.properties[code].notifyChange(getContext().getContentResolver(), uri);
                if (Log.isLoggable(SettingsBackupAgent.BACKUP_DEBUG_TAG, Log.DEBUG)) {
                    Log.d(SettingsBackupAgent.BACKUP_DEBUG_TAG,
                            prop.getPath() + " updated, need new backup");
                }
                hasUpdates = 1;
            }
            return rowsChanged;
        }
    }

    @Override
    public String getType(Uri uri) {
        PropertiesWrapper wrapper = mPropertiesSupplier.get();
        return wrapper.uriMatcher.match(uri) == UriMatcher.NO_MATCH
                ? null
                : "vnd.android.cursor.item/vnd." + SettingsContract.SETTINGS_AUTHORITY;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException(); // We don't support insertion
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException(); // We don't support deletion
    }

    /**
     * Load all the properties required for the settings provider. Should only be called on device
     * boot or after a restore.
     */
    private PropertiesWrapper loadProperties() {
        StrictMode.ThreadPolicy oldPolicy = CwStrictMode.allowDiskWrites();

        // We init GservicesValue here, because this provider runs in the system process and
        // gkeys might not have been initialized, yet.
        GservicesValue.init(getContext());

        // used to ensure no duplicate paths and then used to build the UriMatcher
        SettingProperties[] properties = new PropertiesMap(this::getContext).toArray();

        // Generate UriMatcher and props records
        UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        for (int i = 0; i < properties.length; ++i) {
            uriMatcher.addURI(SettingsContract.SETTINGS_AUTHORITY,
                    properties[i].getPath(), i);
        }

        // Add an entry to keep track of whether a new backup is needed
        uriMatcher.addURI(SettingsContract.SETTINGS_AUTHORITY,
                SettingsBackupAgent.BACKUP_UPDATES_PATH, properties.length);

        CwStrictMode.restoreStrictMode(oldPolicy);

        return new PropertiesWrapper(uriMatcher, properties);
    }
}
