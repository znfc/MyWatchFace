package com.google.android.clockwork.settings.cellular;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.preference.ListPreference;
import android.provider.BaseColumns;
import android.provider.Telephony;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import com.android.internal.telephony.dataconnection.ApnSetting;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccController;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.common.concurrent.CwAsyncTask;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Preference for APN settings.
 */
public class ApnSettingsPreference extends ListPreference {
    public static final String TAG = ApnSettingsPreference.class.getSimpleName();

    public static final String PREFERRED_APN_URI = "content://telephony/carriers/preferapn";
    private static final Uri PREFERAPN_URI = Uri.parse(PREFERRED_APN_URI);

    private static final String COLUMN_APN_ID = "apn_id";

    private HashMap<String, ApnData> mApns;
    private ApnData mCurrentApn;

    public ApnSettingsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ApnSettingsPreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        mApns = new HashMap<String, ApnData>();

        setTitle(R.string.access_point_names_action);
        setIcon(R.drawable.ic_settings_access_point_names);
        setPersistent(false);
        setDialogTitle(R.string.access_point_names_action);

        setOnPreferenceChangeListener((p, newVal) -> {
            final String newApnIndex = (String) newVal;
            final ApnData apn = mApns.get(newApnIndex);

            boolean selectedSelectedApn =
                    mCurrentApn != null && mCurrentApn.primaryKey.equals(newApnIndex);
            // Selecting the same APN presents a view of the APN
            if (!apn.selectable || selectedSelectedApn) {
                getContext().startActivity(
                        new Intent(
                                Intent.ACTION_VIEW,
                                ContentUris.withAppendedId(
                                        Telephony.Carriers.CONTENT_URI,
                                        Integer.parseInt(newApnIndex)
                                )
                        )
                );
            } else {
                setSelectedApn(apn);
            }
            return true;
        });

