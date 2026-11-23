/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.alarm;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.djonique.birdays.R;
import com.djonique.birdays.models.Person;
import com.djonique.birdays.utils.BirdaysPreferencesManager;
import com.djonique.birdays.utils.Constants;
import com.djonique.birdays.utils.Utils;

import java.util.Calendar;

public class AlarmHelper {

    private static final String TAG = "AlarmHelper";
    private static final int REQUEST_CODE_OFFSET = 99;
    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;
    private final Context context;
    private final AlarmManager alarmManager;
    private final SharedPreferences preferences;
    private long additionalNotificationOffset;

    public AlarmHelper(Context context) {
        this.context = context;
        alarmManager = ((AlarmManager)
                context.getApplicationContext().getSystemService(Context.ALARM_SERVICE));
        preferences = BirdaysPreferencesManager.getDefaultSharedPreferences(context);
    }

    public void setAlarms(Person person) {
        // Scheduling while notifications are off would keep the app waking the device (and
        // waking it exactly, when the permission is granted) for a reminder that is discarded
        // on arrival. AlarmSetter and the settings toggle therefore stop at this gate too.
        if (!preferences.getBoolean(Constants.NOTIFICATIONS_KEY, true)) return;
        try {
            setAlarm(person);
            additionalNotificationOffset = Long.parseLong(preferences.getString(Constants.ADDITIONAL_NOTIFICATION_KEY, "0"));
            if (additionalNotificationOffset != 0) {
                setAdditionalAlarm(person);
            }
        } catch (SecurityException e) {
            // Callers on a background thread pass an application context and cannot show a
            // Toast; logging keeps the reason visible in logcat/Crashlytics instead.
            Log.w(TAG, "Alarm scheduling is not permitted", e);
            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(() ->
                        Toast.makeText(context, R.string.security_exception, Toast.LENGTH_LONG).show());
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to schedule alarm for " + (person != null ? person.getName() : "null"), e);
        }
    }

    private void setAlarm(Person person) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(Constants.NAME, person.getName());
        intent.putExtra(Constants.WHEN, context.getString(R.string.today));
        intent.putExtra(Constants.TIME_STAMP, person.getTimeStamp());

        long triggerAtMillis = setupCalendarYear(person, 0);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(),
                Long.hashCode(person.getTimeStamp()), intent, flags);

        setAlarmDependingOnApi(alarmManager, triggerAtMillis, pendingIntent);
    }

    private void setAdditionalAlarm(Person person) {
        additionalNotificationOffset = Long.parseLong(preferences.getString(Constants.ADDITIONAL_NOTIFICATION_KEY, "0"));

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(Constants.ACTION_ADDITIONAL_ALARM);
        intent.putExtra(Constants.NAME, person.getName());
        intent.putExtra(Constants.WHEN, setWhen(additionalNotificationOffset));
        intent.putExtra(Constants.TIME_STAMP, person.getTimeStamp());

        long triggerAtMillis = setupCalendarYear(person, additionalNotificationOffset);

        int requestCode = Long.hashCode(person.getTimeStamp()) + REQUEST_CODE_OFFSET;

        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(),
                requestCode, intent, flags);

        setAlarmDependingOnApi(alarmManager, triggerAtMillis, pendingIntent);
    }

    private void setAlarmDependingOnApi(AlarmManager alarmManager,
                                        long triggerAtMillis,
                                        PendingIntent pendingIntent) {
        if (alarmManager == null) return;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
        } catch (Exception e) {
            Log.w(TAG, "Exact alarm failed, falling back to inexact alarm", e);
            try {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } catch (Exception ex) {
                Log.w(TAG, "Inexact alarm also failed", ex);
            }
        }
    }

    private String setWhen(long offset) {
        String[] dates = context.getResources().getStringArray(R.array.additional_notification_delay);
        String[] entryValues = context.getResources().getStringArray(R.array.additional_notification_entry_values);
        String when = null;
        for (int i = 1; i < entryValues.length; i++) {
            if (offset == Long.parseLong(entryValues[i])) {
                when = dates[i];
            }
        }
        return when;
    }

    public void removeAlarms(long timeStamp) {
        removeAlarm(timeStamp);
        additionalNotificationOffset = Long.parseLong(preferences.getString(Constants.ADDITIONAL_NOTIFICATION_KEY, "0"));
        if (additionalNotificationOffset != 0) {
            removeAdditionalAlarm(timeStamp);
        }
    }

    private void removeAlarm(long timeStamp) {
        Intent intent = new Intent(context, AlarmReceiver.class);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, Long.hashCode(timeStamp), intent, flags);

        if (alarmManager != null) alarmManager.cancel(pendingIntent);
    }

    private void removeAdditionalAlarm(long timeStamp) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(Constants.ACTION_ADDITIONAL_ALARM);

        int requestCode = Long.hashCode(timeStamp) + REQUEST_CODE_OFFSET;

        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags);

        if (alarmManager != null) alarmManager.cancel(pendingIntent);
    }

    /**
     * Trigger time of the next reminder for {@code person}.
     *
     * <p>The reminder is {@code offset} whole days before the birthday, not that many
     * milliseconds: the offset moves the date in calendar days, so a daylight-saving change
     * between the two dates cannot shift the reminder by an hour or onto the wrong day. Only the
     * month and the day of the stored date are used; the birth year is replaced with the year the
     * reminder belongs to.</p>
     */
    private long setupCalendarYear(Person person, long offset) {
        int[] time = Utils.parseNotificationTime(Utils.getNotificationTime(preferences));
        int offsetDays = (int) Math.round((double) offset / DAY_MILLIS);

        Calendar now = Calendar.getInstance();
        int year = now.get(Calendar.YEAR);

        Calendar calendar = birthdayReminder(person, year, offsetDays, time[0], time[1]);
        if (!calendar.after(now)) {
            // This year's reminder has already passed, so the next one belongs to next year's
            // birthday. Rebuilding from the stored date (instead of adding a year to the already
            // shifted reminder) keeps the day offset correct around a leap day.
            calendar = birthdayReminder(person, year + 1, offsetDays, time[0], time[1]);
            while (!calendar.after(now)) {
                calendar.add(Calendar.YEAR, 1);
            }
        }
        return calendar.getTimeInMillis();
    }

    private Calendar birthdayReminder(Person person, int year, int offsetDays, int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(person.getDate());
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (offsetDays != 0) {
            calendar.add(Calendar.DAY_OF_MONTH, -offsetDays);
        }
        return calendar;
    }
}
