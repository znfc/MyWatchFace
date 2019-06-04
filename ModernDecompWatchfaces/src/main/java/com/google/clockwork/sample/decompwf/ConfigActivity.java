package com.google.clockwork.sample.decompwf;

import android.support.wearable.watchface.decomposition.WatchFaceDecomposition;
import android.support.wearable.watchface.decompositionface.DecompositionConfigActivity;

public class ConfigActivity extends DecompositionConfigActivity {

  @Override
  protected WatchFaceDecomposition buildDecompositionForWatchFace(String watchFaceClassName) {
    switch (watchFaceClassName) {
      case "com.google.clockwork.sample.decompwf.ModernComplicationWf":
        return ModernComplicationWf.createDecomposition(this);
      default:
        throw new IllegalArgumentException("Unknown watch face class name" + watchFaceClassName);
    }
  }

}
