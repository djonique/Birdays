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
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import com.djonique.birdays.R;
import com.djonique.birdays.models.Person;
import com.djonique.birdays.utils.Constants;
import com.djonique.birdays.utils.Utils;

import java.util.Calendar;

public class AlarmHelper {

    private static final int REQUEST_CODE_OFFSET = 99;
    private final long defaultNotificationTime = 645703200000L - Utils.getTimeOffset();
    private final Context context;
    private final AlarmManager alarmManager;
    private final SharedPreferences preferences;
    private long additionalNotificationOffset;

    public AlarmHelper(Context context) {
        this.context = context;
        alarmManager = ((AlarmManager)
                context.getApplicationContext().getSystemService(Context.ALARM_SERVICE));
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void setAlarms(Person person) {
        try {
            setAlarm(person);
            additionalNotificationOffset = Long.parseLong(preferences.getString(Constants.ADDITIONAL_NOTIFICATION_KEY, "0"));
            if (additionalNotificationOffset != 0) {
                setAdditionalAlarm(person);
            }
        } catch (SecurityException e) {
            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(() ->
                        Toast.makeText(context, R.string.security_exception, Toast.LENGTH_LONG).show());
            }
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
                (int) person.getTimeStamp(), intent, flags);

        setAlarmDependingOnApi(alarmManager, triggerAtMillis, pendingIntent);
    }

    private void setAdditionalAlarm(Person person) {
        additionalNotificationOffset = Long.parseLong(preferences.getString(Constants.ADDITIONAL_NOTIFICATION_KEY, "0"));

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(Constants.NAME, person.getName());
        intent.putExtra(Constants.WHEN, setWhen(additionalNotificationOffset));
        intent.putExtra(Constants.TIME_STAMP, person.getTimeStamp());

        long triggerAtMillis = setupCalendarYear(person, additionalNotificationOffset);

        int requestCode = (int) person.getTimeStamp() + REQUEST_CODE_OFFSET;

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
        } catch (SecurityException e) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
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

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, (int) timeStamp, intent, flags);

        if (alarmManager != null) alarmManager.cancel(pendingIntent);
    }

    private void removeAdditionalAlarm(long timeStamp) {
        Intent intent = new Intent(context, AlarmReceiver.class);

        int requestCode = (int) timeStamp + REQUEST_CODE_OFFSET;

        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags);

        if (alarmManager != null) alarmManager.cancel(pendingIntent);
    }

    private long setupCalendarYear(Person person, long offset) {
        long now = Calendar.getInstance().getTimeInMillis();
        long notificationTime = preferences.getLong(Constants.NOTIFICATION_TIME_KEY, defaultNotificationTime);
        Calendar notificationTimeCalendar = Calendar.getInstance();
        notificationTimeCalendar.setTimeInMillis(notificationTime);

        int hour = notificationTimeCalendar.get(Calendar.HOUR_OF_DAY);
        int minutes = notificationTimeCalendar.get(Calendar.MINUTE);

        long date = person.getDate() - offset;
        int year = Calendar.getInstance().get(Calendar.YEAR);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date);
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minutes);
        calendar.set(Calendar.MILLISECOND, 0);
        if (now > calendar.getTimeInMillis()) {
            calendar.set(Calendar.YEAR, year + 1);
        }
        return calendar.getTimeInMillis();
    }
}