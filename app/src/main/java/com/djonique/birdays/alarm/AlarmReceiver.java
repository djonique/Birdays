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
import android.media.AudioAttributes;
import android.net.Uri;
import android.provider.Settings;

import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;

import com.djonique.birdays.R;
import com.djonique.birdays.activities.DetailActivity;
import com.djonique.birdays.database.DbHelper;
import com.djonique.birdays.models.Person;
import com.djonique.birdays.utils.BirdaysPreferencesManager;
import com.djonique.birdays.utils.Constants;
import com.djonique.birdays.utils.NamedDaemonThreadFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "com.djonique.birdays";

    private NotificationManager manager;
    private SharedPreferences preferences;

    private static AudioAttributes notificationAudioAttributes() {
        return new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build();
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        manager = ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE));
        preferences = BirdaysPreferencesManager.getDefaultSharedPreferences(context);

        // Reminders can still be in the system queue for a moment after the user switches them
        // off (or the toggle write races with a firing alarm). Nothing is posted and nothing is
        // re-armed in that case.
        if (!preferences.getBoolean(Constants.NOTIFICATIONS_KEY, true)) return;

        createNotificationChannel(context);

        String name = intent.getStringExtra(Constants.NAME);
        String when = intent.getStringExtra(Constants.WHEN);
        long timeStamp = intent.getLongExtra(Constants.TIME_STAMP, 0);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

        PendingIntent pendingIntent = TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(getResultIntent(context, timeStamp))
                .getPendingIntent(Long.hashCode(timeStamp), flags);

        NotificationCompat.Builder builder = buildNotification(context, name, when);

        setDefaultsAndRingtone(builder);

        builder.setContentIntent(pendingIntent);

        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        if (manager != null) {
            manager.notify(Long.hashCode(timeStamp), notification);
        }

        rearmNextYear(context, timeStamp);
    }

    /**
     * Birthday alarms fire once a year, so the next occurrence has to be scheduled after the
     * notification is posted. Without this the alarm is consumed and the contact never gets a
     * notification again until the app rewrites all alarms.
     */
    private void rearmNextYear(Context context, long timeStamp) {
        if (timeStamp == 0) return;

        final PendingResult pendingResult = goAsync();
        final ExecutorService executor =
                Executors.newSingleThreadExecutor(new NamedDaemonThreadFactory("alarm-rearm"));

        executor.execute(() -> {
            try (DbHelper dbHelper = new DbHelper(context)) {
                Person person = dbHelper.query().getPerson(timeStamp);
                if (person != null) {
                    new AlarmHelper(context).setAlarms(person);
                }
            } catch (Exception ignored) {
                // The alarm simply stays unscheduled; the next app launch re-creates it.
            } finally {
                executor.shutdown();
                if (pendingResult != null) {
                    pendingResult.finish();
                }
            }
        });
    }

    /**
     * Builds a clean navigation intent. The broadcast intent must never be reused: it carries the
     * alarm's own extras and, when the list screen is visible, was previously handed to
     * TaskStackBuilder as-is, which produced an intent that did not resolve to DetailActivity.
     */
    Intent getResultIntent(Context context, long timeStamp) {
        Intent resultIntent = new Intent(context, DetailActivity.class);
        resultIntent.putExtra(Constants.TIME_STAMP, timeStamp);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return resultIntent;
    }

    /**
     * The channel is created once and never deleted: removing it would reset the user's own
     * importance and sound choices. Android ignores later sound/importance changes to a channel,
     * so the stored ringtone is applied here at creation time. A ringtone picked afterward is a
     * documented platform limitation and can only be changed in the system settings for the
     * channel; on API 25 and lower the per-notification sound below still applies.
     */
    private void createNotificationChannel(Context context) {
        if (manager == null) return;
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return;

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                context.getString(R.string.channel_name), NotificationManager.IMPORTANCE_HIGH);
        channel.enableLights(true);
        channel.enableVibration(true);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel.setSound(channelSound(), notificationAudioAttributes());
        manager.createNotificationChannel(channel);
    }

    /**
     * The ringtone selected at the moment the channel is first created; null means "Silent".
     */
    private Uri channelSound() {
        String ringtone = preferences.getString(Constants.RINGTONE_KEY,
                Settings.System.DEFAULT_NOTIFICATION_URI.toString());
        if (ringtone.isEmpty()) return null;
        try {
            return Uri.parse(ringtone);
        } catch (Exception e) {
            return Settings.System.DEFAULT_NOTIFICATION_URI;
        }
    }

    private NotificationCompat.Builder buildNotification(Context context, String title, String text) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID);
        builder.setContentTitle(title);
        builder.setContentText(text);
        builder.setSmallIcon(R.drawable.ic_notification);
        builder.setColor(ContextCompat.getColor(context, R.color.md_theme_secondary));
        builder.setCategory(NotificationCompat.CATEGORY_EVENT);
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        return builder;
    }

    /**
     * Only relevant on API 25 and lower: once a notification is posted to a channel (API 26+),
     * the channel decides the sound and the builder value is ignored.
     */
    private void setDefaultsAndRingtone(NotificationCompat.Builder builder) {
        String ringtone = preferences.getString(Constants.RINGTONE_KEY,
                Settings.System.DEFAULT_NOTIFICATION_URI.toString());
        if (ringtone.isEmpty()) {
            builder.setDefaults(NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_LIGHTS);
            return;
        }
        try {
            builder.setDefaults(NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_LIGHTS);
            builder.setSound(Uri.parse(ringtone));
        } catch (Exception e) {
            builder.setDefaults(NotificationCompat.DEFAULT_ALL);
        }
    }
}
