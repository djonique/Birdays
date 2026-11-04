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