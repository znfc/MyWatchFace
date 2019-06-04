package com.google.android.clockwork.settings;

import android.app.ActivityThread;
import android.app.Application;
import android.content.Context;
import android.os.Trace;
import android.util.Log;

import com.google.android.clockwork.common.concurrent.CwStrictMode;
import com.google.android.clockwork.common.concurrent.Executors;
import com.google.android.clockwork.common.logging.ClearcutCwEventLogger;
import com.google.android.clockwork.common.logging.CwEventLogger;
import com.google.android.gsf.GservicesValue;
import com.google.common.logging.Cw.CwEvent.CwComponent;
import com.google.common.logging.Cw.CwEvent.CwNodeType;

import java.util.Objects;

public class SettingsApplication extends Application {

    static {
        Executors.initializeWatchExecutors();
        Executors.injectAsyncTaskExecutor();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        // Load gkeys because inevitably, something needs them during app start up.
        Trace.beginSection("attachBaseContext-gservices");
        GservicesValue.init(base);
        Trace.endSection();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Ensure we're not doing the wrong thing.
        CwStrictMode.init();

        // Even though we're not supposed to add things here, we want to ensure that during 
        // Application.onCreate() strict mode is still enforced, since in some cases, it won't be.
        // https://code.google.com/p/android/issues/detail?id=35298
        CwStrictMode.ensureStrictModeEnabled();

        // Clearcut logger can only be instantiated inside the default Settings process.
        // See b/77342602
        if (Objects.equals(ActivityThread.currentProcessName(), getApplicationInfo().packageName)) {
            ClearcutCwEventLogger simpleLogger = ClearcutCwEventLogger.create(
                    getApplicationContext(),
                    CwComponent.CW_COMPONENT_HOME,
                    CwNodeType.CW_NODE_WATCH_UNKNOWN,
                    true /*enabled*/,
                    true /*counterFlushEnabled*/);
            CwEventLogger.setInstance(simpleLogger);
        }
    }
}
