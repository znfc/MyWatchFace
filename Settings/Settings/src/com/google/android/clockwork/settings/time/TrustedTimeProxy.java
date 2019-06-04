package com.google.android.clockwork.settings.time;

import android.net.Network;
import android.util.NtpTrustedTime;

/**
 * Proxy class for {@link android.util.NtpTrustedTime}.
 */
public class TrustedTimeProxy {

  private final NtpTrustedTime mTrustedTime;

  public TrustedTimeProxy(NtpTrustedTime trustedTime) {
    mTrustedTime = trustedTime;
  }

  public boolean forceRefresh() {
    return mTrustedTime.forceRefresh();
  }

  public boolean forceRefresh(Network network) {
    return mTrustedTime.forceRefresh(network);
  }

  public boolean hasCache() {
    return mTrustedTime.hasCache();
  }

  public long getCacheAge() {
    return mTrustedTime.getCacheAge();
  }

  public long getCacheCertainty() {
    return mTrustedTime.getCacheCertainty();
  }

  public long currentTimeMillis() {
    return mTrustedTime.currentTimeMillis();
  }
}