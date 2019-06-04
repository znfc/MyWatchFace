package com.google.android.clockwork.settings.provider;

import android.content.ComponentName;
import android.content.Intent;

/** Wrapper for starting a service. */
public interface ServiceStarter {
    /** {@see android.content.Context#startService} */
    ComponentName startService(Intent service) throws SecurityException, IllegalStateException;
}
