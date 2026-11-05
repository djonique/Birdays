/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.utils;

import android.app.Application;
import com.yandex.mobile.ads.common.YandexAds;

public class BirdaysApplication extends Application {

    private static boolean activityVisible;

    @Override
    public void onCreate() {
        super.onCreate();
        YandexAds.initialize(this, () -> {});
    }

    public static boolean isActivityVisible() {
        return activityVisible;
    }

    public static void activityResumed() {
        activityVisible = true;
    }

    public static void activityPaused() {
        activityVisible = false;
    }
}