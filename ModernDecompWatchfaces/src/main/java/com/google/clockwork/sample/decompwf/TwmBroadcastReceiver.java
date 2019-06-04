package com.google.clockwork.sample.decompwf;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class TwmBroadcastReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    Bundle b = new Bundle();
    b.putParcelable("twm_decomposition", ModernDecompWf.createDecomposition(context));
    setResultExtras(b);
  }
}



