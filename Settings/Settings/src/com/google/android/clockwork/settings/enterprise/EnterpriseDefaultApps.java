package com.google.android.clockwork.settings.enterprise;

import android.content.Intent;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;

/**
 * UI grouping of important intents that can be configured by device and profile owners.
 */
public enum EnterpriseDefaultApps {
    BROWSER(new Intent[] {
        buildIntent(Intent.ACTION_VIEW, Intent.CATEGORY_BROWSABLE, "http:", null)}),
    CALENDAR(new Intent[] {
        buildIntent(Intent.ACTION_INSERT, null, null, "vnd.android.cursor.dir/event")}),
    CAMERA(new Intent[] {
        new Intent(MediaStore.ACTION_IMAGE_CAPTURE),
        new Intent(MediaStore.ACTION_VIDEO_CAPTURE)}),
    CONTACTS(new Intent[] {
        buildIntent(Intent.ACTION_PICK, null, null, ContactsContract.Contacts.CONTENT_TYPE)}),
    EMAIL(new Intent[] {
        new Intent(Intent.ACTION_SENDTO), new Intent(Intent.ACTION_SEND),
        new Intent(Intent.ACTION_SEND_MULTIPLE)}),
    MAP(new Intent[] {buildIntent(Intent.ACTION_VIEW, null, "geo:", null)}),
    PHONE(new Intent[] {new Intent(Intent.ACTION_DIAL), new Intent(Intent.ACTION_CALL)});
    private final Intent[] mIntents;

    EnterpriseDefaultApps(Intent[] intents) {
        mIntents = intents;
    }

    public Intent[] getIntents() {
        return mIntents;
    }

    private static Intent buildIntent(String action, String category, String protocol,
            String type) {
        final Intent intent = new Intent(action);
        if (category != null) {
            intent.addCategory(category);
        }
        if (protocol != null) {
            intent.setData(Uri.parse(protocol));
        }
        if (type != null) {
            intent.setType(type);
        }
        return intent;
    }
}
