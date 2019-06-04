package com.google.android.clockwork.settings;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.content.Intent;
import android.os.Bundle;
import com.google.android.clockwork.robolectric.shadows.ShadowGservices;
import com.google.android.clockwork.settings.utils.FeatureManager;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.truth.Expect;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.shadows.ShadowApplication;

@RunWith(ClockworkRobolectricTestRunner.class)
public class LeGserviceFlagsOverrideReceiverTest {

    private static final String ACTION_OVERRIDE_GSERVICES =
            "com.google.gservices.intent.action.GSERVICES_OVERRIDE";

    @Rule public final Expect expect = Expect.create();
    @Mock private FeatureManager mFeatureManager;

    private final LeGserviceFlagsOverrideReceiver receiver = new LeGserviceFlagsOverrideReceiver();
    private ImmutableMap<String, Optional<String>> flagsToOverride;

    @Before
    public void setUp() {
        initMocks(this);
        flagsToOverride = LeGserviceFlagsOverrideReceiver.getFlagsToOverride();
        FeatureManager.INSTANCE.setTestInstance(mFeatureManager);
        when(mFeatureManager.isLocalEditionDevice()).thenReturn(true);
    }

    @After
    public void tearDown() {
        ShadowGservices.reset();
        FeatureManager.INSTANCE.clearTestInstance();
    }

    @Test
    public void overrideFlagWhenForcingLeGservicesOverrides() {
        ShadowGservices.override("cw:le_force_override_gservice_flags", true);

        receiver.onReceive(ShadowApplication.getInstance().getApplicationContext(), null);

        Intent intent = ShadowApplication.getInstance().getBroadcastIntents().get(0);
        expect.that(intent.getAction()).isEqualTo(ACTION_OVERRIDE_GSERVICES);
        Bundle bundle = intent.getExtras();
        expect.that(bundle.keySet().size()).isEqualTo(flagsToOverride.size());
        for (String key : bundle.keySet()) {
            String value = bundle.getString(key);
            expect.that(value).isEqualTo(flagsToOverride.get(key).orNull());
        }
    }

    @Test
    public void cleanupWhenNotForcingLeOverrides() {
        ShadowGservices.override("cw:le_force_override_gservice_flags", false);
        ShadowGservices.override("cw:le_gservice_flags_overridden", true);

        receiver.onReceive(ShadowApplication.getInstance().getApplicationContext(), null);

        Intent intent = ShadowApplication.getInstance().getBroadcastIntents().get(0);
        expect.that(intent.getAction()).isEqualTo(ACTION_OVERRIDE_GSERVICES);
        Bundle bundle = intent.getExtras();
        expect.that(bundle.keySet().size()).isEqualTo(flagsToOverride.size());
        for (String key : bundle.keySet()) {
            String value = bundle.getString(key);
            expect.that(value).isNull();
            expect.that(flagsToOverride).containsKey(key);
        }
    }

    @Test
    public void noBroadcastSentOutWhenNoCheckinAndFlagsAlreadyOverridden() {
        ShadowGservices.override("cw:le_force_override_gservice_flags", true);
        ShadowGservices.override("cw:le_gservice_flags_overridden", true);

        receiver.onReceive(ShadowApplication.getInstance().getApplicationContext(), null);

        expect.that(ShadowApplication.getInstance().getBroadcastIntents()).isEmpty();
    }

    @Test
    public void noBroadcastSentOutWhenCheckinHappensAndOverriddenFlagsCleared() {
        ShadowGservices.override("cw:le_force_override_gservice_flags", false);
        ShadowGservices.override("cw:le_gservice_flags_overridden", false);

        receiver.onReceive(ShadowApplication.getInstance().getApplicationContext(), null);

        expect.that(ShadowApplication.getInstance().getBroadcastIntents()).isEmpty();
    }

    @Test
    public void noBroadcastSentOnRoW() {
        when(mFeatureManager.isLocalEditionDevice()).thenReturn(false);
        ShadowGservices.override("cw:le_force_override_gservice_flags", false);
        ShadowGservices.override("cw:le_gservice_flags_overridden", false);

        receiver.onReceive(ShadowApplication.getInstance().getApplicationContext(), null);

        expect.that(ShadowApplication.getInstance().getBroadcastIntents()).isEmpty();
    }
}
