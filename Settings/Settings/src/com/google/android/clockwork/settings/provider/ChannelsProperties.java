package com.google.android.clockwork.settings.provider;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.CancellationSignal;

import com.google.android.clockwork.settings.ChannelsConfig;
import com.google.android.clockwork.settings.SettingsContract;
import com.google.android.clockwork.settings.utils.ChannelCursorQueryUtil;
import com.google.android.clockwork.settings.utils.ParcelableMarshallingUtil;
import com.google.android.clockwork.settings.utils.SettingsCursor;
import com.google.android.clockwork.settings.notification.NotificationBackend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.List;

/**
 * System properties related to Notification Channels
 */
public class ChannelsProperties extends SettingProperties implements ChannelsConfig {

    private NotificationBackend mBackend;
    private Supplier<PackageManager> mPackageManagerSupplier;
    private Supplier<Context> mContextSupplier;

    public ChannelsProperties(Supplier<PackageManager> pm, Supplier<Context> context,
                              NotificationBackend backend) {
        super(SettingsContract.CHANNELS_PATH);
        mPackageManagerSupplier = pm;
        mContextSupplier = context;
        mBackend = backend;
    }

    @Override
    public SettingsCursor query(String[] projection, Bundle queryArgs,
                                CancellationSignal cancellationSignal) {
        final long prevUid = Binder.clearCallingIdentity();
        try {
            SettingsCursor cursor = ChannelCursorQueryUtil
                    .parseQueryAndGetResponse(mContextSupplier.get(), queryArgs, this);
            cursor = cursor == null ? new SettingsCursor() : cursor;
            return cursor;
        } finally {
            Binder.restoreCallingIdentity(prevUid);
        }
    }

    /**
     * This is a stub - queries without parameters here don't make semantic sense
     */
    @Override
    public SettingsCursor query() {
        return new SettingsCursor();
    }

    @Override
    public int update(ContentValues values) {
        final long prevUid = Binder.clearCallingIdentity();
        try {
            return ChannelCursorQueryUtil
                    .parseContentValuesAndUpdate(mContextSupplier.get(), values, this);
        } finally {
            Binder.restoreCallingIdentity(prevUid);
        }
    }

    @Override
    public Map<String, Boolean> areAppsBlocked(Context context, List<String> packages) {
        Map<String, Boolean> blocked = new HashMap<>();
        for (String pkg : packages) {
            blocked.put(pkg, getIsBlocked(pkg));
        }
        return blocked;
    }

    @Override
    public boolean isAppBlocked(Context context, String packageName) {
        return getIsBlocked(packageName);
    }

    @Override
    public Map<String, List<NotificationChannel>> getChannel(Context context, String packageName,
                                                      String id) {
        Map<String, List<NotificationChannel>> channels = new HashMap<>();
        NotificationChannel channel = getChannel(packageName, id);
        String groupName = "";
        if (channel.getGroup() != null) {
            groupName = getGroupNameForChannel(packageName, channel);
        }
        List<NotificationChannel> channelList = new ArrayList<>();
        channelList.add(channel);
        channels.put(groupName, channelList);
        return channels;
    }

    @Override
    public Map<String, List<NotificationChannel>> getChannelsForApp(Context context, String packageName) {
        Map<String, List<NotificationChannel>> channels = new HashMap<>();
        if (onlyHasDefaultChannel(packageName)) {
            NotificationChannel channel =
                    getChannel(packageName, NotificationChannel.DEFAULT_CHANNEL_ID);
            String groupName = "";
            if (channel.getGroup() != null) {
                groupName = getGroupNameForChannel(packageName, channel);
            }
            List<NotificationChannel> channelList = new ArrayList<>();
            channelList.add(channel);
            channels.put(groupName, channelList);
        } else {
            List<NotificationChannelGroup> groups = getChannelGroupsForPackage(packageName);
            if (groups != null) {
                for (NotificationChannelGroup group : groups) {
                    String groupName = getGroupNameOrEmpty(group);
                    List<NotificationChannel> channelList = group.getChannels();
                    Collections.sort(channelList, mChannelComparator);
                    channels.put(groupName, channelList);
                }
            }
        }
        return channels;
    }

