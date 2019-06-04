package com.google.android.clockwork.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.admin.DevicePolicyManager;
import android.app.AlertDialog;
import android.content.Intent;
import android.provider.Settings;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowAlertDialog;

/** Tests for {@link RemoteBugreportActivity}. */
@RunWith(ClockworkRobolectricTestRunner.class)
public class RemoteBugreportActivityTest {

    private Intent getIntentWithExtra(int extra) {
        return new Intent(Settings.ACTION_SHOW_REMOTE_BUGREPORT_DIALOG)
                .putExtra(DevicePolicyManager.EXTRA_BUGREPORT_NOTIFICATION_TYPE, extra);
    }

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testFullLifeCycle() throws Exception {
        Robolectric.buildActivity(
            RemoteBugreportActivity.class, new Intent(Settings.ACTION_SHOW_REMOTE_BUGREPORT_DIALOG))
                .create().start().resume().visible()
                .get();
    }

    @Test
    public void handleBugreportFinishedShare() {
        TestableRemoteBugreportActivity activity =
                Robolectric.buildActivity(
                    TestableRemoteBugreportActivity.class, getIntentWithExtra(
                                DevicePolicyManager.NOTIFICATION_BUGREPORT_FINISHED_NOT_ACCEPTED))
                        .create().start().resume().visible()
                        .get();
        AlertDialog alertDialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(alertDialog);
        assertTrue(alertDialog.isShowing());

        assertFalse(activity.isFinishing());
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        assertTrue(activity.isFinishing());

        assertEquals(1, activity.getBroadcastIntents().size());
        assertEquals(DevicePolicyManager.ACTION_BUGREPORT_SHARING_ACCEPTED,
                activity.getBroadcastIntents().get(0).getAction());
    }

    @Test
    public void handleBugreportFinishedDeny() {
        TestableRemoteBugreportActivity activity =
                Robolectric.buildActivity(
                    TestableRemoteBugreportActivity.class, getIntentWithExtra(
                                DevicePolicyManager.NOTIFICATION_BUGREPORT_FINISHED_NOT_ACCEPTED))
                        .create().start().resume().visible()
                        .get();
        AlertDialog alertDialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(alertDialog);
        assertTrue(alertDialog.isShowing());

        assertFalse(activity.isFinishing());
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        assertTrue(activity.isFinishing());

        assertEquals(1, activity.getBroadcastIntents().size());
        assertEquals(DevicePolicyManager.ACTION_BUGREPORT_SHARING_DECLINED,
                activity.getBroadcastIntents().get(0).getAction());
    }

    @Test
    public void handleBugreportFinishedDismiss() {
        TestableRemoteBugreportActivity activity =
                Robolectric.buildActivity(
                    TestableRemoteBugreportActivity.class, getIntentWithExtra(
                                DevicePolicyManager.NOTIFICATION_BUGREPORT_FINISHED_NOT_ACCEPTED))
                        .create().start().resume().visible()
                        .get();
        AlertDialog alertDialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(alertDialog);
        assertTrue(alertDialog.isShowing());

        assertFalse(activity.isFinishing());
        alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).performClick();
        assertTrue(activity.isFinishing());
        assertEquals(0, activity.getBroadcastIntents().size());
    }

    @Test
    public void handleBugreportNotFinishedDismiss() {
        TestableRemoteBugreportActivity activity =
                Robolectric.buildActivity(
                    TestableRemoteBugreportActivity.class, getIntentWithExtra(
                                DevicePolicyManager.NOTIFICATION_BUGREPORT_ACCEPTED_NOT_FINISHED))
                        .create().start().resume().visible()
                        .get();
        AlertDialog alertDialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(alertDialog);
        assertTrue(alertDialog.isShowing());

        assertFalse(activity.isFinishing());
        alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).performClick();
        assertTrue(activity.isFinishing());
        assertEquals(0, activity.getBroadcastIntents().size());
    }

    @Test
    public void handleBugreportNotFinishedOK() {
        TestableRemoteBugreportActivity activity =
                Robolectric.buildActivity(
                    TestableRemoteBugreportActivity.class, getIntentWithExtra(
                                DevicePolicyManager.NOTIFICATION_BUGREPORT_ACCEPTED_NOT_FINISHED))
                        .create().start().resume().visible()
                        .get();
        AlertDialog alertDialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(alertDialog);
        assertTrue(alertDialog.isShowing());

        assertFalse(activity.isFinishing());
        alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).performClick();
        assertTrue(activity.isFinishing());
        assertEquals(0, activity.getBroadcastIntents().size());
    }
}

class TestableRemoteBugreportActivity extends RemoteBugreportActivity {
    public List<Intent> mIntents = new ArrayList<Intent>();

    @Override
    protected void sendBroadcastAsSystem(Intent intent) {
        mIntents.add(intent);
    }

    public List<Intent> getBroadcastIntents() {
        return mIntents;
    }
}
