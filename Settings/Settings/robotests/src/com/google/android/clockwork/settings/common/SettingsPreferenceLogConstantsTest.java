package com.google.android.clockwork.settings.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import com.google.protos.wireless.android.clockwork.apps.logs.CwEnums.CwSettingsUiEvent;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

/** Test class for SettingsPreferenceLogConstants */
@RunWith(ClockworkRobolectricTestRunner.class)
public class SettingsPreferenceLogConstantsTest {
  private static final String VALID_PREF_KEY = "pref_yolo";
  private static final String INVALID_PREF_KEY = "pref_bogus_preference_key_xyzzy";
  private static final String IGNORED_PREF_KEY = "ignore_TheRestIsNotChecked";

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testNullKey() {
    SettingsPreferenceLogConstants.sUserDebugOrEngBuild = false;
    CwSettingsUiEvent event = SettingsPreferenceLogConstants.getLoggingId(null);
    assertNull(event);
  }

  @Test
  public void testUserBuild_ValidPrefKey() {
    SettingsPreferenceLogConstants.sUserDebugOrEngBuild = false;
    CwSettingsUiEvent event
            = SettingsPreferenceLogConstants.getLoggingId(VALID_PREF_KEY);
    assertNotNull(event);
  }

  @Test
  public void testUserBuild_InvalidPrefKey() {
    SettingsPreferenceLogConstants.sUserDebugOrEngBuild = false;
    CwSettingsUiEvent event = SettingsPreferenceLogConstants.getLoggingId(INVALID_PREF_KEY);
    assertEquals(event, CwSettingsUiEvent.UNKNOWN);
  }

  @Test
  public void testUserBuild_IgnoredPrefKey() {
    SettingsPreferenceLogConstants.sUserDebugOrEngBuild = false;
    CwSettingsUiEvent event = SettingsPreferenceLogConstants.getLoggingId(IGNORED_PREF_KEY);
    assertEquals(event, CwSettingsUiEvent.UNKNOWN);
  }

  @Test
  public void testUserDebugOrEngBuild_ValidPrefKey() {
    SettingsPreferenceLogConstants.sUserDebugOrEngBuild = true;
    CwSettingsUiEvent event = SettingsPreferenceLogConstants.getLoggingId(VALID_PREF_KEY);
    assertNotNull(event);
  }

  @Test
  public void testUserDebugOrEngBuild_InvalidPrefKey() {
    thrown.expect(IllegalArgumentException.class);
    SettingsPreferenceLogConstants.sUserDebugOrEngBuild = true;
    CwSettingsUiEvent event = SettingsPreferenceLogConstants.getLoggingId(INVALID_PREF_KEY);
  }

  @Test
  public void testUserDebugOrEngBuild_IgnoredPrefKey() {
    SettingsPreferenceLogConstants.sUserDebugOrEngBuild = true;
    CwSettingsUiEvent event = SettingsPreferenceLogConstants.getLoggingId(IGNORED_PREF_KEY);
    assertEquals(event, CwSettingsUiEvent.UNKNOWN);
  }
}
