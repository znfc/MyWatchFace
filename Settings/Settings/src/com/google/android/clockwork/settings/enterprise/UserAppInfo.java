package com.google.android.clockwork.settings.enterprise;

import android.content.pm.ApplicationInfo;
import android.content.pm.UserInfo;
import android.text.TextUtils;

import java.util.Objects;

/**
 * Simple class for bringing together information about application and user for which it was
 * installed.
 */
public class UserAppInfo {
    public final UserInfo userInfo;
    public final ApplicationInfo appInfo;

    public UserAppInfo(UserInfo mUserInfo, ApplicationInfo mAppInfo) {
        this.userInfo = mUserInfo;
        this.appInfo = mAppInfo;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        final UserAppInfo that = (UserAppInfo) other;

        // As UserInfo and AppInfo do not support hashcode/equals contract, assume
        // equality based on corresponding identity fields.
        return that.userInfo.id == userInfo.id && TextUtils.equals(that.appInfo.packageName,
                appInfo.packageName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userInfo.id, appInfo.packageName);
    }
}
