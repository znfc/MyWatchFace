package com.google.android.clockwork.settings;

import com.android.internal.util.IndentingPrintWriter;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;

import com.google.android.clockwork.settings.time.TimeSyncManager;
import com.google.android.clockwork.settings.time.TimeZoneMediator;

import java.io.FileDescriptor;
import java.io.PrintWriter;

/**
 * A debug service for time and timezone syncing
 */
public class TimeDebugService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        if (this.checkCallingOrSelfPermission(android.Manifest.permission.DUMP)
                != PackageManager.PERMISSION_GRANTED) {
            writer.println("Permission Denial: can't dump TimeService from from pid="
                    + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        try {
            final IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
            TimeSyncManager.INSTANCE.get(this).dump(pw);
            TimeZoneMediator.INSTANCE.get(this).dump(pw);
        } catch (Exception e) {
            writer.println("Exception dumping TimeDebugService");
            writer.println(e);
        }
    }
}