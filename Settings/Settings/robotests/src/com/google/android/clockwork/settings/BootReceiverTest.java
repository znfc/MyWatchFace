package com.google.android.clockwork.settings;

import static com.google.android.clockwork.settings.testing.VoiceProvidersLeUtils.assertPackageEnabled;
import static com.google.android.clockwork.settings.testing.VoiceProvidersLeUtils.setUpDisabledPackage;
import static com.google.android.clockwork.settings.testing.VoiceProvidersLeUtils.setUpEnabledPackage;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.provider.Settings;
import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.utils.FeatureManager;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

/** Test operations when boot complete received */
@RunWith(ClockworkRobolectricTestRunner.class)
public class BootReceiverTest {

    private static final String LE_NTP_SERVER = "2.android.pool.ntp.org";

    private static final String VOICE_PROVIDER_PACKAGE1 = "package.one";
    private static final String VOICE_PROVIDER_PACKAGE2 = "package.two";
    private static final String VOICE_PROVIDER_PACKAGE3 = "package.three";

    @Mock private FeatureManager mFeatureManager;

    private Context mContext;
    private final BootReceiver mBootReceiver = new BootReceiver();

    @Before
    public void setUp() {
        initMocks(this);
        mContext = RuntimeEnvironment.application;
        FeatureManager.INSTANCE.setTestInstance(mFeatureManager);
    }

    @After
    public void tearDown() {
        FeatureManager.INSTANCE.clearTestInstance();
    }

    @Test
    public void avoidCaptivePortalsForRowDevices() throws Exception {
        when(mFeatureManager.isLocalEditionDevice()).thenReturn(false);
        mBootReceiver.avoidCaptivePortals(mContext);

        assertThat(
                Settings.Global.getInt(
                        mContext.getContentResolver(), Settings.Global.CAPTIVE_PORTAL_MODE),
                is(Settings.Global.CAPTIVE_PORTAL_MODE_AVOID));

        // RoW devices won't change default captive portal detection URLs.
        assertNull(
                Settings.Global.getString(
                        mContext.getContentResolver(), Settings.Global.CAPTIVE_PORTAL_HTTPS_URL));
        assertNull(
                Settings.Global.getString(
                        mContext.getContentResolver(), Settings.Global.CAPTIVE_PORTAL_HTTP_URL));
        assertNull(
                Settings.Global.getString(
                        mContext.getContentResolver(),
                        Settings.Global.CAPTIVE_PORTAL_FALLBACK_URL));
    }

    @Test
    public void avoidCaptivePortalsForLeDevices() throws Exception {
        when(mFeatureManager.isLocalEditionDevice()).thenReturn(true);
        mBootReceiver.avoidCaptivePortals(mContext);

        assertThat(
                Settings.Global.getInt(
                        mContext.getContentResolver(), Settings.Global.CAPTIVE_PORTAL_MODE),
                is(Settings.Global.CAPTIVE_PORTAL_MODE_AVOID));

        // LE devices have to use dedicated URLs that can work in China.
        assertThat(
                Settings.Global.getString(
                        mContext.getContentResolver(), Settings.Global.CAPTIVE_PORTAL_HTTPS_URL),
                is("https://connectivitycheck.gstatic.com/generate_204"));
        assertThat(
                Settings.Global.getString(
                        mContext.getContentResolver(), Settings.Global.CAPTIVE_PORTAL_HTTP_URL),
                is("http://connectivitycheck.gstatic.com/generate_204"));
        assertThat(
                Settings.Global.getString(
                        mContext.getContentResolver(), Settings.Global.CAPTIVE_PORTAL_FALLBACK_URL),
                is("http://connectivitycheck.gstatic.com/generate_204"));
    }

    @Test
    public void updateNtpServerRowDevices() throws Exception {
        when(mFeatureManager.isLocalEditionDevice()).thenReturn(false);
        mBootReceiver.updateNtpServer(mContext);

        assertNull(
                Settings.Global.getString(
                        mContext.getContentResolver(), Settings.Global.NTP_SERVER));
    }

