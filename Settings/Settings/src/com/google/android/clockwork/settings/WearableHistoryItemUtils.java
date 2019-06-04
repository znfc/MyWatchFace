/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.google.android.clockwork.settings;

import android.os.BatteryStats;
import com.google.android.clockwork.battery.WearableHistoryItem;

public class WearableHistoryItemUtils {
    public static final int[] INPUT_STATE_FLAGS = {
            BatteryStats.HistoryItem.STATE_BRIGHTNESS_SHIFT,
            BatteryStats.HistoryItem.STATE_BRIGHTNESS_MASK,
            BatteryStats.HistoryItem.STATE_PHONE_SIGNAL_STRENGTH_SHIFT,
            BatteryStats.HistoryItem.STATE_PHONE_SIGNAL_STRENGTH_MASK,
            BatteryStats.HistoryItem.STATE_PHONE_STATE_SHIFT,
            BatteryStats.HistoryItem.STATE_PHONE_STATE_MASK,
            BatteryStats.HistoryItem.STATE_DATA_CONNECTION_SHIFT,
            BatteryStats.HistoryItem.STATE_DATA_CONNECTION_MASK,
            BatteryStats.HistoryItem.STATE_CPU_RUNNING_FLAG,
            BatteryStats.HistoryItem.STATE_WAKE_LOCK_FLAG,
            BatteryStats.HistoryItem.STATE_GPS_ON_FLAG,
            BatteryStats.HistoryItem.STATE_WIFI_FULL_LOCK_FLAG,
            BatteryStats.HistoryItem.STATE_WIFI_SCAN_FLAG,
            BatteryStats.HistoryItem.STATE_WIFI_MULTICAST_ON_FLAG,
            BatteryStats.HistoryItem.STATE_MOBILE_RADIO_ACTIVE_FLAG,
            BatteryStats.HistoryItem.STATE_SENSOR_ON_FLAG,
            BatteryStats.HistoryItem.STATE_AUDIO_ON_FLAG,
            BatteryStats.HistoryItem.STATE_PHONE_SCANNING_FLAG,
            BatteryStats.HistoryItem.STATE_SCREEN_ON_FLAG,
            BatteryStats.HistoryItem.STATE_BATTERY_PLUGGED_FLAG,
    };

    public static final int[] OUTPUT_STATE_FLAGS = {
            WearableHistoryItem.STATE_BRIGHTNESS_SHIFT,
            WearableHistoryItem.STATE_BRIGHTNESS_MASK,
            WearableHistoryItem.STATE_PHONE_SIGNAL_STRENGTH_SHIFT,
            WearableHistoryItem.STATE_PHONE_SIGNAL_STRENGTH_MASK,
            WearableHistoryItem.STATE_PHONE_STATE_SHIFT,
            WearableHistoryItem.STATE_PHONE_STATE_MASK,
            WearableHistoryItem.STATE_DATA_CONNECTION_SHIFT,
            WearableHistoryItem.STATE_DATA_CONNECTION_MASK,
            WearableHistoryItem.STATE_CPU_RUNNING_FLAG,
            WearableHistoryItem.STATE_WAKE_LOCK_FLAG,
            WearableHistoryItem.STATE_GPS_ON_FLAG,
            WearableHistoryItem.STATE_WIFI_FULL_LOCK_FLAG,
            WearableHistoryItem.STATE_WIFI_SCAN_FLAG,
            WearableHistoryItem.STATE_WIFI_MULTICAST_ON_FLAG,
            WearableHistoryItem.STATE_MOBILE_RADIO_ACTIVE_FLAG,
            WearableHistoryItem.STATE_SENSOR_ON_FLAG,
            WearableHistoryItem.STATE_AUDIO_ON_FLAG,
            WearableHistoryItem.STATE_PHONE_SCANNING_FLAG,
            WearableHistoryItem.STATE_SCREEN_ON_FLAG,
            WearableHistoryItem.STATE_BATTERY_PLUGGED_FLAG,
    };

