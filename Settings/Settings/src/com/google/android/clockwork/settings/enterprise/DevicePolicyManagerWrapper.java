package com.google.android.clockwork.settings.enterprise;

import android.annotation.NonNull;
import android.content.ComponentName;
import android.os.UserHandle;
import android.support.annotation.Nullable;

import java.util.List;

/**
 * This interface replicates a subset of the android.app.DevicePolicyManager (DPM). The interface
 * exists so that we can use a thin wrapper around the DPM in production code and a mock in tests.
 * We cannot directly mock or shadow the DPM because some of the methods we rely on are newer than
 * the API version supported by Robolectric.
 */
public interface DevicePolicyManagerWrapper {

    public ComponentName getDeviceOwnerComponentOnAnyUser();

    public int getDeviceOwnerUserId();

    public boolean isCurrentInputMethodSetByOwner();

    CharSequence getDeviceOwnerOrganizationName();

    long getLastSecurityLogRetrievalTime();

    long getLastBugReportRequestTime();

    long getLastNetworkLogRetrievalTime();

    boolean isSecurityLoggingEnabled(@Nullable ComponentName admin);

    boolean isNetworkLoggingEnabled(@Nullable ComponentName admin);

    ComponentName getDeviceOwnerComponentOnCallingUser();

    @Nullable ComponentName getProfileOwnerAsUser(final int userId);

    int getMaximumFailedPasswordsForWipe(@Nullable ComponentName admin, int userHandle);

    List<String> getOwnerInstalledCaCerts(@NonNull UserHandle user);

    public @Nullable List<ComponentName> getActiveAdminsAsUser(int userId);

    int getPermissionGrantState(@Nullable ComponentName admin, String packageName,
            String permission);
}
