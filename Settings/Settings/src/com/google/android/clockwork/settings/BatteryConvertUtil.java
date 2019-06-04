package com.google.android.clockwork.settings;

import com.android.internal.os.BatterySipper;

public class
        BatteryConvertUtil {
    public static int getCategory(BatterySipper.DrainType drainType) {
        switch (drainType) {
            case IDLE:
                return com.google.android.clockwork.battery.Constants.CATEGORY_IDLE;
            case CELL:
                return com.google.android.clockwork.battery.Constants.CATEGORY_CELL_STANDBY;
            case PHONE:
                return com.google.android.clockwork.battery.Constants.CATEGORY_VOICE_CALLS;
            case WIFI:
                return com.google.android.clockwork.battery.Constants.CATEGORY_WIFI;
            case BLUETOOTH:
                return com.google.android.clockwork.battery.Constants.CATEGORY_BLUETOOTH;
            case SCREEN:
                return com.google.android.clockwork.battery.Constants.CATEGORY_SCREEN;
            case FLASHLIGHT:
                return com.google.android.clockwork.battery.Constants.CATEGORY_FLASHLIGHT;
            case APP:
                return com.google.android.clockwork.battery.Constants.CATEGORY_APP;
            case USER:
                return com.google.android.clockwork.battery.Constants.CATEGORY_USER;
            case UNACCOUNTED:
                return com.google.android.clockwork.battery.Constants.CATEGORY_UNACCOUNTED;
            case OVERCOUNTED:
                return com.google.android.clockwork.battery.Constants.CATEGORY_OVERCOUNTED;
            default:
                return com.google.android.clockwork.battery.Constants.CATEGORY_NONE;
        }
    }
}
