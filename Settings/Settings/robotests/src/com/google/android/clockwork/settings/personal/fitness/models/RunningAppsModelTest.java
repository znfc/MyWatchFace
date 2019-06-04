package com.google.android.clockwork.settings.personal.fitness.models;

import static android.content.ComponentName.unflattenFromString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import com.google.common.collect.ImmutableList;
import java.util.List;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

/** Tests for {@link RunningAppsModel}. */
@RunWith(ClockworkRobolectricTestRunner.class)
public class RunningAppsModelTest extends TestCase {

    private static final String TARGET_PACKAGE = "com.google.running.app";
    private static final String OTHER_PACKAGE = "com.google.notrunning.app";
    private static final String HOME_PACKAGE = "com.google.android.wearable.app";

    private static final String TARGET_COMPONENT = TARGET_PACKAGE + "/.RunningActivity";
    private static final String OTHER_COMPONENT = OTHER_PACKAGE + "/.NotRunningActivity";
    private static final String HOME_COMPONENT = HOME_PACKAGE
            + "/com.google.android.clockwork.home2.activity.HomeActivity2";

    private RecentTaskInfo mTargetTaskInfo = mock(RecentTaskInfo.class);
    private RecentTaskInfo mOtherTaskInfo = mock(RecentTaskInfo.class);
    private RecentTaskInfo mHomeTaskInfo = mock(RecentTaskInfo.class);

    private List<RecentTaskInfo> mTasksWithTarget =
            ImmutableList.of(mHomeTaskInfo, mTargetTaskInfo);
    private List<RecentTaskInfo> mTasksWithOther =
            ImmutableList.of(mHomeTaskInfo, mOtherTaskInfo);

    private String[] mTargetAndOther = new String[] {
            OTHER_COMPONENT,
            TARGET_COMPONENT
    };

    @Mock ActivityManager mActivityManager;
    private RunningAppsModel mRunningAppsModel;

    @Before
    public void setUp() {
        initMocks(this);
        mTargetTaskInfo.baseActivity = unflattenFromString(TARGET_COMPONENT);
        mOtherTaskInfo.baseActivity = unflattenFromString(OTHER_COMPONENT);
        mHomeTaskInfo.baseActivity = unflattenFromString(HOME_COMPONENT);
    }

    @Test
    public void isForegroundShouldReturnTrueWhenAppIsRunning() {
        when(mActivityManager.getRecentTasks(2, 0)).thenReturn(mTasksWithTarget);

        mRunningAppsModel = new RunningAppsModel(mActivityManager);

        assertTrue(mRunningAppsModel.matchesForegroundAppPackage(TARGET_COMPONENT));
        assertTrue(mRunningAppsModel.matchesForegroundAppPackage(TARGET_PACKAGE));
        assertTrue(mRunningAppsModel.matchesForegroundAppPackage(TARGET_PACKAGE + "/.Other"));
    }

    @Test
    public void isForegroundShouldReturnFalseWhenAppIsNotRunning() {
        when(mActivityManager.getRecentTasks(2, 0)).thenReturn(mTasksWithOther);

        mRunningAppsModel = new RunningAppsModel(mActivityManager);

        assertFalse(mRunningAppsModel.matchesForegroundAppPackage(TARGET_COMPONENT));
        assertFalse(mRunningAppsModel.matchesForegroundAppPackage(TARGET_PACKAGE));
        assertFalse(mRunningAppsModel.matchesForegroundAppPackage(TARGET_PACKAGE + "/.Other"));
    }

    @Test
    public void shouldReturnTrueWhenTargetIsNotRunningButOtherIs() {
        when(mActivityManager.getRecentTasks(2, 0)).thenReturn(mTasksWithOther);

        mRunningAppsModel = new RunningAppsModel(mActivityManager);

        assertTrue(mRunningAppsModel.isOtherFitnessAppRunning(
                TARGET_COMPONENT,
                new String[] {
                        OTHER_COMPONENT,
                        TARGET_COMPONENT
                }));

        assertTrue(mRunningAppsModel.isOtherFitnessAppRunning(
                TARGET_PACKAGE + "/.AnotherActivityInTargetPackage",
                new String[] {
                        OTHER_COMPONENT,
                        TARGET_COMPONENT
                }));
    }

    @Test
    public void shouldReturnFalseWhenTargetIsRunningButOtherIsNot() {
        when(mActivityManager.getRecentTasks(2, 0)).thenReturn(mTasksWithTarget);

        mRunningAppsModel = new RunningAppsModel(mActivityManager);

        assertFalse(mRunningAppsModel.isOtherFitnessAppRunning(
                TARGET_COMPONENT,
                new String[] {
                        OTHER_COMPONENT,
                        TARGET_COMPONENT
                }));
    }
}