package com.google.android.clockwork.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppGlobals;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.support.wearable.view.WearableDialogHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.google.android.apps.wearable.settings.R;

/**
 * ShowAdminSupportDetailsDialog shows an "Action Not Allowed" dialog for settings that
 * have been disabled by a Device Admin.
 *
 * The Extras sent with the ACTION_SHOW_ADMIN_SUPPORT_DETAILS intent are as follows:
 *  DevicePolicyManager.EXTRA_DEVICE_ADMIN to specify the ComponentName of the admin which
 *      specified the restriction.
 *  Intent.EXTRA_USER_ID to specify the userId of the admin which specified the restriction.
 *
 * Based on [[android]] packages/apps/Settings/src/com/android/settings
 *     /ShowAdminSupportDetailsDialog.java
 */
public class ShowAdminSupportDetailsDialog extends Activity
        implements DialogInterface.OnDismissListener {

    private static final String TAG = "AdminSupportDialog";

    private EnforcedAdmin mEnforcedAdmin;
    private WearableDialogHelper.DialogBuilder mDialogBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mEnforcedAdmin = getAdminDetailsFromIntent(getIntent());

        mDialogBuilder = new WearableDialogHelper.DialogBuilder(this);
        mDialogBuilder.setOnDismissListener(this);
        initializeDialogViews(mDialogBuilder, mEnforcedAdmin.component, mEnforcedAdmin.userId);
        mDialogBuilder.show();
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        EnforcedAdmin admin = getAdminDetailsFromIntent(intent);
        if (!mEnforcedAdmin.equals(admin)) {
            mEnforcedAdmin = admin;
            initializeDialogViews(mDialogBuilder, mEnforcedAdmin.component, mEnforcedAdmin.userId);
            mDialogBuilder.show();
        }
    }

    private static EnforcedAdmin getAdminDetailsFromIntent(Intent intent) {
        EnforcedAdmin admin = new EnforcedAdmin(null, UserHandle.myUserId());
        if (intent == null) {
            return admin;
        }
        admin.component = intent.getParcelableExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN);
        admin.userId = intent.getIntExtra(Intent.EXTRA_USER_ID, UserHandle.myUserId());
        return admin;
    }

    private void initializeDialogViews(WearableDialogHelper.DialogBuilder dialogBuilder,
            ComponentName admin, int userId) {
        if (admin != null) {
            if (!RestrictedLockUtils.isAdminInCurrentUserOrProfile(this, admin)
                    || !RestrictedLockUtils.isCurrentUserOrProfile(this, userId)) {
                admin = null;
            } else {
                ActivityInfo ai = null;
                try {
                    ai = AppGlobals.getPackageManager().getReceiverInfo(admin, 0 /* flags */,
                            userId);
                } catch (RemoteException e) {
                    Log.w(TAG, "Missing reciever info", e);
                }
                if (ai != null) {
                    Drawable icon = ai.loadIcon(getPackageManager());
                    Drawable badgedIcon = getPackageManager().getUserBadgedIcon(
                            icon, new UserHandle(userId));
                    dialogBuilder.setIcon(badgedIcon);
                }
            }
        }

        setAdminSupportDetails(this, dialogBuilder, new EnforcedAdmin(admin, userId), true);
    }

    private static void setAdminSupportDetails(final Activity activity,
            WearableDialogHelper.DialogBuilder dialogBuilder,
            final EnforcedAdmin enforcedAdmin, final boolean finishActivity) {
        if (enforcedAdmin == null) {
            return;
        }

        dialogBuilder.setTitle(R.string.disabled_by_policy_title);
        dialogBuilder.setMessage(R.string.default_admin_support_msg);
        if (enforcedAdmin.component != null) {
            DevicePolicyManager dpm = (DevicePolicyManager) activity.getSystemService(
                    Context.DEVICE_POLICY_SERVICE);
            if (!RestrictedLockUtils.isAdminInCurrentUserOrProfile(activity,
                    enforcedAdmin.component) || !RestrictedLockUtils.isCurrentUserOrProfile(
                    activity, enforcedAdmin.userId)) {
                enforcedAdmin.component = null;
            } else {
                if (enforcedAdmin.userId == UserHandle.USER_NULL) {
                    enforcedAdmin.userId = UserHandle.myUserId();
                }
                CharSequence supportMessage = null;
                if (UserHandle.isSameApp(Process.myUid(), Process.SYSTEM_UID)) {
                    supportMessage = dpm.getShortSupportMessageForUser(
                            enforcedAdmin.component, enforcedAdmin.userId);
                }
                if (supportMessage != null) {
                  dialogBuilder.setMessage(supportMessage);
                }
            }
        }
        dialogBuilder.setNeutralButton(R.string.admin_support_more_info,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        if (enforcedAdmin.component != null) {
                            intent.setClass(activity, DeviceAdminAdd.class);
                            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                                    enforcedAdmin.component);
                            // DeviceAdminAdd class may need to run as managed profile.
                            activity.startActivityAsUser(intent,
                                    new UserHandle(enforcedAdmin.userId));
                        }
                        if (finishActivity) {
                            activity.finish();
                        }
                    }
                });
                dialogBuilder.setNeutralIcon(R.drawable.ic_cc_settings_about);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }
}