    @Test
    public void updateNtpServerLeDevices() throws Exception {
        when(mFeatureManager.isLocalEditionDevice()).thenReturn(true);
        mBootReceiver.updateNtpServer(mContext);

        assertThat(
                Settings.Global.getString(
                        mContext.getContentResolver(), Settings.Global.NTP_SERVER),
                is(LE_NTP_SERVER));
    }

    @Test
    public void configureDefaultVoiceProviderForRowDevices() {
        when(mFeatureManager.isLocalEditionDevice()).thenReturn(false);
        prepareVoiceProvidersResources(
                new String[] {VOICE_PROVIDER_PACKAGE1, VOICE_PROVIDER_PACKAGE2});
        Context spiedContext = spy(mContext);

        mBootReceiver.configureDefaultVoiceProvider(spiedContext);

        verifyZeroInteractions(spiedContext);
    }

    @Test
    public void configureDefaultVoiceProvidersForLeDisablesOtherProviders() {
        when(mFeatureManager.isLocalEditionDevice()).thenReturn(true);
        prepareVoiceProvidersResources(
                new String[] {
                    VOICE_PROVIDER_PACKAGE1, VOICE_PROVIDER_PACKAGE2, VOICE_PROVIDER_PACKAGE3
                });
        setUpDisabledPackage(VOICE_PROVIDER_PACKAGE1);
        setUpEnabledPackage(VOICE_PROVIDER_PACKAGE2);
        setUpEnabledPackage(VOICE_PROVIDER_PACKAGE3);

        mBootReceiver.configureDefaultVoiceProvider(RuntimeEnvironment.application);

        assertPackageEnabled(
                VOICE_PROVIDER_PACKAGE1, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
        assertPackageEnabled(
                VOICE_PROVIDER_PACKAGE2, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
        assertPackageEnabled(
                VOICE_PROVIDER_PACKAGE3, PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
    }

    @Test
    public void configureDefaultVoiceProvidersForLeEnablesProviderIfNoneIsEnabled() {
        when(mFeatureManager.isLocalEditionDevice()).thenReturn(true);
        prepareVoiceProvidersResources(
                new String[] {VOICE_PROVIDER_PACKAGE1, VOICE_PROVIDER_PACKAGE2});
        setUpDisabledPackage(VOICE_PROVIDER_PACKAGE1);
        setUpDisabledPackage(VOICE_PROVIDER_PACKAGE2);

        mBootReceiver.configureDefaultVoiceProvider(RuntimeEnvironment.application);

        assertPackageEnabled(
                VOICE_PROVIDER_PACKAGE1, PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
        assertPackageEnabled(
                VOICE_PROVIDER_PACKAGE2, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
    }

    @Test
    public void disableDnsOverTlsForLeDevice() {
      when(mFeatureManager.isLocalEditionDevice()).thenReturn(true);
      mBootReceiver.updateDnsOverTls(mContext);

      assertThat(Settings.Global.getString(
                      mContext.getContentResolver(),
                      Settings.Global.PRIVATE_DNS_DEFAULT_MODE),
              is(ConnectivityManager.PRIVATE_DNS_MODE_OFF));
    }

    @Test
    public void DnsOverTlsDefaultNotSetForRoWDevice() {
      when(mFeatureManager.isLocalEditionDevice()).thenReturn(false);
      mBootReceiver.updateDnsOverTls(mContext);

      assertNull(Settings.Global.getString(
              mContext.getContentResolver(), Settings.Global.PRIVATE_DNS_DEFAULT_MODE));
    }

    private void prepareVoiceProvidersResources(String[] voiceProviders) {
        RuntimeEnvironment.application = spy(RuntimeEnvironment.application);
        when(RuntimeEnvironment.application.getApplicationContext())
                .thenReturn(RuntimeEnvironment.application);
        Resources spiedResources = spy(RuntimeEnvironment.application.getResources());
        when(RuntimeEnvironment.application.getResources()).thenReturn(spiedResources);
        when(spiedResources.getStringArray(R.array.config_le_system_voice_assistant_packages))
                .thenReturn(voiceProviders);
    }
}
