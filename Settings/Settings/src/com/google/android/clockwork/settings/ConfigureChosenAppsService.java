package com.google.android.clockwork.settings;

import android.app.AppGlobals;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.PatternMatcher;
import android.os.RemoteException;
import android.util.Log;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ConfigureChosenAppsService extends IntentService {

    private static final String TAG = "ConfigureChosenAppsSvc";

    private static final String SETTINGS_PACKAGE_NAME = "com.google.android.apps.wearable.settings";
    private static final String HOME_PACKAGE_NAME = "com.google.android.wearable.app";

    public ConfigureChosenAppsService() {
        super("ConfigureChosenAppsService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (!verifyIntent(intent)) {
            if (Log.isLoggable(TAG, Log.WARN)) {
                Log.w(TAG, "Intent from an unverified sender.");
            }
            return;
        }
        if (SettingsIntents.ACTION_CLEAR_LAST_CHOSEN_APP.equals(intent.getAction())) {
            handleClearLastChosenApp(intent);
        } else if (SettingsIntents.ACTION_SET_LAST_CHOSEN_APP.equals(intent.getAction())) {
            handleSetLastChosenApp(intent);
        }
    }

    private boolean verifyIntent(Intent intent) {
        final PendingIntent pendingIntent =
                intent.getParcelableExtra(SettingsIntents.EXTRA_PENDING_INTENT_KEY);
        if (pendingIntent != null) {
            final String packageName = pendingIntent.getCreatorPackage();
            return packageName.equals(SETTINGS_PACKAGE_NAME)
                    || packageName.equals(HOME_PACKAGE_NAME);
        } else {
            return false;
        }
    }

    private void handleClearLastChosenApp(Intent intent) {
        final Intent originalIntent = intent.getParcelableExtra(
                SettingsIntents.EXTRA_ORIGINAL_INTENT);
        if (originalIntent == null) {
            return;
        }
        final IntentFilter filter = new IntentFilter(originalIntent.getAction());
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        final ComponentName bogusComponentName = new ComponentName("", "");
        try {
            // A hack to trick the PackageManager into forgetting all preferred activities for
            // the given action. This hack depends on the fact that setLastChosenActivity first
            // wipes out all current preferred activities before setting a new (bogus in our
            // case) activity.
            // TODO: In L there is a new PackageManager#removePreferredActivity we can use
            // for this instead.
            AppGlobals.getPackageManager().setLastChosenActivity(originalIntent, null,
                    PackageManager.MATCH_DEFAULT_ONLY, filter, 0, bogusComponentName);
        } catch (RemoteException re) {
            if (Log.isLoggable(TAG, Log.ERROR)) {
                Log.e(TAG, "Error calling setLastChosenActivity", re);
            }
        }
    }

    private void handleSetLastChosenApp(Intent intent) {
        final Intent originalIntent = intent.getParcelableExtra(
                SettingsIntents.EXTRA_ORIGINAL_INTENT);
        if (originalIntent == null) {
            return;
        }
        final ComponentName newActivityComponentName = intent.getParcelableExtra(
                SettingsIntents.EXTRA_COMPONENT_NAME);
        if (newActivityComponentName == null) {
            return;
        }

        originalIntent.setComponent(null);

        final PackageManager packageManager = getApplicationContext().getPackageManager();
        final List<ResolveInfo> activities = packageManager.queryIntentActivities(originalIntent,
                PackageManager.MATCH_DEFAULT_ONLY | PackageManager.GET_RESOLVED_FILTER);
        final ResolveInfo currentPreferredActivity = getPreferredActivityForIntent(originalIntent,
                activities);

        // Don't bother setting a new preferred activity if it is the same as the currently
        // preferred activity.
        if (currentPreferredActivity != null
                && getComponentName(currentPreferredActivity).equals(newActivityComponentName)) {
            return;
        }

        final ResolveInfo newPreferredActivity =
                findActivityWithComponentName(newActivityComponentName, activities);
        if (newPreferredActivity == null) {
            Log.e(TAG, "Couldn't find new preferred activity.");
            return;
        }

        // replacePreferredActivity needs the set of all possible Activities. Compute the best match
        // score along the way.
        final int activitiesCount = activities.size();
        ComponentName[] set = new ComponentName[activitiesCount];
        int bestMatch = 0;
        for (int i = 0; i < activitiesCount; i++) {
            ResolveInfo activity = activities.get(i);
            set[i] = getComponentName(activity);
            if (activity.match > bestMatch) {
                bestMatch = activity.match;
            }
        }

        final IntentFilter filter = createIntentFilterForActivity(newPreferredActivity,
                originalIntent);
        try {
            // Setting the preferred activity for an intent turns out to be somewhat finicky (at
            // least as far as I can determine). First we need to clear any currently set
            // preferred activity, which is achieved by calling setLastChosenActivity with an
            // empty component name. Then we can call addPreferredActivity with the new
            // component name.
            // TODO: Figure out how to do this correctly and fix it in B.
            ComponentName emptyComponentName = new ComponentName("", "");
            AppGlobals.getPackageManager().setLastChosenActivity(originalIntent,
                    originalIntent.resolveTypeIfNeeded(getContentResolver()),
                    PackageManager.MATCH_DEFAULT_ONLY,
                    filter, bestMatch, emptyComponentName);
            getPackageManager().addPreferredActivity(filter, bestMatch, set,
                    newActivityComponentName);
        } catch (RemoteException re) {
            if (Log.isLoggable(TAG, Log.ERROR)) {
                Log.e(TAG, "Error calling replacePreferredActivity", re);
            }
        }
    }

    private ResolveInfo getPreferredActivityForIntent(Intent intent, List<ResolveInfo> candidates) {
        final ResolveInfo activity = getApplicationContext().getPackageManager()
                .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (activity == null || activity.activityInfo == null) {
            return null;
        }
        // Make sure the Activity we just found is among the candidates (if, for example, the
        // Activity is ResolverActivity, then it won't be among the candidates).
        if (findActivityWithComponentName(getComponentName(activity), candidates) != null) {
            return activity;
        } else {
            return null;
        }
    }

    private ResolveInfo findActivityWithComponentName(
            ComponentName name, List<ResolveInfo> activities) {
        for (final ResolveInfo activity : activities) {
            if (activity.activityInfo != null) {
                if (activity.activityInfo.packageName.equals(name.getPackageName())
                        && activity.activityInfo.name.equals(name.getClassName())) {
                    return activity;
                }
            }
        }
        return null;
    }

    /**
     * Return an appropriate IntentFilter for the ResolveInfo of an activity and an Intent
     * intended to match that activity.
     */
    private IntentFilter createIntentFilterForActivity(ResolveInfo activity, Intent intent) {
        // This code was copied from ResolverActivity in order to match what it does for Intent
        // resolution.
        IntentFilter filter = new IntentFilter();

        if (intent.getAction() != null) {
            filter.addAction(intent.getAction());
        }
        Set<String> categories = intent.getCategories();
        if (categories != null) {
            for (String cat : categories) {
                filter.addCategory(cat);
            }
        }
        filter.addCategory(Intent.CATEGORY_DEFAULT);

        int cat = activity.match & IntentFilter.MATCH_CATEGORY_MASK;
        Uri data = intent.getData();
        if (cat == IntentFilter.MATCH_CATEGORY_TYPE) {
            String mimeType = intent.resolveType(this);
            if (mimeType != null) {
                try {
                    filter.addDataType(mimeType);
                } catch (IntentFilter.MalformedMimeTypeException e) {
                    Log.w("ResolverActivity", e);
                    filter = null;
                }
            }
        }

        // Because we use PackageManager.GET_RESOLVED_FILTER when getting the activity list
        // I don't believe it is possible for activity.filter to be null, but testing anyway
        // just in case.
        if (data != null && data.getScheme() != null && activity.filter != null) {
            // We need the data specification if there was no type,
            // OR if the scheme is not one of our magical "file:"
            // or "content:" schemes (see IntentFilter for the reason).
            if (cat != IntentFilter.MATCH_CATEGORY_TYPE
                    || (!"file".equals(data.getScheme())
                    && !"content".equals(data.getScheme()))) {
                filter.addDataScheme(data.getScheme());

                // Look through the resolved filter to determine which part
                // of it matched the original Intent.
                Iterator<PatternMatcher> pIt = activity.filter.schemeSpecificPartsIterator();
                if (pIt != null) {
                    String ssp = data.getSchemeSpecificPart();
                    while (ssp != null && pIt.hasNext()) {
                        PatternMatcher p = pIt.next();
                        if (p.match(ssp)) {
                            filter.addDataSchemeSpecificPart(p.getPath(), p.getType());
                            break;
                        }
                    }
                }
                Iterator<IntentFilter.AuthorityEntry> aIt = activity.filter.authoritiesIterator();
                if (aIt != null) {
                    while (aIt.hasNext()) {
                        IntentFilter.AuthorityEntry a = aIt.next();
                        if (a.match(data) >= 0) {
                            int port = a.getPort();
                            filter.addDataAuthority(a.getHost(),
                                    port >= 0 ? Integer.toString(port) : null);
                            break;
                        }
                    }
                }
                pIt = activity.filter.pathsIterator();
                if (pIt != null) {
                    String path = data.getPath();
                    while (path != null && pIt.hasNext()) {
                        PatternMatcher p = pIt.next();
                        if (p.match(path)) {
                            filter.addDataPath(p.getPath(), p.getType());
                            break;
                        }
                    }
                }
            }
        }

        return filter;
    }

    private ComponentName getComponentName(ResolveInfo info) {
        return info != null && info.activityInfo != null
                ? new ComponentName(info.activityInfo.packageName, info.activityInfo.name)
                : null;
    }

}
