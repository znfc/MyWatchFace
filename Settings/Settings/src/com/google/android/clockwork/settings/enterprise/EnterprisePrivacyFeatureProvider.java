/**
 * Based on android phone settings:
 * packages/apps/Settings/src/com/android/settings/enterprise/EnterprisePrivacyFeatureProvider.java
 */
package com.google.android.clockwork.settings.enterprise;

import android.annotation.UserIdInt;
import android.content.Intent;

import java.util.Date;
import java.util.List;

public interface EnterprisePrivacyFeatureProvider {

    /**
     * Returns whether the device is managed by a Device Owner app.
     */
    boolean hasDeviceOwner();

    /**
     * Returns whether the device is in COMP mode (primary user managed by a Device Owner app and
     * work profile managed by a Profile Owner app).
     */
    boolean isInCompMode();

    /**
     * Returns the name of the organization managing the device via a Device Owner app. If the
     * device is not managed by a Device Owner app or the name of the managing organization was not
     * set, returns {@code null}.
     */
    String getDeviceOwnerOrganizationName();

    /**
     * Returns a message informing the user that the device is managed by a Device Owner app. The
     * message includes a Learn More link that takes the user to the enterprise privacy section of
     * Settings. If the device is not managed by a Device Owner app, returns {@code null}.
     */
    CharSequence getDeviceOwnerDisclosure();

    /**
     * Returns the time at which the Device Owner last retrieved security logs, or {@code null} if
     * logs were never retrieved by the Device Owner on this device.
     */
    Date getLastSecurityLogRetrievalTime();

    /**
     * Returns the time at which the Device Owner last requested a bug report, or {@code null} if no
     * bug report was ever requested by the Device Owner on this device.
     */
    Date getLastBugReportRequestTime();

    /**
     * Returns the time at which the Device Owner last retrieved network logs, or {@code null} if
     * logs were never retrieved by the Device Owner on this device.
     */
    Date getLastNetworkLogRetrievalTime();

    /**
     * Returns whether security logging is currently enabled.
     */
    boolean isSecurityLoggingEnabled();

    /**
     * Returns whether network logging is currently enabled.
     */
    boolean isNetworkLoggingEnabled();

    /**
     * Returns whether the Device Owner or Profile Owner in the current user set an always-on VPN.
     */
    boolean isAlwaysOnVpnSetInCurrentUser();

    /**
     * Returns whether the Profile Owner in the current user's managed profile (if any) set an
     * always-on VPN.
     */
    boolean isAlwaysOnVpnSetInManagedProfile();

    /**
     * Returns whether the Device Owner set a recommended global HTTP proxy.
     */
    boolean isGlobalHttpProxySet();

    /**
     * Returns the number of failed login attempts that the Device Owner or Profile Owner allows
     * before the current user is wiped, or zero if no such limit is set.
     */
    int getMaximumFailedPasswordsBeforeWipeInCurrentUser();

    /**
     * Returns the number of failed login attempts that the Profile Owner allows before the current
     * user's managed profile (if any) is wiped, or zero if no such limit is set.
     */
    int getMaximumFailedPasswordsBeforeWipeInManagedProfile();

    /**
     * Returns the label of the current user's input method if that input method was set by a Device
     * Owner or Profile Owner in that user. Otherwise, returns {@code null}.
     */
    String getImeLabelIfOwnerSet();

    /**
     * Returns the number of CA certificates that the Device Owner or Profile Owner installed in
     * current user.
     */
    int getNumberOfOwnerInstalledCaCertsForCurrentUser();

    /**
     * Returns the number of CA certificates that the Device Owner or Profile Owner installed in
     * the current user's managed profile  (if any).
     */
    int getNumberOfOwnerInstalledCaCertsForManagedProfile();

    /**
     * Returns the number of Device Admin apps active in the current user and the user's managed
     * profile (if any).
     */
    int getNumberOfActiveDeviceAdminsForCurrentUserAndManagedProfile();

    /**
     * Return the persistent preferred activities configured by the admin for the given user.
     * A persistent preferred activity is an activity that the admin configured to always handle a
     * given intent (e.g. open browser), even if the user has other apps installed that would also
     * be able to handle the intent.
     *
     * @param userId  ID of the user for which to find persistent preferred activities
     * @param intents The intents for which to find persistent preferred activities
     * @return the persistent preferred activities for the given intents, ordered first by user id,
     * then by package name
     */
    List<UserAppInfo> findPersistentPreferredActivities(@UserIdInt int userId, Intent[] intents);

    /**
     * Calculates the number of apps registered as default Intent handlers via policy.
     */
    int getNumberOfEnterpriseSetDefaultApps();

    /**
     * Asynchronously builds the list of apps installed in the current user and all its
     * managed profiles that have been granted one or more of the given permissions by the admin.
     *
     * @param permissions Only consider apps that have been granted one or more of these
     *                    permissions by the admin, either at run-time or install-time
     * @param callback    The callback to invoke with the result
     */
    void listAppsWithAdminGrantedPermissions(String[] permissions, ListOfAppsCallback callback);

    /**
     * Asynchronously builds the list of apps installed on the device via policy in the current user
     * and all its managed profiles.
     *
     * @param callback The callback to invoke with the result
     */
    void listPolicyInstalledApps(ListOfAppsCallback callback);

    interface ListOfAppsCallback {
        void onListOfAppsResult(List<UserAppInfo> result);
    }
}
