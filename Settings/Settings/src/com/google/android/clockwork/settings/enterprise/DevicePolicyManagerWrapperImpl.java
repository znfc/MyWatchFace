package com.google.android.clockwork.settings.enterprise;

import android.annotation.NonNull;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.os.UserHandle;
import android.support.annotation.Nullable;

import java.util.List;

/**
 * An implementation of the {@link DevicePolicyManagerWrapper} interface that calls the underlying
 * DevicePolicyManager methods replicated in DevicePolicyManagerWrapper.
 */
public class DevicePolicyManagerWrapperImpl implements DevicePolicyManagerWrapper {

    private final DevicePolicyManager mDpm;

    public DevicePolicyManagerWrapperImpl(DevicePolicyManager dpm) {
        mDpm = dpm;
    }

    @Override
    public ComponentName getDeviceOwnerComponentOnAnyUser() {
        return mDpm.getDeviceOwnerComponentOnAnyUser();
    }

    @Override
    public int getDeviceOwnerUserId() {
        return mDpm.getDeviceOwnerUserId();
    }

    @Override
    public boolean isCurrentInputMethodSetByOwner() {
        return mDpm.isCurrentInputMethodSetByOwner();
    }

    @Override
    public CharSequence getDeviceOwnerOrganizationName() {
        return mDpm.getDeviceOwnerOrganizationName();
    }

    @Override
    public long getLastSecurityLogRetrievalTime() {
        return mDpm.getLastSecurityLogRetrievalTime();
    }

    @Override
    public long getLastBugReportRequestTime() {
        return mDpm.getLastBugReportRequestTime();
    }

    @Override
    public long getLastNetworkLogRetrievalTime() {
        return mDpm.getLastNetworkLogRetrievalTime();
    }

    @Override
    public boolean isSecurityLoggingEnabled(@Nullable ComponentName admin) {
        return mDpm.isSecurityLoggingEnabled(admin);
    }

    @Override
    public boolean isNetworkLoggingEnabled(@Nullable ComponentName admin) {
        return mDpm.isNetworkLoggingEnabled(admin);
    }

    @Override
    public ComponentName getDeviceOwnerComponentOnCallingUser() {
        return mDpm.getDeviceOwnerComponentOnCallingUser();
    }

    @Override
    public @Nullable ComponentName getProfileOwnerAsUser(final int userId) {
        return mDpm.getProfileOwnerAsUser(userId);
    }

    @Override
    public int getMaximumFailedPasswordsForWipe(@Nullable ComponentName admin, int userHandle) {
        return mDpm.getMaximumFailedPasswordsForWipe(admin, userHandle);
    }

    @Override
    public List<String> getOwnerInstalledCaCerts(@NonNull UserHandle user) {
        return mDpm.getOwnerInstalledCaCerts(user);
    }

    @Override
    public @Nullable List<ComponentName> getActiveAdminsAsUser(int userId) {
        return mDpm.getActiveAdminsAsUser(userId);
    }

    @Override
    public int getPermissionGrantState(@Nullable ComponentName admin, String packageName,
            String permission) {
        return mDpm.getPermissionGrantState(admin, packageName, permission);
    }
}