        // Request the APN entries from the content subsystem
        new FillListTask().submit();
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        // Hide the cancel button.
        builder.setNegativeButton(null, null);
    }

    /**
     * Given a current mcc/mnc and a subscription ID, query the resolver for
     * all possible APNs that work on this cellular network.
     */
    private void populateApnList() {
        final TelephonyManager tm = getContext().getSystemService(TelephonyManager.class);
        final int subId = SubscriptionManager.from(getContext()).getDefaultSubscriptionId();
        final String mccmnc = tm.getSimOperator(subId);
        final ContentResolver resolver = getContext().getContentResolver();

        mApns.clear();

        final String where = "numeric=\""
            + mccmnc
            + "\" AND NOT (type='ia' AND (apn=\"\" OR apn IS NULL))";

        final Cursor cursor = resolver.query(
                Telephony.Carriers.CONTENT_URI, new String[] {
                BaseColumns._ID,
                Telephony.Carriers.NAME,
                Telephony.Carriers.APN,
                Telephony.Carriers.TYPE,
                Telephony.Carriers.MVNO_TYPE,
                Telephony.Carriers.MVNO_MATCH_DATA }, where, null,
                Telephony.Carriers.DEFAULT_SORT_ORDER);

        // Ordered based upon the database query
        final int ID_INDEX = 0;
        final int NAME_INDEX = 1;
        final int APN_INDEX = 2;
        final int TYPES_INDEX = 3;
        final int MVNO_TYPE_INDEX = 4;
        final int MVNO_MATCH_DATA_INDEX = 5;

        if (cursor != null) {
            debugLog("where=" + where);
            debugLog("cursor=" + DatabaseUtils.dumpCursorToString(cursor));

            IccRecords iccRecords = null;
            final UiccController uicc = UiccController.getInstance();
            if (uicc != null) {
                iccRecords = uicc.getIccRecords(SubscriptionManager.getPhoneId(subId),
                        UiccController.APP_FAM_3GPP);
            }

            ArrayList<ApnData> mnoApnList = new ArrayList<>();
            ArrayList<ApnData> mvnoApnList = new ArrayList<>();

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                final String type = cursor.getString(TYPES_INDEX);
                final boolean isMmsApn = ((type != null) && type.equals("mms"));

                ApnData apn = new ApnData(
                        cursor.getString(ID_INDEX),
                        cursor.getString(NAME_INDEX),
                        cursor.getString(APN_INDEX),
                        !isMmsApn);

                final String mvnoType = cursor.getString(MVNO_TYPE_INDEX);
                final String mvnoMatchData = cursor.getString(MVNO_MATCH_DATA_INDEX);
                boolean isForMvno = !TextUtils.isEmpty(mvnoType) &&
                        !TextUtils.isEmpty(mvnoMatchData);

                if (isForMvno) {
                    if (iccRecords != null &&
                            ApnSetting.mvnoMatches(iccRecords, mvnoType, mvnoMatchData)) {
                        mvnoApnList.add(apn);
                    }
                } else {
                    mnoApnList.add(apn);
                }

                cursor.moveToNext();
            }
            cursor.close();

            if (!mvnoApnList.isEmpty()) {
                mnoApnList = mvnoApnList;
            }

            for (ApnData apn : mnoApnList) {
                mApns.put(apn.primaryKey, apn);
            }
            mCurrentApn = mApns.get(getSelectedApnKey(resolver));
        }
    }

    /**
     * Query the content resolver to get the current APN primary key
     *
     * Returns the primary key in the content DB of the currently selected APN.
     */
    private String getSelectedApnKey(final ContentResolver resolver) {
        Cursor cursor = resolver.query(PREFERAPN_URI,
                new String[] {BaseColumns._ID},
                null, null, Telephony.Carriers.DEFAULT_SORT_ORDER);

        // Ordered based upon the database query
        final int ID_INDEX = 0;

        String key = null;
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            key = cursor.getString(ID_INDEX);
        }
        cursor.close();

        debugLog("getSelectedApnKey()=" + key);
        return key;
    }

    /**
     * Store the current default APN primary key back into the content resolver
     */
    private void setSelectedApn(ApnData apn) {
        debugLog("setSelectedApnKey() key=" + apn.primaryKey);
        mCurrentApn = apn;

        ContentValues values = new ContentValues();
        values.put(COLUMN_APN_ID, mCurrentApn.primaryKey);
        getContext().getContentResolver().update(PREFERAPN_URI, values, null, null);

        setSummary(apn.name);
    }

    /**
     * Use AsyncTask to avoid disk access in the main thread.
     */
    private class FillListTask extends CwAsyncTask<Void, Void, Void> {
        public FillListTask() {
            super("ApnSettingsFillListTask");
        }

        @Override
        public Void doInBackground(Void... params) {
            populateApnList();
            return null;
        }

        @Override
        public void onPostExecute(Void result) {
            String[] entryArr = new String[mApns.size()];
            String[] valueArr = new String[mApns.size()];
            Iterator<ApnData> apnIterator = mApns.values().iterator();
            for (int i = 0; i < mApns.size(); i++) {
                ApnData apn = apnIterator.next();
                entryArr[i] = apn.name + "\n" + apn.apn;
                valueArr[i] = apn.primaryKey;
            }
            setEntries(entryArr);
            setEntryValues(valueArr);

            setValue(mCurrentApn == null ? null : mCurrentApn.primaryKey);
            setSummary(mCurrentApn == null ? null : mCurrentApn.name);
        }
    }

    private void debugLog(final String s) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, s);
        }
    }

    /**
     * Internal structure to hold APN data
     */
    private static class ApnData {
        String primaryKey;
        String name;
        String apn;
        boolean selectable;

        public ApnData(String primaryKey, String name, String apn, boolean selectable) {
            this.primaryKey = primaryKey;
            this.name = name;
            this.apn = apn;
            this.selectable = selectable;
        }
    }
}
