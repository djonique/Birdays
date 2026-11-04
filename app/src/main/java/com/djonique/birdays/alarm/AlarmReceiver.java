/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.alarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.djonique.birdays.R;
import com.djonique.birdays.activities.DetailActivity;
import com.djonique.birdays.utils.BirdaysApplication;
import com.djonique.birdays.utils.Constants;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "com.djonique.birdays";

    private NotificationManager manager;
    private SharedPreferences preferences;

    @Override
    public void onReceive(Context context, Intent intent) {

        manager = ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE));

        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        String name = intent.getStringExtra(Constants.NAME);
        String when = intent.getStringExtra(Constants.WHEN);
        long timeStamp = intent.getLongExtra(Constants.TIME_STAMP, 0);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(getResultIntent(context, timeStamp, intent))
                .getPendingIntent(((int) timeStamp), flags);

        createNotificationChannel(context);

        NotificationCompat.Builder builder = buildNotification(context, name, when);

        setDefaultsAndRingtone(builder);

        builder.setContentIntent(pendingIntent);

        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        if (manager != null) {
            manager.notify((int) timeStamp, notification);
        }
    }

    private Intent getResultIntent(Context context, long timeStamp, Intent intent) {
        Intent resultIntent = new Intent(context, DetailActivity.class);
        resultIntent.putExtra(Constants.TIME_STAMP, timeStamp);
        if (BirdaysApplication.isActivityVisible()) {
            resultIntent = intent;
        }
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return resultIntent;
    }

    private void createNotificationChannel(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    context.getString(R.string.channel_name), NotificationManager.IMPORTANCE_HIGH);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private NotificationCompat.Builder buildNotification(Context context, String title, String text) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID);
        builder.setContentTitle(title);
        builder.setContentText(text);
        builder.setSmallIcon(R.drawable.ic_notification);
        builder.setColor(ContextCompat.getColor(context, R.color.accent_green_200));
        builder.setCategory(NotificationCompat.CATEGORY_EVENT);
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        return builder;
    }

    private void setDefaultsAndRingtone(NotificationCompat.Builder builder) {
        String ringtone = preferences.getString(Constants.RINGTONE_KEY,
                Settings.System.DEFAULT_NOTIFICATION_URI.toString());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                setRingtone(builder, Uri.parse(ringtone));
            } catch (Exception e) {
                builder.setDefaults(NotificationCompat.DEFAULT_ALL);
            }
        } else {
            setRingtone(builder, Uri.parse(ringtone));
        }
    }

    private void setRingtone(NotificationCompat.Builder builder, Uri ringtoneUri) {
        builder.setDefaults(NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_LIGHTS);
        builder.setSound(ringtoneUri);
    }
}