    public static final int[] INPUT_STATE2_FLAGS = {
            BatteryStats.HistoryItem.STATE2_WIFI_SUPPL_STATE_SHIFT,
            BatteryStats.HistoryItem.STATE2_WIFI_SUPPL_STATE_MASK,
            BatteryStats.HistoryItem.STATE2_WIFI_SIGNAL_STRENGTH_SHIFT,
            BatteryStats.HistoryItem.STATE2_WIFI_SIGNAL_STRENGTH_MASK,
            BatteryStats.HistoryItem.STATE2_POWER_SAVE_FLAG,
            BatteryStats.HistoryItem.STATE2_VIDEO_ON_FLAG,
            BatteryStats.HistoryItem.STATE2_WIFI_RUNNING_FLAG,
            BatteryStats.HistoryItem.STATE2_WIFI_ON_FLAG,
            BatteryStats.HistoryItem.STATE2_FLASHLIGHT_FLAG,
    };

    public static final int[] OUTPUT_STATE2_FLAGS = {
            WearableHistoryItem.STATE2_WIFI_SUPPL_STATE_SHIFT,
            WearableHistoryItem.STATE2_WIFI_SUPPL_STATE_MASK,
            WearableHistoryItem.STATE2_WIFI_SIGNAL_STRENGTH_SHIFT,
            WearableHistoryItem.STATE2_WIFI_SIGNAL_STRENGTH_MASK,
            WearableHistoryItem.STATE2_LOW_POWER_FLAG,
            WearableHistoryItem.STATE2_VIDEO_ON_FLAG,
            WearableHistoryItem.STATE2_WIFI_RUNNING_FLAG,
            WearableHistoryItem.STATE2_WIFI_ON_FLAG,
            WearableHistoryItem.STATE2_FLASHLIGHT_FLAG,
    };

    public static final byte[] INPUT_CMDS = {
            BatteryStats.HistoryItem.CMD_UPDATE,
            BatteryStats.HistoryItem.CMD_NULL,
            BatteryStats.HistoryItem.CMD_START,
            BatteryStats.HistoryItem.CMD_CURRENT_TIME,
            BatteryStats.HistoryItem.CMD_OVERFLOW,
            BatteryStats.HistoryItem.CMD_RESET,
    };


    public static final byte[] OUTPUT_CMDS = {
            WearableHistoryItem.CMD_UPDATE,
            WearableHistoryItem.CMD_NULL,
            WearableHistoryItem.CMD_START,
            WearableHistoryItem.CMD_CURRENT_TIME,
            WearableHistoryItem.CMD_OVERFLOW,
            WearableHistoryItem.CMD_RESET,
    };

    public static WearableHistoryItem convertHistoryItem(
            BatteryStats.HistoryItem historyItem) {
        return new WearableHistoryItem(
                historyItem.time,
                historyItem.currentTime,
                historyItem.batteryLevel,
                convertState(historyItem.states),
                convertState2(historyItem.states2),
                convertCmd(historyItem.cmd));
    }
    
    private static byte convertCmd(byte cmd) {
        for (int i = 0; i < INPUT_CMDS.length; i++) {
            if (cmd == INPUT_CMDS[i]) {
                return OUTPUT_CMDS[i];
            }
        }

        return WearableHistoryItem.CMD_NULL;
    }

    private static int convertState(int state) {
        return convertInt(state, INPUT_STATE_FLAGS, OUTPUT_STATE_FLAGS);
    }

    private static int convertState2(int state) {
        return convertInt(state, INPUT_STATE2_FLAGS, OUTPUT_STATE2_FLAGS);
    }

    private static int convertInt(int input, int[] inputFlags, int[] outputFlags) {
        int output = 0;
        for (int i = 0; i < inputFlags.length; i++) {
            output |= convertFlag(input, inputFlags[i], outputFlags[i]);
        }

        return output;
    }

    private static int convertFlag(int input, int inFlag, int outFlag) {
        return (input & inFlag) != 0 ? outFlag : 0;
    }
}
