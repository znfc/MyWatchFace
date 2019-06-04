package com.google.android.clockwork.settings.personal.buttons;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.content.res.Resources;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.support.wearable.input.ShadowWearableButtons;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.util.FragmentTestUtil;

/** Unit tests for {@link ButtonSettingsFragment}. */
@RunWith(ClockworkRobolectricTestRunner.class)
public class ButtonsSettingsFragmentTest {
    @Mock Resources mMockResources;
    private ButtonSettingsFragment mFragment;

    @Before
    public void setup() {
        initMocks(this);
        mFragment = new ButtonSettingsFragment();
        mFragment.mResources = mMockResources;
    }

    @Test
    public void testInit_withOverride() {
        when(mMockResources.getString(R.string.button_location_description_stem_1))
                .thenReturn("OEM_STEM_1");
        when(mMockResources.getString(R.string.button_location_description_stem_2))
                .thenReturn("OEM_STEM_2");
        when(mMockResources.getString(R.string.button_location_description_stem_3))
                .thenReturn("OEM_STEM_3");

        FragmentTestUtil.startFragment(mFragment);
        PreferenceScreen prefScreen = mFragment.getPreferenceScreen();

        Assert.assertEquals("Customize hardware buttons", prefScreen.getTitle());
        Assert.assertEquals(2, prefScreen.getPreferenceCount());

        Preference pref1 = prefScreen.getPreference(0);
        Assert.assertEquals("OEM_STEM_1", pref1.getTitle());
        Preference pref2 = prefScreen.getPreference(1);
        Assert.assertEquals("OEM_STEM_2", pref2.getTitle());
    }

    @Test
    public void testInit_withoutOverride() {
        FragmentTestUtil.startFragment(mFragment);
        PreferenceScreen prefScreen = mFragment.getPreferenceScreen();

        Preference pref1 = prefScreen.getPreference(0);
        Assert.assertEquals(ShadowWearableButtons.STEM_1_LABEL, pref1.getTitle());
        Preference pref2 = prefScreen.getPreference(1);
        Assert.assertEquals(ShadowWearableButtons.STEM_2_LABEL, pref2.getTitle());
    }
}
