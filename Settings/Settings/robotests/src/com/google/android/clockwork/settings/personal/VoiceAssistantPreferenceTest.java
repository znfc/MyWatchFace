package com.google.android.clockwork.settings.personal;

import static com.google.android.clockwork.settings.testing.VoiceProvidersLeUtils.assertPackageEnabled;
import static com.google.android.clockwork.settings.testing.VoiceProvidersLeUtils.setUpDisabledPackage;
import static com.google.android.clockwork.settings.testing.VoiceProvidersLeUtils.setUpEnabledPackage;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

/** Test voice assistant preference */
@RunWith(ClockworkRobolectricTestRunner.class)
public class VoiceAssistantPreferenceTest {

    private static final String VOICE_PROVIDER_PACKAGE1 = "package.one";
    private static final String VOICE_PROVIDER_PACKAGE2 = "package.two";
    private static final String VOICE_PROVIDER_LABEL1 = "Label1";
    private static final String VOICE_PROVIDER_LABEL2 = "Label2";

    @Before
    public void setUpResources() {
        RuntimeEnvironment.application = spy(RuntimeEnvironment.application);
        when(RuntimeEnvironment.application.getApplicationContext())
                .thenReturn(RuntimeEnvironment.application);
        Resources spiedResources = spy(RuntimeEnvironment.application.getResources());
        when(RuntimeEnvironment.application.getResources()).thenReturn(spiedResources);
        when(spiedResources.getStringArray(R.array.config_le_system_voice_assistant_packages))
                .thenReturn(new String[] {VOICE_PROVIDER_PACKAGE1, VOICE_PROVIDER_PACKAGE2});
    }

    @Test
    public void testConstructor() {
        setUpDisabledPackage(VOICE_PROVIDER_PACKAGE1, VOICE_PROVIDER_LABEL1);
        setUpEnabledPackage(VOICE_PROVIDER_PACKAGE2, VOICE_PROVIDER_LABEL2);

        VoiceAssistantPreference voiceAssistantPreference =
                new VoiceAssistantPreference(RuntimeEnvironment.application);
        assertArrayEquals(
                voiceAssistantPreference.getEntryValues(),
                new String[] {VOICE_PROVIDER_PACKAGE1, VOICE_PROVIDER_PACKAGE2});
        assertArrayEquals(
                voiceAssistantPreference.getEntries(),
                new String[] {VOICE_PROVIDER_LABEL1, VOICE_PROVIDER_LABEL2});
        assertEquals(voiceAssistantPreference.getValue(), VOICE_PROVIDER_PACKAGE2);
        assertEquals(voiceAssistantPreference.getSummary(), VOICE_PROVIDER_LABEL2);
    }

    @Test
    public void testConstructorWithMissingPackage() {
        setUpEnabledPackage(VOICE_PROVIDER_PACKAGE1, VOICE_PROVIDER_LABEL1);

        VoiceAssistantPreference voiceAssistantPreference =
                new VoiceAssistantPreference(RuntimeEnvironment.application);
        assertArrayEquals(
                voiceAssistantPreference.getEntryValues(), new String[] {VOICE_PROVIDER_PACKAGE1});
        assertArrayEquals(
                voiceAssistantPreference.getEntries(), new String[] {VOICE_PROVIDER_LABEL1});
        assertEquals(voiceAssistantPreference.getValue(), VOICE_PROVIDER_PACKAGE1);
        assertEquals(voiceAssistantPreference.getSummary(), VOICE_PROVIDER_LABEL1);
    }

    /**
     * Multiple providers should never be enabled at the same time, but in case it happened it
     * shouldn't crash.
     */
    @Test
    public void testConstructorWithMultipleProvidersEnabled() {
        setUpEnabledPackage(VOICE_PROVIDER_PACKAGE1, VOICE_PROVIDER_LABEL1);
        setUpEnabledPackage(VOICE_PROVIDER_PACKAGE2, VOICE_PROVIDER_LABEL2);

        VoiceAssistantPreference voiceAssistantPreference =
                new VoiceAssistantPreference(RuntimeEnvironment.application);
        assertArrayEquals(
                voiceAssistantPreference.getEntryValues(),
                new String[] {VOICE_PROVIDER_PACKAGE1, VOICE_PROVIDER_PACKAGE2});
        assertArrayEquals(
                voiceAssistantPreference.getEntries(),
                new String[] {VOICE_PROVIDER_LABEL1, VOICE_PROVIDER_LABEL2});
        assertEquals(voiceAssistantPreference.getValue(), VOICE_PROVIDER_PACKAGE1);
        assertEquals(voiceAssistantPreference.getSummary(), VOICE_PROVIDER_LABEL1);
    }

    @Test
    public void testTappingChangesProvider() {
        setUpEnabledPackage(VOICE_PROVIDER_PACKAGE1, VOICE_PROVIDER_LABEL1);
        setUpDisabledPackage(VOICE_PROVIDER_PACKAGE2, VOICE_PROVIDER_LABEL2);
        VoiceAssistantPreference voiceAssistantPreference =
                new VoiceAssistantPreference(RuntimeEnvironment.application);

        voiceAssistantPreference
                .getOnPreferenceChangeListener()
                .onPreferenceChange(voiceAssistantPreference, VOICE_PROVIDER_PACKAGE2);

        assertPackageEnabled(
                VOICE_PROVIDER_PACKAGE1, PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
        assertPackageEnabled(
                VOICE_PROVIDER_PACKAGE2, PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
    }

    @Test
    public void testTappingCurrentProviderDoesntTouchProviders() {
        setUpDisabledPackage(VOICE_PROVIDER_PACKAGE1, VOICE_PROVIDER_LABEL1);
        setUpEnabledPackage(VOICE_PROVIDER_PACKAGE2, VOICE_PROVIDER_LABEL2);
        VoiceAssistantPreference voiceAssistantPreference =
                new VoiceAssistantPreference(RuntimeEnvironment.application);

        voiceAssistantPreference
                .getOnPreferenceChangeListener()
                .onPreferenceChange(voiceAssistantPreference, VOICE_PROVIDER_PACKAGE2);

        assertPackageEnabled(
                VOICE_PROVIDER_PACKAGE1, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
        assertPackageEnabled(
                VOICE_PROVIDER_PACKAGE2, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
    }
}
