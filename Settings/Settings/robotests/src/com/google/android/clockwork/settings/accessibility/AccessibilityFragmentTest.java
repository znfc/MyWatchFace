package com.google.android.clockwork.settings.accessibility;

import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ClockworkRobolectricTestRunner.class)
public class AccessibilityFragmentTest {
  private AccessibilityFragment mFragment;

  @Before
  public void setup() {
    mFragment = new AccessibilityFragment();
  }

  @Test
  public void accessibilityLabelHasExperimental() {
    Assert.assertEquals("Switch Access (experimental)",
        mFragment.munchAccessibilityLabel(RuntimeEnvironment.application, "Switch Access"));
    Assert.assertEquals("TalkBack (experimental)",
        mFragment.munchAccessibilityLabel(RuntimeEnvironment.application, "TalkBack"));
    Assert.assertEquals("blah",
        mFragment.munchAccessibilityLabel(RuntimeEnvironment.application, "blah"));
  }

}
