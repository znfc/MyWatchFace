package com.google.android.clockwork.settings.cellular;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.InfoListItemInitializer;

import java.util.HashSet;

public class ApnDetailActivity extends ListActivity {

    private static String sNotSet;
    private Uri mUri;
    private Cursor mCursor;
    private InfoListItemInitializer mInfoListItemInitializer;
    private TelephonyManager mTelephonyManager;
    private ArrayAdapter<Item> mAdapter;

    /**
     * Standard projection for the interesting columns of a normal note.
     */
    private static final String[] sProjection = new String[] {
            Telephony.Carriers._ID,     // 0
            Telephony.Carriers.NAME,    // 1
            Telephony.Carriers.APN,     // 2
            Telephony.Carriers.PROXY,   // 3
            Telephony.Carriers.PORT,    // 4
            Telephony.Carriers.USER,    // 5
            Telephony.Carriers.SERVER,  // 6
            Telephony.Carriers.PASSWORD, // 7
            Telephony.Carriers.MMSC, // 8
            Telephony.Carriers.MCC, // 9
            Telephony.Carriers.MNC, // 10
            Telephony.Carriers.NUMERIC, // 11
            Telephony.Carriers.MMSPROXY,// 12
            Telephony.Carriers.MMSPORT, // 13
            Telephony.Carriers.AUTH_TYPE, // 14
            Telephony.Carriers.TYPE, // 15
            Telephony.Carriers.PROTOCOL, // 16
            Telephony.Carriers.CARRIER_ENABLED, // 17
            Telephony.Carriers.BEARER, // 18
            Telephony.Carriers.BEARER_BITMASK, // 19
            Telephony.Carriers.ROAMING_PROTOCOL, // 20
            Telephony.Carriers.MVNO_TYPE,   // 21
            Telephony.Carriers.MVNO_MATCH_DATA  // 22
    };

