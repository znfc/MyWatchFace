package com.google.android.clockwork.settings;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.google.android.clockwork.power.PowerSettingsManager;

/**
 * This service exposes an interface for enabling or disabling the extra cores that are disabled by
 * default on supported devices.
 */
public class CpuCoreService extends Service {
  private static final String TAG = "CpuCoreService";

  private static final int MSG_ENABLE_EXTRA_CORES = 1;
  private static final int MSG_DISABLE_EXTRA_CORES = 2;

  private Messenger mMessenger;
  private PowerSettingsManager mPowerSettingsManager;

  private final class IncomingHandler extends Handler {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case MSG_ENABLE_EXTRA_CORES:
          enableExtraCores();
          break;
        case MSG_DISABLE_EXTRA_CORES:
          disableExtraCores();
          break;
        default:
          super.handleMessage(msg);
      }
    }
  }

  @Override
  public void onCreate() {
    super.onCreate();

    mMessenger = new Messenger(new IncomingHandler());
    mPowerSettingsManager = PowerSettingsManager.getOrCreate(this);
  }

  @Override
  public IBinder onBind(Intent intent) {
    if (Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(TAG, "onBind " + intent);
    }

    return mMessenger.getBinder();
  }

  private void enableExtraCores() {
    if (Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(TAG, "Enabling extra CPU cores");
    }
    mPowerSettingsManager.updateForegroundLoadHint(true);
  }

  private void disableExtraCores() {
    if (Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(TAG, "Disabling extra CPU cores");
    }
    mPowerSettingsManager.updateForegroundLoadHint(false);
  }

  @Override
  public void onDestroy() {
    if (Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(TAG, "onDestroy");
    }
    disableExtraCores();
    super.onDestroy();
  }
}
