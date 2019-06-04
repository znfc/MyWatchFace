package com.google.android.clockwork.settings.provider;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.os.Bundle;

import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.utils.ChannelCursorQueryUtil;
import com.google.android.clockwork.settings.utils.SettingsCursor;
import com.google.android.clockwork.settings.utils.ParcelableMarshallingUtil;
import com.google.android.clockwork.settings.notification.NotificationBackend;
import com.google.android.clockwork.system.robolectric.ClockworkRobolectricTestRunner;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// TODO(b/62443640): remove "disabled" from end of file to enable
@RunWith(ClockworkRobolectricTestRunner.class)
public class ChannelsPropertiesTestDisabled {

    @Mock
    Context context;
    @Mock
    PackageManager packageManager;
    @Mock
    NotificationBackend notificationBackend;
    @Captor
    ArgumentCaptor<String> packageCaptor;
    @Captor
    ArgumentCaptor<Integer> uIdCaptor;
    @Captor
    ArgumentCaptor<Boolean> enabledCaptor;

    private Supplier<PackageManager> packageManagerSupplier = () -> packageManager;
    private Supplier<Context> contextSupplier = () -> context;

    private String package1 = "package1";
    private String package2 = "package2";
    private String package3 = "package3";
    private int package1Uid = 1;
    private int package2Uid = 2;
    private int package3Uid = 3;

    private String group1Id = "group1";
    private String group2Id = "group2";
    private String group3Id = "group3";
    private String group1Name = "group1name";
    private String group2Name = "group2name";
    private String group3Name = "group3name";
    private NotificationChannelGroup group1;
    private NotificationChannelGroup group2;
    private NotificationChannelGroup group3;

    private String channel1Id = "channel1";
    private String channel1Name = "channel1Name";
    private int channel1Importance = 1;
    private String channel2Id = "channel2";
    private String channel2Name = "channel2Name";
    private int channel2Importance = 2;
    private String channel3Id = "channel3";
    private String channel3Name = "channel3Name";
    private int channel3Importance = 3;
    private NotificationChannel channel1 =
            new NotificationChannel(channel1Id, channel1Name, channel1Importance);
    private NotificationChannel channel2 =
            new NotificationChannel(channel2Id, channel2Name, channel2Importance);
    private NotificationChannel channel3 =
            new NotificationChannel(channel3Id, channel3Name, channel3Importance);

    private List<NotificationChannelGroup> groups1;

    private ChannelsProperties channelsPropertiesUnderTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        try {
            when(packageManager.getPackageUid(package1, anyInt())).thenReturn(package1Uid);
            when(packageManager.getPackageUid(package2, anyInt())).thenReturn(package2Uid);
            when(packageManager.getPackageUid(package3, anyInt())).thenReturn(package3Uid);
        } catch (PackageManager.NameNotFoundException e) {

        }

        channel1 = new NotificationChannel(channel1Id, channel1Name, channel1Importance);
        channel2 = new NotificationChannel(channel2Id, channel2Name, channel2Importance);
        channel3 = new NotificationChannel(channel3Id, channel3Name, channel3Importance);
        group1 = new NotificationChannelGroup(group1Id, group1Name);
        group2 = new NotificationChannelGroup(group2Id, group2Name);
        group3 = new NotificationChannelGroup(group3Id, group3Name);

        groups1 = new ArrayList<NotificationChannelGroup>();
        groups1.add(group1);
        groups1.add(group2);

        when(group1.getChannels()).thenReturn(listForChannels(channel1));
        when(group2.getChannels()).thenReturn(listForChannels(channel2, channel3));