    private static final int ID_INDEX = 0;
    private static final int NAME_INDEX = 1;
    private static final int APN_INDEX = 2;
    private static final int PROXY_INDEX = 3;
    private static final int PORT_INDEX = 4;
    private static final int USER_INDEX = 5;
    private static final int SERVER_INDEX = 6;
    private static final int PASSWORD_INDEX = 7;
    private static final int MMSC_INDEX = 8;
    private static final int MCC_INDEX = 9;
    private static final int MNC_INDEX = 10;
    private static final int MMSPROXY_INDEX = 12;
    private static final int MMSPORT_INDEX = 13;
    private static final int AUTH_TYPE_INDEX = 14;
    private static final int TYPE_INDEX = 15;
    private static final int PROTOCOL_INDEX = 16;
    private static final int CARRIER_ENABLED_INDEX = 17;
    private static final int BEARER_INDEX = 18;
    private static final int BEARER_BITMASK_INDEX = 19;
    private static final int ROAMING_PROTOCOL_INDEX = 20;
    private static final int MVNO_TYPE_INDEX = 21;
    private static final int MVNO_MATCH_DATA_INDEX = 22;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            finish();
            return;
        }

        sNotSet = getResources().getString(R.string.apn_not_set);

        mUri = getIntent().getData();
        mInfoListItemInitializer = new InfoListItemInitializer(this, true);

        mCursor = managedQuery(mUri, sProjection, null, null);
        mCursor.moveToFirst();

        mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        mAdapter = new ArrayAdapter<Item>(this, R.layout.info_list_item,
                R.id.title) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(parent.getContext()).inflate(
                            R.layout.info_list_item, parent, false);
                }
                Item item = getItem(position);
                mInfoListItemInitializer.initListItemView(
                        convertView, position, mAdapter.getCount(), item.title, item.value);
                return convertView;
            }
        };

        mAdapter.add(new Item(getString(R.string.apn_name), getSummary(mCursor.getString(
                NAME_INDEX))));
        mAdapter.add(new Item(getString(R.string.apn_apn), getSummary(mCursor.getString(
                APN_INDEX))));
        mAdapter.add(new Item(getString(R.string.apn_http_proxy), getSummary(mCursor.getString(
                PROXY_INDEX))));
        mAdapter.add(new Item(getString(R.string.apn_http_port), getSummary(mCursor.getString(
                PORT_INDEX))));
        mAdapter.add(new Item(getString(R.string.apn_user), getSummary(mCursor.getString(
                USER_INDEX))));
        mAdapter.add(new Item(getString(R.string.apn_password), getSummary(mCursor.getString(
                PASSWORD_INDEX))));
        mAdapter.add(new Item(getString(R.string.apn_server), getSummary(mCursor.getString(
                SERVER_INDEX))));
        mAdapter.add(new Item(getString(R.string.apn_mmsc), getSummary(mCursor.getString(
                MMSC_INDEX))));
        mAdapter.add(new Item(getString(R.string.apn_mms_proxy), getSummary(mCursor.getString(
                MMSPROXY_INDEX))));
        mAdapter.add(new Item(getString(R.string.apn_mms_port), getSummary(mCursor.getString(
                MMSPORT_INDEX))));
        mAdapter.add(new Item(getString(R.string.apn_mcc), getSummary(mCursor.getString(
                MCC_INDEX))));
        mAdapter.add(new Item(getString(R.string.apn_mnc), getSummary(mCursor.getString(
                MNC_INDEX))));
        mAdapter.add(new Item(getString(R.string.apn_auth_type), getSummary(
                mCursor.getInt(AUTH_TYPE_INDEX),
                getResources().getStringArray(R.array.apn_auth_entries))));
        mAdapter.add(new Item(getString(R.string.apn_type), getSummary(mCursor.getString(
                TYPE_INDEX))));
        mAdapter.add(new Item(getString(R.string.apn_protocol), getProtocolSummary(
                mCursor.getString(PROTOCOL_INDEX))));
        mAdapter.add(new Item(getString(R.string.apn_roaming_protocol), getProtocolSummary(
                mCursor.getString(ROAMING_PROTOCOL_INDEX))));
        mAdapter.add(new Item(getString(R.string.carrier_enabled),
                mCursor.getInt(CARRIER_ENABLED_INDEX) == 1 ?
                        getString(R.string.carrier_enabled_summaryOn) :
                        getString(R.string.carrier_enabled_summaryOff)));
        mAdapter.add(new Item(getString(R.string.bearer), getBearerSummary()));
        mAdapter.add(new Item(getString(R.string.mvno_type), getSummary(
                mCursor.getString(MVNO_TYPE_INDEX))));
        mAdapter.add(new Item(getString(R.string.mvno_match_data), getSummary(
                mCursor.getString(MVNO_MATCH_DATA_INDEX))));

        ListView listView = getListView();
        listView.setDivider(null);
        listView.setVerticalScrollBarEnabled(false);
        setListAdapter(mAdapter);
    }

    private String getSummary(final String value) {
        return TextUtils.isEmpty(value) ? sNotSet : value;
    }

    private String getSummary(final int index, final String[] entries) {
        return (index < 0) ? sNotSet : entries[index];
    }

    private String getProtocolSummary(final String protocol) {
        final String[] entries = getResources().getStringArray(R.array.apn_protocol_entries);
        final String[] values = getResources().getStringArray(R.array.apn_protocol_values);
        for (int i = 0; i < values.length; ++i) {
            if (values[i].equals(protocol)) {
                return entries[i];
            }
        }

        return getSummary(protocol);
    }

    private String getBearerSummary() {
        final int bearerInitialVal = mCursor.getInt(BEARER_INDEX);

        HashSet<String> bearers = new HashSet<String>();
        int bearerBitmask = mCursor.getInt(BEARER_BITMASK_INDEX);
        if (bearerBitmask == 0) {
            if (bearerInitialVal == 0) {
                bearers.add("" + 0);
            }
        } else {
            int i = 1;
            while (bearerBitmask != 0) {
                if ((bearerBitmask & 1) == 1) {
                    bearers.add("" + i);
                }
                bearerBitmask >>= 1;
                i++;
            }
        }

        if (bearerInitialVal != 0 && bearers.contains("" + bearerInitialVal) == false) {
            // add bearerInitialVal to bearers
            bearers.add("" + bearerInitialVal);
        }

        final String[] bearerValues = getResources().getStringArray(R.array.bearer_values);
        final String[] bearerEntries = getResources().getStringArray(R.array.bearer_entries);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bearerValues.length; i++) {
            if (bearers.contains(bearerValues[i])) {
                sb.append(bearerEntries[i]);
                sb.append(",");
            }
        }
        if (sb.length() > 0) { // remove the trailing comma
            sb.deleteCharAt(sb.length() - 1);
        }

        return sb.toString();
    }

    class Item {
        final String title;
        final String value;

        public Item(String title, String value) {
            this.title = title;
            this.value = value;
        }
    }
}
