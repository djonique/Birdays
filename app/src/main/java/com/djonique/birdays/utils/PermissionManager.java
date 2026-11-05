/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
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
     * Requests the permissions needed for the initial contacts import in a single prompt:
     * reading contacts and, on Android 13+, posting notifications.
     */
    public static void requestReadingContactsAndNotificationsPermission(Activity activity) {
        if (notificationsPermissionGranted(activity)) {
            requestReadingContactsPermission(activity);
        } else {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.POST_NOTIFICATIONS},
                    Constants.READ_CONTACTS_PERMISSION_CODE);
        }
    }

    /**
     * Checks if posting notifications is allowed. The permission does not exist before Android 13.
     */
    public static boolean notificationsPermissionGranted(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests posting notifications permission on Android 13+
     */
    public static void requestNotificationsPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.POST_NOTIFICATIONS},
                Constants.NOTIFICATIONS_PERMISSION_CODE);
    }

    /**
     * Checks if permission for writing and reading to SD card is granted
     */
    public static boolean writingSdPermissionGranted(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests writing to SD card permission if it is not granted
     */
    public static void requestWritingSdPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                Constants.WRITE_EXTERNAL_STORAGE_PERMISSION_CODE);
    }

    /**
     * Requests reading to SD card permission if it is not granted
     */
    public static void requestReadingSdPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                Constants.READ_EXTERNAL_STORAGE_PERMISSION_CODE);
    }
}