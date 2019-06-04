package com.google.android.clockwork.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import com.google.android.clockwork.host.GKeys;
import com.google.android.clockwork.settings.utils.FeatureManager;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

/**
 * {@link BroadcastReceiver} to overrides or cleanup LE safe Gservices default values upon boot or
 * checkin completed. See go/le-force-override-gservices.
 */
public final class LeGserviceFlagsOverrideReceiver extends BroadcastReceiver {

    /**
     * IMPORTANT: Don't delete any entry in this map. If a flag doesn't need to be overridden any
     * more, please change it to absent. This is to avoid forgetting to remove old overridden
     * GService flags. The constant value is wrapped into a function to avoid holding memory in the
     * process given that the event triggers flag overriding is fairly infrequent.
     */
    @VisibleForTesting
    static final ImmutableMap<String, Optional<String>> getFlagsToOverride() {
        ImmutableMap.Builder<String, Optional<String>> flagsBuilder =
                ImmutableMap.<String, Optional<String>>builder();
        flagsBuilder
                // Special flag to indicate the flag overriding succeeds.
                .put("cw:le_gservice_flags_overridden", Optional.of("true"))
                // Default sidewinder whitelisted services to work properly on LE devices.
                .put(
                        "gms:sidewinder:whitelist_by_device",
                        Optional.of(
                                "3,5,8,14,23,24,25,29,40,41,44,45,46,51,57,63,64,71,84,85,89,91,92,157"))
                // Disable sending requests to GCM to avoid sending requests to Google.
                .put("gcm_service_enable", Optional.of("-1"))
                // Disable requests to GCM for registration tokens that are not filtered by
                // gcm_service_enable flag
                .put("c2dm_aid_url", Optional.of(""))
                // Disable subscription requests to GCM from Phenotype.
                .put("gms:phenotype:disable_gcm_interaction", Optional.of("true"))
                // Disable network scheduler to avoid sending requests to Google.
                .put("nts.scheduler_active", Optional.of("false"))
                // Enable checkin dnspatcher to help resolve checkin server under poisoned DNS.
                .put("checkin_enable_dnspatcher", Optional.of("true"))
                // Enable "Find my Phone" application (b/69943490).
                .put("mdm.ring_my_phone_sidewinder", Optional.of("true"));
        return flagsBuilder.build();
    }

    private static final String ACTION_OVERRIDE_GSERVICES =
            "com.google.gservices.intent.action.GSERVICES_OVERRIDE";
    private static final String PERMISSION_WRITE_GSERVICES =
            "com.google.android.providers.gsf.permission.WRITE_GSERVICES";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!FeatureManager.INSTANCE.get(context).isLocalEditionDevice()) {
            return;
        }

        if (GKeys.LE_FORCE_OVERRIDE_GSERVICE_FLAGS.get()
                && !GKeys.LE_GSERVICE_FLAGS_OVERRIDDEN.get()) {
            // This is a fresh device that hasn't got one successful checkin and the corresponding
            // Gservices flags haven't been overridden yet. Override them.
            overrideFlags(context);
            return;
        }

        if (!GKeys.LE_FORCE_OVERRIDE_GSERVICE_FLAGS.get()
                && GKeys.LE_GSERVICE_FLAGS_OVERRIDDEN.get()) {
            // There is at least one successful checkin and the flags are still overridden. Cleanup
            // all existing overridden flags to restore state.
            clearOverriddenFlags(context);
            return;
        }
    }

    private static void overrideFlags(Context context) {
        Intent gserviceIntent = new Intent(ACTION_OVERRIDE_GSERVICES);
        ImmutableMap<String, Optional<String>> flagsToOverride = getFlagsToOverride();

        for (ImmutableMap.Entry<String, Optional<String>> entry : flagsToOverride.entrySet()) {
            gserviceIntent.putExtra(entry.getKey(), entry.getValue().orNull());
        }

        context.sendBroadcast(gserviceIntent, PERMISSION_WRITE_GSERVICES);
    }

    private static void clearOverriddenFlags(Context context) {
        Intent gserviceIntent = new Intent(ACTION_OVERRIDE_GSERVICES);
        ImmutableMap<String, Optional<String>> flagsToOverride = getFlagsToOverride();

        for (ImmutableMap.Entry<String, Optional<String>> entry : flagsToOverride.entrySet()) {
            gserviceIntent.putExtra(entry.getKey(), (String) null);
        }

        context.sendBroadcast(gserviceIntent, PERMISSION_WRITE_GSERVICES);
    }
}
