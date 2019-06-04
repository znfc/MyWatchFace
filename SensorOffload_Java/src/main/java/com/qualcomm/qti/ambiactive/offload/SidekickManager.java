/**
 * Copyright (c) 2018 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.ambiactive.offload;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import com.google.android.clockwork.decomposablewatchface.WatchFaceDecomposition;
import com.google.android.clockwork.sidekick.SidekickServiceConstants;
import com.google.android.clockwork.sidekick.ISidekickService;

public class SidekickManager {
  private static final String TAG = "SidekickManager";

  private final ISidekickService mService;

  public SidekickManager() {
    mService =
        ISidekickService.Stub.asInterface(
            ServiceManager.getService(SidekickServiceConstants.NAME));
  }

  public boolean sidekickExists() {
    if (mService == null) {
      Log.w(TAG, "No Sidekick service.");
      return false;
    }

    try {
        return mService.sidekickExists();
    } catch (RemoteException e) {
      throw e.rethrowFromSystemServer();
    }
  }
    /**
     * Tells Sidekick to reset its internal state.
     * @return Status
     */
  public boolean clearWatchFace() {
    Log.i(TAG, "clearWatchFace called: ");
    if (mService == null) {
      Log.w(TAG, "No Sidekick service.");
      return false;
    }

    try {
      mService.clearWatchFace();
      return true;
    } catch (RemoteException e) {
      throw e.rethrowFromSystemServer();
    }
  }
    /**
     * Used to replace some components of a previously sent (valid) watch face.
     * All components in the Decomposition must have IDs equal to existing
     * components they will replace.
     * @return Status
     */
  public boolean replaceWatchFaceComponents(WatchFaceDecomposition watchFace) {
    Log.i(TAG, "replaceWatchFaceComponents called: " + watchFace);
    if (mService == null) {
      Log.w(TAG, "No Sidekick service.");
      return false;
    }

    try {
      mService.replaceWatchFaceComponents(watchFace);
      return true;
    } catch (RemoteException e) {
      throw e.rethrowFromSystemServer();
    }
  }
    /**
     * Sends a new set of assets, completely replacing whatever face may have
     * already been in Sidekick.
     * @param forTWM Indicates this WF should be used for TWM
     * @return Status
     */
  public boolean sendWatchFace(WatchFaceDecomposition watchFace, boolean forTWM) {
    Log.i(TAG, "sendWatchFace called: " + watchFace);
    if (mService == null) {
      Log.w(TAG, "No Sidekick service.");
      return false;
    }

    try {
      mService.sendWatchFace(watchFace,forTWM);
      return true;
    } catch (RemoteException e) {
      throw e.rethrowFromSystemServer();
    }
  }
    /**
     * Whether Sidekick should eventually take control of the display
     */
  public void setShouldControlDisplay(boolean visible) {

    Log.i(TAG, "setShouldControlDisplay called: " + visible);
    if (mService == null) {
      Log.w(TAG, "No Sidekick service.");
    }
    try {
      mService.setShouldControlDisplay(visible);
    } catch (RemoteException e) {
      throw e.rethrowFromSystemServer();

    }

  }

}