        channelsPropertiesUnderTest =
                new ChannelsProperties(packageManagerSupplier, contextSupplier,
                        notificationBackend);
    }

    @Ignore("b/62443640")
    @Test
    public void testGetBlocked_notBlocked() {
        when(notificationBackend.getNotificationsBanned(package1, package1Uid)).thenReturn(false);
        SettingsCursor cursor = channelsPropertiesUnderTest
                .query(null, ChannelCursorQueryUtil.getQueryBundleForBlockedApp(package1), null);
        Map<String, Boolean> blocked = ChannelCursorQueryUtil.parseBlockedResponse(cursor);
        assertEquals(1, blocked.size());
        assertFalse(blocked.get(package1));
    }

    @Ignore("b/62443640")
    @Test
    public void testGetBlocked_isBlocked() {
        when(notificationBackend.getNotificationsBanned(package1, package1Uid)).thenReturn(true);
        SettingsCursor cursor = channelsPropertiesUnderTest
                .query(null, ChannelCursorQueryUtil.getQueryBundleForBlockedApp(package1), null);
        Map<String, Boolean> blocked = ChannelCursorQueryUtil.parseBlockedResponse(cursor);
        assertEquals(1, blocked.size());
        assertFalse(blocked.get(package1));
    }

    @Ignore("b/62443640")
    @Test
    public void testGetChannelsForPackage_noDefault() {
        when(notificationBackend.getChannelGroups(package1, package1Uid))
                .thenReturn(new ParceledListSlice<NotificationChannelGroup>(groups1));
        SettingsCursor cursor = channelsPropertiesUnderTest
                .query(null, ChannelCursorQueryUtil.getQueryBundleForAllChannels(package1), null);
        Map<String, List<NotificationChannel>> channels =
                ChannelCursorQueryUtil.parseChannelResponse(cursor);
        assertEquals(2, channels.size());
        for (Map.Entry<String, List<NotificationChannel>> entry : channels.entrySet()) {
            if (entry.getKey().equals(group1.getName())) {
                Set<NotificationChannel> channelSet = new HashSet<>(entry.getValue());
                assertTrue(channelSet.contains(channel1));
                assertEquals(1, channelSet.size());
            } else {
                assertTrue(entry.getKey().equals(group2.getName()));
                Set<NotificationChannel> channelSet = new HashSet<>(entry.getValue());
                assertTrue(channelSet.contains(channel2));
                assertTrue(channelSet.contains(channel3));
                assertEquals(2, entry.getValue().size());
            }
        }
    }

    @Ignore("b/62443640")
    @Test
    public void testGetChannelsForPackage_hasDefault() {
        when(notificationBackend.onlyHasDefaultChannel(package1, package1Uid)).thenReturn(true);
        when(notificationBackend
                .getChannel(package1, package1Uid, NotificationChannel.DEFAULT_CHANNEL_ID))
                .thenReturn(channel1);
        SettingsCursor cursor = channelsPropertiesUnderTest.query(null, ChannelCursorQueryUtil.getQueryBundleForAllChannels(package1), null);
        Map<String, List<NotificationChannel>> channels =
                ChannelCursorQueryUtil.parseChannelResponse(cursor);
        assertEquals(1, channels.size());
        Map.Entry<String, List<NotificationChannel>> entry = channels.entrySet().iterator().next();
        assertEquals(group1.getName(), entry.getKey());
        assertEquals(1, entry.getValue().size());
        assertEquals(channel1, entry.getValue().iterator().next());
    }

    @Ignore("b/62443640")
    @Test
    public void testGetSingleChannel() {
        when(notificationBackend.getChannel(package1, package1Uid, channel1Id))
                .thenReturn(channel1);
        SettingsCursor cursor = channelsPropertiesUnderTest.query(null, ChannelCursorQueryUtil.getQueryBundleForSingleChannel(package1, channel1Id), null);
        Map<String, List<NotificationChannel>> channels =
                ChannelCursorQueryUtil.parseChannelResponse(cursor);
        assertEquals(1, channels.size());
        Map.Entry<String, List<NotificationChannel>> entry = channels.entrySet().iterator().next();
        assertEquals(group1.getName(), entry.getKey());
        assertEquals(1, entry.getValue().size());
        assertEquals(channel1, entry.getValue().iterator().next());
    }

    private List<NotificationChannel> listForChannels(NotificationChannel... o) {
        List<NotificationChannel> list = new ArrayList<>(o.length);
        for (NotificationChannel obj : o) {
            list.add(obj);
        }
        return list;
    }
}
