package com.google.android.clockwork.settings.system;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.apps.wearable.settings.R;
import com.google.android.clockwork.settings.TimeService;
import com.google.android.clockwork.settings.DefaultSettingsContentResolver;
import com.google.android.clockwork.settings.SettingsContentResolver;
import com.google.android.clockwork.settings.SettingsContract;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public final class DateTimeSettingsHelper {
    private static final String TAG = "DateTimeSettings";

    /** Sends the input intent to TimeService */
    public static void sendTimeServiceIntent(Context context, String action) {
        debugLog("Sending time service intent with action: " + action);
        final Intent intent = new Intent(context, TimeService.class);
        intent.setAction(action);
        context.startService(intent);
    }

    /** Sets the time to the input hour and minute */
    public static void setTime(AlarmManager alarmManager, int hour, int minute) {
        debugLog("Setting time to: " + hour + ":" + minute);
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long when = c.getTimeInMillis();
        if (when / 1000 < Integer.MAX_VALUE) {
            alarmManager.setTime(when);
        }
    }

    /** Sets the date to the input year, month, and day */
    public static void setDate(
            Calendar calendar,
            AlarmManager alarmManager,
            int year,
            int month,
            int day) {
        debugLog("Setting date to: " + year + "/" + month + "/" + day);
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        long when = calendar.getTimeInMillis();
        if (when / 1000 < Integer.MAX_VALUE) {
            alarmManager.setTime(when);
        }
    }

    /** Sets the time zone to the time zone associated with the input time zone id */
    public static void setTimeZone(AlarmManager alarmManager, String timeZoneId) {
        debugLog("Setting time zone to id: " + timeZoneId);
        alarmManager.setTimeZone(timeZoneId);
    }

    /**
     * Formats the time zone offset and name for the input time zone into a string of the form
     * GMT+XX:XX [time zone name]. To fit the string on the watch face, we prefer the short name
     * of the time zone, but when the short display name for the input time zone matches a GMT
     * offset string (like GMT+XX:XX), we simply display the GMT string instead.
     */
    public static String getTimeZoneOffsetAndName(TimeZone tz, Calendar now, Context context) {
        String gmtString = formatOffset(tz.getOffset(now.getTimeInMillis()), context);
        String zoneNameString = tz.getDisplayName(tz.inDaylightTime(now.getTime()), TimeZone.SHORT,
                Locale.getDefault());
        if (zoneNameString == null || zoneNameString.startsWith(context.getString(R.string.gmt))) {
            return gmtString;
        }
        return gmtString + " " + zoneNameString;
    }

    /**
     * Formats the provided timezone offset into a string of the form GMT+XX:XX
     */
    public static String formatOffset(long offset, Context context) {
        long off = offset / 1000 / 60;
        final StringBuilder sb = new StringBuilder();

        sb.append(context.getString(R.string.gmt));
        if (off < 0) {
            sb.append('-');
            off = -off;
        } else {
            sb.append('+');
        }

        int hours = (int) (off / 60);
        int minutes = (int) (off % 60);

        sb.append((char) ('0' + hours / 10));
        sb.append((char) ('0' + hours % 10));
        sb.append(':');
        sb.append((char) ('0' + minutes / 10));
        sb.append((char) ('0' + minutes % 10));

        return sb.toString();
    }

    /**
     * Returns true if the watch is paired with an "alt" phone (iOS), and false otherwise.
     */
    public static boolean isAltMode(SettingsContentResolver settingsContentResolver) {
        return settingsContentResolver.getIntValueForKey(
            SettingsContract.BLUETOOTH_MODE_URI,
            SettingsContract.KEY_BLUETOOTH_MODE,
            SettingsContract.BLUETOOTH_MODE_UNKNOWN) == SettingsContract.BLUETOOTH_MODE_ALT;
    }

    private static void debugLog(String message) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, message);
        }
    }
}