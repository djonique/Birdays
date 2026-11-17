/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class PermissionManager {

    /**
     * Checks if permission for reading contacts is granted
     */
    public static boolean readingContactsPermissionGranted(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests reading contacts permission if it is not granted
     */
    public static void requestReadingContactsPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_CONTACTS},
                Constants.READ_CONTACTS_PERMISSION_CODE);
    }

    /**
     * Checks if the app is allowed to post notifications: the runtime permission on Android 13+
     * and the system switch for the app on older versions.
     */
    public static boolean notificationsPermissionGranted(Context context) {
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    /**
     * Checks if the notification permission can still be requested at runtime.
     * Before Android 13 the permission does not exist, so only the system settings can help.
     */
    public static boolean canRequestNotificationsPermission(Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests posting notifications permission on Android 13+. On older versions notifications
     * are disabled only by the system switch, so the notification settings screen is opened.
     */
    @SuppressLint("InlinedApi")
    public static void requestNotificationsPermission(Activity activity) {
        if (canRequestNotificationsPermission(activity)) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    Constants.NOTIFICATIONS_PERMISSION_CODE);
        } else {
            openNotificationSettings(activity);
        }
    }

    /**
     * Opens the system notification settings of the app
     */
    public static void openNotificationSettings(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, activity.getPackageName());
        try {
            activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            try {
                activity.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + activity.getPackageName())));
            } catch (ActivityNotFoundException ignored) {
            }
        }
    }
}