    @Override
    public int blockOrUnblockApp(Context context, boolean shouldBlock, String packageName) {
        try {
            int uId = mPackageManagerSupplier.get().getPackageUid(packageName, 0);
            mBackend.setNotificationsEnabledForPackage(packageName, uId, !shouldBlock);
            return 1;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    @Override
    public int setChannel(Context context, NotificationChannel channel, String packageName) {
        try {
            int uId = mPackageManagerSupplier.get().getPackageUid(packageName, 0);
            mBackend.updateChannel(packageName, uId, channel);
            return 1;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    private void addSingleChannelToCursor(String packageName, String channelId,
                                          SettingsCursor cursor) {
        NotificationChannel channel = getChannel(packageName, channelId);
        String groupName = "";
        if (channel.getGroup() != null) {
            groupName = getGroupNameForChannel(packageName, channel);
        }
        addChannelToCursor(channel, groupName, cursor);
    }

    private void addChannelsFromPackageToCursor(String packageName, SettingsCursor cursor) {
        boolean onlyHasDefault = onlyHasDefaultChannel(packageName);
        if (onlyHasDefault) {
            addSingleChannelToCursor(packageName, NotificationChannel.DEFAULT_CHANNEL_ID, cursor);
        } else {
            List<NotificationChannelGroup> groups = getChannelGroupsForPackage(packageName);
            for (NotificationChannelGroup group : groups) {
                final String groupName = getGroupNameOrEmpty(group);
                final List<NotificationChannel> channels = group.getChannels();
                Collections.sort(channels, mChannelComparator);
                for (NotificationChannel channel : channels) {
                    addChannelToCursor(channel, groupName, cursor);
                }
            }
        }
    }

    private void addChannelToCursor(NotificationChannel channel, String groupName,
                                    SettingsCursor cursor) {
        cursor.addRow(new Object[]{groupName, ParcelableMarshallingUtil.marshall(channel)});
    }

    /**
     * Retrieves the group name from a channel via a group lookup
     */
    private String getGroupNameForChannel(String packageName, NotificationChannel channel) {
        try {
            int uId = mPackageManagerSupplier.get().getPackageUid(packageName, 0);
            return getGroupNameOrEmpty(mBackend.getGroup(channel.getGroup(), packageName, uId));
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    /**
     * Checks if the only channel a given app has is the default one
     */
    private boolean onlyHasDefaultChannel(String packageName) {
        try {
            int uId = mPackageManagerSupplier.get().getPackageUid(packageName, 0);
            return mBackend.onlyHasDefaultChannel(packageName, uId);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private NotificationChannel getChannel(String packageName, String channelId) {
        try {
            int uId = mPackageManagerSupplier.get().getPackageUid(packageName, 0);
            return mBackend.getChannel(packageName, uId, channelId);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private List<NotificationChannelGroup> getChannelGroupsForPackage(String packageName) {
        try {
            int uId = mPackageManagerSupplier.get().getPackageUid(packageName, 0);
            List<NotificationChannelGroup> groups =
                    mBackend.getChannelGroups(packageName, uId).getList();
            Collections.sort(groups, mChannelGroupComparator);
            return groups;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private int updateChannel(NotificationChannel channel, String packageName) {
        try {
            int uId = mPackageManagerSupplier.get().getPackageUid(packageName, 0);
            mBackend.updateChannel(packageName, uId, channel);
            return 1;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    /**
     * Returns whether or not all notifications for a given app are blocked
     */
    private boolean getIsBlocked(String packageName) {
        try {
            int uId = mPackageManagerSupplier.get().getPackageUid(packageName, 0);
            return mBackend.getNotificationsBanned(packageName, uId);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Blocks notifciations (or unblocks them) for a given app
     */
    private int blockOrUnblockApp(boolean shouldBlock, String packageName) {
        try {
            int uId = mPackageManagerSupplier.get().getPackageUid(packageName, 0);
            mBackend.setNotificationsEnabledForPackage(packageName, uId, !shouldBlock);
            return 1;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    /**
     * Returns a group's name as a string or the empty string if the group's name is null.
     */
    private static String getGroupNameOrEmpty(NotificationChannelGroup group) {
        if (group.getName() == null) {
            return "";
        } else {
            return group.getName().toString();
        }
    }

    /**
     * Comparator for sorting channels - deleted below non-deleted, then alphabetically by name
     */
    private Comparator<NotificationChannel> mChannelComparator =
            new Comparator<NotificationChannel>() {

                @Override
                public int compare(NotificationChannel left, NotificationChannel right) {
                    if (left.isDeleted() != right.isDeleted()) {
                        return Boolean.compare(left.isDeleted(), right.isDeleted());
                    }
                    return left.getId().compareTo(right.getId());
                }
            };

    /**
     * Comparator for sorting channel groups - alphabetically by name, with non-grouped ones last
     */
    private Comparator<NotificationChannelGroup> mChannelGroupComparator =
            new Comparator<NotificationChannelGroup>() {

                @Override
                public int compare(NotificationChannelGroup left, NotificationChannelGroup right) {
                    // Non-grouped channels (in placeholder group with a null id) come last
                    if (left.getId() == null && right.getId() != null) {
                        return 1;
                    } else if (right.getId() == null && left.getId() != null) {
                        return -1;
                    }
                    return left.getId().compareTo(right.getId());
                }
            };
}
