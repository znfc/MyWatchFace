/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.clockwork.settings.keyguard;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.AcceptDenyDialog;

import com.android.internal.widget.LockPatternUtils;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.common.systemui.SystemUIContract;

/**
 * Removes device lock from the user's device.
 */
public class RemoveLockActivity extends WearableActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String currentPassword;
        if (getIntent() != null
            && getIntent().hasExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD)) {
            currentPassword =
                getIntent().getStringExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD);
        } else {
            currentPassword = null;
        }

        AcceptDenyDialog diag = new AcceptDenyDialog(this);
        if (hasPayTokens()){
            diag.setTitle(getString(R.string.screen_lock_remove_pay_dialog_title));
            diag.setMessage(getString(R.string.screen_lock_remove_pay_dialog_text));
        } else {
            diag.setTitle(getString(R.string.screen_lock_remove_dialog));
        }

        diag.setPositiveButton((dialog, which) -> {
            new LockPatternUtils(this).clearLock(currentPassword, UserHandle.myUserId());

            // Broadcast the keyguard password state has changed
            sendBroadcast(new Intent(SystemUIContract.ACTION_KEYGUARD_PASSWORD_SET)
                    .putExtra(SystemUIContract.EXTRA_PASSWORD_SET, false));
            setResult(RESULT_OK);
        });
        diag.setNegativeButton((dialog, which) -> { /* do nothing */ });
        diag.setOnDismissListener((dialog) -> finish());
        diag.show();
    }

    private boolean hasPayTokens() {
        Cursor cursor = getContentResolver().query(
            SettingsContract.TAP_AND_PAY_PATH_URI, null, null, null, null);
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    if (SettingsContract.KEY_HAS_PAY_TOKENS.equals(cursor.getString(0))) {
                        return cursor.getInt(1) > 0;
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return false;
    }
}
