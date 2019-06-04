package com.google.android.clockwork.settings;

import android.content.Context;
import android.os.UserHandle;
import android.support.wearable.preference.AcceptDenySwitchPreference;
import android.util.AttributeSet;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

/**
 * RestrictedAcceptDenySwitchPreference is an AcceptDenySwitchPreference that
 * checks for an admin-specified restriction on given setting.  If it exists,
 * then onClick() shows the admin support dialog instead of performing its
 * onClick() action.
 */
public class RestrictedAcceptDenySwitchPreference extends AcceptDenySwitchPreference {

    private String mRestriction;

    public RestrictedAcceptDenySwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setRestriction(String restriction) {
        mRestriction = restriction;
    }

    @Override
    protected void onClick() {
        if (mRestriction != null) {
            final EnforcedAdmin enforcedAdmin =
                    RestrictedLockUtils.checkIfRestrictionEnforced(getContext(),
                              mRestriction, UserHandle.myUserId());
              if (enforcedAdmin != null) {
                  RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getContext(),
                      enforcedAdmin);
                  return;
              }
        }
        super.onClick();
    }
}
