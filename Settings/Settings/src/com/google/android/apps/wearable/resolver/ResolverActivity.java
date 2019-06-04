package com.google.android.apps.wearable.resolver;


import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.PatternMatcher;
import android.os.RemoteException;
import android.os.UserHandle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.WearableListView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.TextView;

import com.android.internal.content.PackageMonitor;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.VoiceActions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

// NOTE: A lot of the code in this activity has been copied from the ResolverActivity in AtHome,
// which was copied from original framework ResolverActivity:
// com.android.internal.app.ResolverActivity. Isn't reusing code great?
/**
 * Activity displaying remote information allowing remote settings to be set.
 */
public class ResolverActivity extends WearableActivity implements WearableListView.ClickListener,
        WearableListView.OnScrollListener, View.OnApplyWindowInsetsListener {

    private static final boolean DEBUG = false;
    private static final String TAG = "ResolverActivity";

    private ResolveListHelper mListHelper;
    private PackageManager mPm;
    private boolean mAlwaysUseOption;

    private boolean mRegistered;

    private View mHeader;

    private final PackageMonitor mPackageMonitor = new PackageMonitor() {
        @Override
        public void onSomePackagesChanged() {
            mListHelper.handlePackagesChanged();
        }
    };

    private Intent makeMyIntent() {
        Intent intent = new Intent(getIntent());
        // The resolver activity is set to be hidden from recent tasks.
        // we don't want this attribute to be propagated to the next activity
        // being launched. Note that if the original Intent also had this
        // flag set, we are now losing it. That should be a very rare case
        // and we can live with this.
        intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        onCreate(savedInstanceState, makeMyIntent(), null, null, true);
    }

    protected void onCreate(Bundle savedInstanceState, Intent intent,
            Intent[] initialIntents, List<ResolveInfo> rList,
            boolean alwaysUseOption) {

        if (DEBUG) {
            Log.d(TAG, "onCreate. Initial Intent: " + intent);
        }
        int launchedFromUid = 0;
        try {
            launchedFromUid = ActivityManager.getService().getLaunchedFromUid(
                    getActivityToken());
        } catch (RemoteException e) {
            launchedFromUid = -1;
        }
        mPm = getPackageManager();
        mAlwaysUseOption = alwaysUseOption;
        intent.setComponent(null);

        mPackageMonitor.register(this, getMainLooper(), false);
        mRegistered = true;

        mListHelper = new ResolveListHelper(intent, initialIntents, rList, launchedFromUid);
        int count = mListHelper.getCount();

        super.onCreate(savedInstanceState);

        boolean finishNow = false;

        if (launchedFromUid < 0 || UserHandle.isIsolated(launchedFromUid)) {
            finishNow = true;
        } else if (count == 1) {
            if (DEBUG) {
                Log.d(TAG, "Starting Activity with Intent: " + mListHelper.intentForPosition(0));
            }
            startActivity(mListHelper.intentForPosition(0));
            finishNow = true;
        }

        if (finishNow) {
            mPackageMonitor.unregister();
            mRegistered = false;
            finish();
        } else {
            IntentsAdapter adapter = new IntentsAdapter(this, mListHelper);
            setContentView(R.layout.resolver_activity);
            WearableListView wof = (WearableListView) findViewById(
                    R.id.resolver_activity_wheel_of_fortune);
            wof.setClipChildren(false);
            wof.setAdapter(adapter);
            wof.setClickListener(this);
            wof.addOnScrollListener(this);

            mHeader = findViewById(R.id.resolver_activity_header);

            // Scale the header minHeight smaller for small screens. Capped at 64dp.
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            mHeader.setMinimumHeight((int) Math.min(
                    metrics.heightPixels * 0.27f,
                    metrics.densityDpi * 64 / 160));

            mHeader.setOnApplyWindowInsetsListener(this);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (!mRegistered) {
            mPackageMonitor.register(this, getMainLooper(), false);
            mRegistered = true;
        }
        mListHelper.handlePackagesChanged();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mRegistered) {
            mPackageMonitor.unregister();
            mRegistered = false;
        }
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0) {
            // This resolver is in the unusual situation where it has been
            // launched at the top of a new task. We don't let it be added
            // to the recent tasks shown to the user, and we need to make sure
            // that each time we are launched we get the correct launching
            // uid (not re-using the same resolver from an old launching uid),
            // so we will now finish ourself since being no longer visible,
            // the user probably can't get back to us.
            if (!isChangingConfigurations()) {
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    public void onIntentSelected(ResolveInfo ri, Intent intent, boolean alwaysCheck) {
        if (alwaysCheck) {
            // Build a reasonable intent filter, based on what matched.
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
            int cat = ri.match & IntentFilter.MATCH_CATEGORY_MASK;
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
            if (data != null && data.getScheme() != null) {
                // We need the data specification if there was no type,
                // OR if the scheme is not one of our magical "file:"
                // or "content:" schemes (see IntentFilter for the reason).
                if (cat != IntentFilter.MATCH_CATEGORY_TYPE
                        || (!"file".equals(data.getScheme())
                        && !"content".equals(data.getScheme()))) {
                    filter.addDataScheme(data.getScheme());

                    // Look through the resolved filter to determine which part
                    // of it matched the original Intent.
                    Iterator<PatternMatcher> pIt = ri.filter.schemeSpecificPartsIterator();
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
                    Iterator<IntentFilter.AuthorityEntry> aIt = ri.filter.authoritiesIterator();
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
                    pIt = ri.filter.pathsIterator();
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

            if (filter != null) {
                final int N = mListHelper.mList.size();
                final List<ResolveInfo> activityList = new ArrayList(N);
                ComponentName[] set = new ComponentName[N];
                int bestMatch = 0;
                for (int i = 0; i < N; i++) {
                    ResolveInfo r = mListHelper.mList.get(i).ri;
                    set[i] = new ComponentName(r.activityInfo.packageName, r.activityInfo.name);
                    if (r.match > bestMatch)
                        bestMatch = r.match;
                    activityList.add(r);
                }
                getPackageManager().addPreferredActivity(filter, bestMatch, set,
                        intent.getComponent());

                VoiceActions.setDisambiguationInfoForAction(
                        getApplicationContext(), activityList, intent);
            }
        }

        if (intent != null) {
            startActivity(intent);
        }
    }

    @Override
    public void onClick(WearableListView.ViewHolder holder) {
        final int which = holder.getPosition();
        final ResolveInfo ri = mListHelper.resolveInfoForPosition(which);
        final Intent intent = mListHelper.intentForPosition(which);
        onIntentSelected(ri, intent, true);
        finish();
    }

    @Override
    public void onTopEmptyRegionClick() {

    }

    @Override // WheelOfFortune.OnScrollListener
    public void onScroll(int scroll) {
    }

    @Override // WheelOfFortune.OnScrollListener
    public void onAbsoluteScrollChange(int scroll) {
        if (mHeader == null) {
            return;
        }
        final float y = -clamp(scroll, 0, mHeader.getHeight());
        mHeader.setTranslationY(y);
    }

    @Override // WheelOfFortune.OnScrollListener
    public void onScrollStateChanged(int scrollState) {
    }

    @Override // WheelOfFortune.OnScrollListener
    public void onCentralPositionChanged(int centralPosition) {
    }

    // TODO: This is from MathUtil in Home. Probably need to move MathUtil somewhere shared.
    private float clamp(float val, float min, float max) {
        if (val < min) {
            return min;
        } else if (val > max) {
            return max;
        } else {
            return val;
        }
    }

    @Override
    public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
        if (insets.isRound()) {
            TextView view = (TextView) v;
            int sideSpacing = getResources().getDimensionPixelSize(
                    R.dimen.resolver_side_spacing_round);
            view.setPadding(sideSpacing, v.getPaddingTop(), sideSpacing, v.getPaddingBottom());
            view.setGravity(Gravity.CENTER);
        }
        return insets;
    }

    private final class DisplayResolveInfo {
        ResolveInfo ri;
        CharSequence displayLabel;
        CharSequence extendedInfo;
        Intent origIntent;

        DisplayResolveInfo(ResolveInfo pri, CharSequence pLabel,
                CharSequence pInfo, Intent pOrigIntent) {
            ri = pri;
            displayLabel = pLabel;
            extendedInfo = pInfo;
            origIntent = pOrigIntent;
        }
    }

    public final class ResolveListHelper {
        private final Intent[] mInitialIntents;
        private final List<ResolveInfo> mBaseResolveList;
        private final Intent mIntent;
        private final int mLaunchedFromUid;

        private List<DisplayResolveInfo> mList;

        public ResolveListHelper(Intent intent, Intent[] initialIntents, List<ResolveInfo> rList,
                int launchedFromUid) {
            mIntent = new Intent(intent);
            mIntent.setComponent(null);
            mInitialIntents = initialIntents;
            mBaseResolveList = rList;
            mLaunchedFromUid = launchedFromUid;
            mList = new ArrayList<DisplayResolveInfo>();
            rebuildList();
        }

        public void handlePackagesChanged() {
            rebuildList();
            if (getCount() == 0) {
                // We no longer have any items... just finish the activity.
                finish();
            }
        }

        private void rebuildList() {
            List<ResolveInfo> currentResolveList;

            mList.clear();
            if (mBaseResolveList != null) {
                currentResolveList = mBaseResolveList;
            } else {
                currentResolveList = mPm.queryIntentActivities(
                        mIntent, PackageManager.MATCH_DEFAULT_ONLY
                        | (mAlwaysUseOption ? PackageManager.GET_RESOLVED_FILTER : 0));
                // Filter out any activities that the launched uid does not
                // have permission for. We don't do this when we have an explicit
                // list of resolved activities, because that only happens when
                // we are being subclassed, so we can safely launch whatever
                // they gave us.
                if (currentResolveList != null) {
                    for (int i = currentResolveList.size() - 1; i >= 0; i--) {
                        ActivityInfo ai = currentResolveList.get(i).activityInfo;
                        int granted = ActivityManager.checkComponentPermission(
                                ai.permission, mLaunchedFromUid,
                                ai.applicationInfo.uid, ai.exported);
                        if (granted != PackageManager.PERMISSION_GRANTED) {
                            // Access not allowed!
                            currentResolveList.remove(i);
                        }
                    }
                }
            }
            int N;
            if ((currentResolveList != null) && ((N = currentResolveList.size()) > 0)) {
                // Only display the first matches that are either of equal
                // priority or have asked to be default options.
                ResolveInfo r0 = currentResolveList.get(0);
                for (int i = 1; i < N; i++) {
                    ResolveInfo ri = currentResolveList.get(i);
                    if (DEBUG)
                        Log.v("ResolveListActivity",
                                r0.activityInfo.name + "=" +
                                        r0.priority + "/" + r0.isDefault + " vs " +
                                        ri.activityInfo.name + "=" +
                                        ri.priority + "/" + ri.isDefault);
                    if (r0.priority != ri.priority ||
                            r0.isDefault != ri.isDefault) {
                        while (i < N) {
                            currentResolveList.remove(i);
                            N--;
                        }
                    }
                }
                if (N > 1) {
                    ResolveInfo.DisplayNameComparator rComparator =
                            new ResolveInfo.DisplayNameComparator(mPm);
                    Collections.sort(currentResolveList, rComparator);
                }
                // First put the initial items at the top.
                if (mInitialIntents != null) {
                    for (Intent ii : mInitialIntents) {
                        if (ii == null) {
                            continue;
                        }
                        ActivityInfo ai = ii.resolveActivityInfo(
                                getPackageManager(), 0);
                        if (ai == null) {
                            Log.w("ResolverActivity", "No activity found for "
                                    + ii);
                            continue;
                        }
                        ResolveInfo ri = new ResolveInfo();
                        ri.activityInfo = ai;
                        if (ii instanceof LabeledIntent) {
                            LabeledIntent li = (LabeledIntent) ii;
                            ri.resolvePackageName = li.getSourcePackage();
                            ri.labelRes = li.getLabelResource();
                            ri.nonLocalizedLabel = li.getNonLocalizedLabel();
                            ri.icon = li.getIconResource();
                        }
                        mList.add(new DisplayResolveInfo(ri,
                                ri.loadLabel(getPackageManager()), null, ii));
                    }
                }

                // Check for applications with same name and use application
                // name or package name if necessary
                r0 = currentResolveList.get(0);
                int start = 0;
                CharSequence r0Label = r0.loadLabel(mPm);
                for (int i = 1; i < N; i++) {
                    if (r0Label == null) {
                        r0Label = r0.activityInfo.packageName;
                    }
                    ResolveInfo ri = currentResolveList.get(i);
                    CharSequence riLabel = ri.loadLabel(mPm);
                    if (riLabel == null) {
                        riLabel = ri.activityInfo.packageName;
                    }
                    if (riLabel.equals(r0Label)) {
                        continue;
                    }
                    processGroup(currentResolveList, start, (i - 1), r0, r0Label);
                    r0 = ri;
                    r0Label = riLabel;
                    start = i;
                }
                // Process last group
                processGroup(currentResolveList, start, (N - 1), r0, r0Label);
            }
        }

        private void processGroup(List<ResolveInfo> rList, int start, int end, ResolveInfo ro,
                CharSequence roLabel) {
            // Process labels from start to i
            int num = end - start + 1;
            if (num == 1) {
                // No duplicate labels. Use label for entry at start
                mList.add(new DisplayResolveInfo(ro, roLabel, null, null));
            } else {
                boolean usePkg = false;
                CharSequence startApp = ro.activityInfo.applicationInfo.loadLabel(mPm);
                if (startApp == null) {
                    usePkg = true;
                }
                if (!usePkg) {
                    // Use HashSet to track duplicates
                    HashSet<CharSequence> duplicates =
                            new HashSet<CharSequence>();
                    duplicates.add(startApp);
                    for (int j = start + 1; j <= end; j++) {
                        ResolveInfo jRi = rList.get(j);
                        CharSequence jApp = jRi.activityInfo.applicationInfo.loadLabel(mPm);
                        if ((jApp == null) || (duplicates.contains(jApp))) {
                            usePkg = true;
                            break;
                        } else {
                            duplicates.add(jApp);
                        }
                    }
                    // Clear HashSet for later use
                    duplicates.clear();
                }
                for (int k = start; k <= end; k++) {
                    ResolveInfo add = rList.get(k);
                    if (usePkg) {
                        // Use package name for all entries from start to end-1
                        mList.add(new DisplayResolveInfo(add, roLabel,
                                add.activityInfo.packageName, null));
                    } else {
                        // Use application name for all entries from start to end-1
                        mList.add(new DisplayResolveInfo(add, roLabel,
                                add.activityInfo.applicationInfo.loadLabel(mPm), null));
                    }
                }
            }
        }

        public ResolveInfo resolveInfoForPosition(int position) {
            return mList.get(position).ri;
        }

        public Intent intentForPosition(int position) {
            DisplayResolveInfo dri = mList.get(position);

            Intent intent = new Intent(dri.origIntent != null
                    ? dri.origIntent : mIntent);
            intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT
                    | Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
            ActivityInfo ai = dri.ri.activityInfo;
            intent.setComponent(new ComponentName(
                    ai.applicationInfo.packageName, ai.name));
            return intent;
        }

        public int getCount() {
            return mList.size();
        }
    }
}
