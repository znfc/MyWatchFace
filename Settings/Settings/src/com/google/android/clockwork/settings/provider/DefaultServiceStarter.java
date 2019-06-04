package com.google.android.clockwork.settings.provider;

import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.util.function.Supplier;

public class DefaultServiceStarter implements ServiceStarter {
    private static final String TAG = "DefaultServiceStarter";
    private Supplier<Context> mContext;

    public DefaultServiceStarter(Supplier<Context> context) {
        mContext = context;
    }

    @Override
    public ComponentName startService(Intent service) throws SecurityException,
            IllegalStateException {
        return mContext.get().startService(service);
    }
}
