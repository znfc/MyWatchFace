package com.google.android.clockwork.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.admin.DeviceAdminInfo;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteCallback;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.wearable.view.CircularButton;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.apps.wearable.settings.R;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;

/**
 * DeviceAdminAdd allows a device policy application to be set as an active device admin. It also
 * allows a user to deactive and uninstall an active device admin.
 *
 * The Extras sent with the DeviceAdminAdd intent are as follows:
 *  DevicePolicyManager.EXTRA_DEVICE_ADMIN to specify the component to set as an active device admin
 *  OR
 *  EXTRA_DEVICE_ADMIN_PACKAGE_NAME to specify the package name of an active device admin to
 *  deactivate and uninstall.
 *
 * Based on [[android]] //packages/apps/Settings/src/com/android/settings/DeviceAdminAdd.java
 */
public class DeviceAdminAdd extends Activity {

    static final String TAG = "DeviceAdminAdd";

    static final int DIALOG_WARNING = 1;

    /**
     * Optional key to map to the package name of the Device Admin. Currently only used when
     * uninstalling an active device admin.
     */
    public static final String EXTRA_DEVICE_ADMIN_PACKAGE_NAME =
            "android.app.extra.DEVICE_ADMIN_PACKAGE_NAME";

    private final IBinder mToken = new Binder();
    Handler mHandler;

    DevicePolicyManager mDPM;
    AppOpsManager mAppOps;
    DeviceAdminInfo mDeviceAdmin;
    CharSequence mAddMsgText;

    ImageView mAdminIcon;
    TextView mAdminName;
    TextView mAdminDescription;
    TextView mAddMsg;
    TextView mProfileOwnerWarning;
    TextView mAdminWarning;
    TextView mSupportMessage;
    ViewGroup mAdminPolicies;
    Button mActionButton;
    Button mCancelButton;
    View mUninstallButton;

    boolean mUninstalling = false;
    boolean mAdding;
    boolean mRefreshing;
    boolean mWaitingForRemoveMsg;
    boolean mAdminPoliciesInitialized;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        mHandler = new Handler(getMainLooper());

        mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        mAppOps = (AppOpsManager)getSystemService(Context.APP_OPS_SERVICE);
        PackageManager packageManager = getPackageManager();

        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0) {
            Log.w(TAG, "Cannot start ADD_DEVICE_ADMIN as a new task");
            finish();
            return;
        }

        String action = getIntent().getAction();
        ComponentName who = (ComponentName) getIntent().getParcelableExtra(
                DevicePolicyManager.EXTRA_DEVICE_ADMIN);
        if (who == null) {
            String packageName = getIntent().getStringExtra(EXTRA_DEVICE_ADMIN_PACKAGE_NAME);
            for (ComponentName component : mDPM.getActiveAdmins()) {
                if (component.getPackageName().equals(packageName)) {
                    who = component;
                    mUninstalling = true;
                    break;
                }
            }
            if (who == null) {
                Log.w(TAG, "No component specified in " + action);
                finish();
                return;
            }
        }

        ActivityInfo ai;
        try {
            ai = packageManager.getReceiverInfo(who, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Unable to retrieve device policy " + who, e);
            finish();
            return;
        }

        // When activating, make sure the given component name is actually a valid device admin.
        // No need to check this when deactivating, because it is safe to deactivate an active
        // invalid device admin.
        if (!mDPM.isAdminActive(who)) {
            List<ResolveInfo> avail = packageManager.queryBroadcastReceivers(
                    new Intent(DeviceAdminReceiver.ACTION_DEVICE_ADMIN_ENABLED),
                    PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS);
            int count = avail == null ? 0 : avail.size();
            boolean found = false;
            for (int i = 0; i < count; i++) {
                ResolveInfo ri = avail.get(i);
                if (ai.packageName.equals(ri.activityInfo.packageName)
                        && ai.name.equals(ri.activityInfo.name)) {
                    try {
                        // We didn't retrieve the meta data for all possible matches, so
                        // need to use the activity info of this specific one that was retrieved.
                        ri.activityInfo = ai;
                        DeviceAdminInfo dpi = new DeviceAdminInfo(this, ri);
                        found = true;
                    } catch (XmlPullParserException e) {
                        Log.w(TAG, "Bad " + ri.activityInfo, e);
                    } catch (IOException e) {
                        Log.w(TAG, "Bad " + ri.activityInfo, e);
                    }
                    break;
                }
            }
            if (!found) {
                Log.w(TAG, "Request to add invalid device admin: " + who);
                finish();
                return;
            }
        }

        ResolveInfo ri = new ResolveInfo();
        ri.activityInfo = ai;
        try {
            mDeviceAdmin = new DeviceAdminInfo(this, ri);
        } catch (XmlPullParserException e) {
            Log.w(TAG, "Unable to retrieve device policy " + who, e);
            finish();
            return;
        } catch (IOException e) {
            Log.w(TAG, "Unable to retrieve device policy " + who, e);
            finish();
            return;
        }

        // This admin already exists, and we have two options at this point.  If new policy
        // bits are set, show the user the new list.  If nothing has changed, simply return
        // "OK" immediately.
        if (DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN.equals(getIntent().getAction())) {
            mRefreshing = false;
            if (mDPM.isAdminActive(who)) {
                if (mDPM.isRemovingAdmin(who, android.os.Process.myUserHandle().getIdentifier())) {
                    Log.w(TAG, "Requested admin is already being removed: " + who);
                    finish();
                    return;
                }

                ArrayList<DeviceAdminInfo.PolicyInfo> newPolicies = mDeviceAdmin.getUsedPolicies();
                for (int i = 0; i < newPolicies.size(); i++) {
                    DeviceAdminInfo.PolicyInfo pi = newPolicies.get(i);
                    if (!mDPM.hasGrantedPolicy(who, pi.ident)) {
                        mRefreshing = true;
                        break;
                    }
                }
                if (!mRefreshing) {
                    // Nothing changed (or policies were removed) - return immediately
                    setResult(Activity.RESULT_OK);
                    finish();
                    return;
                }
            }
        }

        mAddMsgText = getIntent().getCharSequenceExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION);

        setContentView(R.layout.device_admin_add);

        mAdminIcon = (ImageView) findViewById(R.id.admin_icon);
        mAdminName = (TextView) findViewById(R.id.admin_name);
        mAdminDescription = (TextView) findViewById(R.id.admin_description);

        mAddMsg = (TextView) findViewById(R.id.add_msg);

        mAdminWarning = (TextView) findViewById(R.id.admin_warning);
        mAdminPolicies = (ViewGroup) findViewById(R.id.admin_policies);
        mSupportMessage = (TextView) findViewById(R.id.admin_support_message);

        mUninstallButton = (View) findViewById(R.id.uninstall_button);
        mUninstallButton.setFilterTouchesWhenObscured(true);
        mUninstallButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mDPM.uninstallPackageWithActiveAdmins(mDeviceAdmin.getPackageName());
                finish();
            }
        });

        mActionButton = (Button) findViewById(R.id.action_button);
        mActionButton.setFilterTouchesWhenObscured(true);
        View.OnClickListener actionListener = new View.OnClickListener() {
            public void onClick(View v) {
                if (!v.isEnabled()) {
                    return;
                }
                if (mAdding) {
                    addAndFinish();
                } else if (mUninstalling) {
                    try {
                        mDPM.uninstallPackageWithActiveAdmins(mDeviceAdmin.getPackageName());
                    } catch (IllegalArgumentException e) {
                        Log.w(TAG, "Not allowed to uninstall " + mDeviceAdmin.getComponent(), e);
                    }
                    finish();
                } else if (!mWaitingForRemoveMsg) {
                    mWaitingForRemoveMsg = true;
                    // Get the warning message from the device admin if there is one
                    // and display it to the user. If a message is not provided in a timely manner,
                    // remove the admin anyways.
                    mDPM.getRemoveWarning(mDeviceAdmin.getComponent(),
                            new RemoteCallback(new RemoteCallback.OnResultListener() {
                                @Override
                                public void onResult(Bundle result) {
                                    CharSequence msg = result != null
                                            ? result.getCharSequence(
                                            DeviceAdminReceiver.EXTRA_DISABLE_WARNING)
                                            : null;
                                    continueRemoveAction(msg);
                                }
                            }, mHandler));
                    // Give the currently active admin a small amount of time to provide a message
                    // to the user but don't want to wait too long.
                    getWindow().getDecorView().getHandler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            continueRemoveAction(null);
                        }
                    }, 2 * 1000);
                }
            }
        };
        mActionButton.setOnClickListener(actionListener);

        mCancelButton = (Button) findViewById(R.id.cancel_button);
        mCancelButton.setFilterTouchesWhenObscured(true);
        mCancelButton.setOnClickListener(actionListener);
    }

    void addAndFinish() {
        try {
            mDPM.setActiveAdmin(mDeviceAdmin.getComponent(), mRefreshing);
            setResult(Activity.RESULT_OK);
        } catch (RuntimeException e) {
            // Something bad happened...  could be that it was
            // already set, though.
            Log.w(TAG, "Exception trying to activate admin "
                    + mDeviceAdmin.getComponent(), e);
            if (mDPM.isAdminActive(mDeviceAdmin.getComponent())) {
                setResult(Activity.RESULT_OK);
            }
        }
        finish();
    }

    void continueRemoveAction(CharSequence msg) {
        if (!mWaitingForRemoveMsg) {
            return;
        }
        mWaitingForRemoveMsg = false;
        // If the admin being removed has not provided a warning in a timely manner, go ahead and
        // remove the admin.
        if (msg == null) {
            mDPM.removeActiveAdmin(mDeviceAdmin.getComponent());
            finish();
        } else {
            // If the admin does provide a warning, show the warning and allow user to cancel if
            // the user wants to.
            Bundle args = new Bundle();
            args.putCharSequence(
                    DeviceAdminReceiver.EXTRA_DISABLE_WARNING, msg);
            showDialog(DIALOG_WARNING, args);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mActionButton.setEnabled(true);
        mCancelButton.setEnabled(true);
        updateInterface();
        // As long as we are running, don't let anyone overlay stuff on top of the screen.
        mAppOps.setUserRestriction(AppOpsManager.OP_SYSTEM_ALERT_WINDOW, true, mToken);
        mAppOps.setUserRestriction(AppOpsManager.OP_TOAST_WINDOW, true, mToken);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mActionButton.setEnabled(false);
        mCancelButton.setEnabled(false);
        mAppOps.setUserRestriction(AppOpsManager.OP_SYSTEM_ALERT_WINDOW, false, mToken);
        mAppOps.setUserRestriction(AppOpsManager.OP_TOAST_WINDOW, false, mToken);
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case DIALOG_WARNING: {
                CharSequence msg = args.getCharSequence(DeviceAdminReceiver.EXTRA_DISABLE_WARNING);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(msg);
                builder.setPositiveButton(R.string.generic_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                mDPM.removeActiveAdmin(mDeviceAdmin.getComponent());
                                finish();
                            }
                        });
                builder.setNegativeButton(R.string.generic_cancel, null);
                return builder.create();
            }
            default:
                return super.onCreateDialog(id, args);

        }
    }

    void updateInterface() {
        mAdminIcon.setImageDrawable(mDeviceAdmin.loadIcon(getPackageManager()));
        mAdminName.setText(mDeviceAdmin.loadLabel(getPackageManager()));
        try {
            mAdminDescription.setText(
                    mDeviceAdmin.loadDescription(getPackageManager()));
            mAdminDescription.setVisibility(View.VISIBLE);
        } catch (Resources.NotFoundException e) {
            mAdminDescription.setVisibility(View.GONE);
        }
        if (mAddMsgText != null) {
            mAddMsg.setText(mAddMsgText);
            mAddMsg.setVisibility(View.VISIBLE);
        } else {
            mAddMsg.setVisibility(View.GONE);
        }
        if (!mRefreshing && mDPM.isAdminActive(mDeviceAdmin.getComponent())) {
            mAdding = false;
            addDeviceAdminPolicies(false /* showDescription */);
            mAdminWarning.setText(getString(R.string.active_device_admin,
                    mDeviceAdmin.getActivityInfo().applicationInfo.loadLabel(
                            getPackageManager())));
            setTitle(R.string.active_device_admin_msg);
            if (mUninstalling) {
                mCancelButton.setText(R.string.remove_and_uninstall_device_admin);
            } else {
                mCancelButton.setText(R.string.deactivate_admin);
            }
            mCancelButton.setVisibility(View.VISIBLE);
            mActionButton.setVisibility(View.GONE);

            CharSequence supportMessage = mDPM.getLongSupportMessageForUser(
                    mDeviceAdmin.getComponent(), UserHandle.myUserId());
            if (!TextUtils.isEmpty(supportMessage)) {
                mSupportMessage.setText(supportMessage);
                mSupportMessage.setVisibility(View.VISIBLE);
            } else {
                mSupportMessage.setVisibility(View.GONE);
            }
        } else {
            addDeviceAdminPolicies(true /* showDescription */);
            mAdminWarning.setText(getString(R.string.device_admin_warning,
                    mDeviceAdmin.getActivityInfo().applicationInfo.loadLabel(getPackageManager())));
            setTitle(getText(R.string.add_device_admin_msg));
            mActionButton.setText(getText(R.string.add_device_admin));
            if (isAdminUninstallable()) {
                mUninstallButton.setVisibility(View.VISIBLE);
            }
            mSupportMessage.setVisibility(View.GONE);
            mAdding = true;
        }
    }

    private void addDeviceAdminPolicies(boolean showDescription) {
        if (!mAdminPoliciesInitialized) {
            boolean isAdminUser = UserManager.get(this).isAdminUser();
            for (DeviceAdminInfo.PolicyInfo pi : mDeviceAdmin.getUsedPolicies()) {
                int descriptionId = isAdminUser ? pi.description : pi.descriptionForSecondaryUsers;
                int labelId = isAdminUser ? pi.label : pi.labelForSecondaryUsers;
                LayoutInflater inflater =
                    (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View view = inflater.inflate(R.layout.app_permission_item, null);
                TextView permGrpView = (TextView) view.findViewById(R.id.permission_group);
                TextView permDescView = (TextView) view.findViewById(R.id.permission_list);
                permGrpView.setText(getText(labelId));
                permDescView.setText(showDescription ? getText(descriptionId) : "");
                mAdminPolicies.addView(view);
            }
            mAdminPoliciesInitialized = true;
        }
    }

    private boolean isAdminUninstallable() {
        // System apps can't be uninstalled.
        return !mDeviceAdmin.getActivityInfo().applicationInfo.isSystemApp();
    }
}
