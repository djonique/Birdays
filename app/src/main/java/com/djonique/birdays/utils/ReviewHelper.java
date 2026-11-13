/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

/**
 * Decides when the app should ask for a review: at least {@link #REQUIRED_LAUNCHES} launches
 * and at least {@link #REQUIRED_DAYS} days since the app was installed.
 */
public final class ReviewHelper {

    public static final int REQUIRED_LAUNCHES = 5;
    public static final int REQUIRED_DAYS = 7;

    private static final String PREFS_NAME = "InAppReview";
    private static final String KEY_LAUNCH_COUNT = "app_launch_count";
    private static final String KEY_REVIEW_REQUESTED = "review_requested";
    private static final long DAY_IN_MILLIS = 24 * 60 * 60 * 1000L;

    private static boolean launchTracked;

    private ReviewHelper() {
    }

    /**
     * Counts one app launch per process lifetime. Call it from the launcher activity, so that
     * the process starts triggered by alarms or widgets are not counted as launches.
     */
    public static void trackLaunch(Context context) {
        if (launchTracked) return;
        launchTracked = true;

        SharedPreferences preferences = getPreferences(context);
        int launchCount = preferences.getInt(KEY_LAUNCH_COUNT, 0) + 1;
        preferences.edit().putInt(KEY_LAUNCH_COUNT, launchCount).apply();
    }

    public static boolean shouldRequestReview(Context context) {
        SharedPreferences preferences = getPreferences(context);
        if (preferences.getBoolean(KEY_REVIEW_REQUESTED, false)) return false;

        int launchCount = preferences.getInt(KEY_LAUNCH_COUNT, 0);
        long daysSinceInstall = System.currentTimeMillis() - getFirstInstallTime(context);
        return launchCount >= REQUIRED_LAUNCHES
                && daysSinceInstall >= REQUIRED_DAYS * DAY_IN_MILLIS;
    }

    public static void markReviewRequested(Context context) {
        getPreferences(context).edit().putBoolean(KEY_REVIEW_REQUESTED, true).apply();
    }

    private static long getFirstInstallTime(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.firstInstallTime;
        } catch (PackageManager.NameNotFoundException e) {
            return System.currentTimeMillis();
        }
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
