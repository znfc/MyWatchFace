package com.google.android.clockwork.settings.personal.fitness.models;

import static com.google.android.clockwork.settings.personal.fitness.models.PackageWhiteListModel.getPackage;

import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.support.annotation.Nullable;
import com.google.android.clockwork.settings.personal.fitness.ExerciseDetectionRelayActivity;
import java.util.Arrays;
import java.util.List;

/**
 * Determines which app is in the foreground and enables the ability to discern whether a fitness
 * app other than the one we want to start is already in the foreground.
 * <p>
 * See {@link #isOtherFitnessAppRunning}.
 */
public class RunningAppsModel {

    private final ActivityManager mActivityManager;

    private String cachedForegroundPackage = null;

    public RunningAppsModel(ActivityManager activityManager) {
        mActivityManager = activityManager;
    }

    /**
     * Returns {@code true} if a fitness app other than the {@code targetComponent} is already in
     * the foreground. Note that this check will be purely based on package names.
     * <p>
     * Examples:
     * <ul>
     *     <li> Will return true if: the target component is Fit's tracking Activity and the user
     *          is in another app whose package matches one in {@code allFitnessComponents}.
     *     <li> Will return false if: the target component is Fit's tracking Activity and the user
     *          is in the Fit tracking Activity.
     *     <li> Will return false if: the target component is Fit's tracking Activity and the user
     *          is in some other Fit Activity.
     *     <li> Will return false if: the target component is Fit's tracking Activity and the user
     *          is looking at the watch face.
     * </ul>
     * <p>
     * <b>Note:</b> This assumes that e.g. {@link ExerciseDetectionRelayActivity} will always be the
     * actual foreground app. So, it will examine the app that was open before this one. If this is
     * ever called from a Service, it will need to be changed.
     */
    public boolean isOtherFitnessAppRunning(String targetComponent,
            CharSequence[] allFitnessComponents) {
        String targetPackage = getPackage(targetComponent);
        return Arrays.stream(allFitnessComponents)
                .map(component -> getPackage((String) component))
                .filter(packageName -> !targetPackage.equals(packageName))
                .anyMatch(this::matchesForegroundAppPackage);
    }

    boolean matchesForegroundAppPackage(String component) {
        String targetPackage = getPackage(component);
        if (targetPackage == null || "".equals(targetPackage)) {
            return false;
        }

        String foregroundPackage = getForegroundPackage();

        return targetPackage.equals(foregroundPackage);
    }

    @Nullable
    private String getForegroundPackage() {
        if (cachedForegroundPackage != null) {
            return cachedForegroundPackage;
        }

        // ExerciseDetectionRelayActivity will always be the first task. So, we will consider the
        // second task to be the actual foreground task.
        List<RecentTaskInfo> recentTasks =
                mActivityManager.getRecentTasks(2 /* maxNum */, 0 /* flags */);

        if (recentTasks.size() < 2) {
            return null;
        }

        RecentTaskInfo recentTaskInfo = recentTasks.get(1);
        if (recentTaskInfo == null || recentTaskInfo.baseActivity == null) {
            return null;
        }

        cachedForegroundPackage = recentTaskInfo.baseActivity.getPackageName();
        return cachedForegroundPackage;
    }
}
