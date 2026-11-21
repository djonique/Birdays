/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.utils;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import com.djonique.birdays.repository.BirdaysRepository;
import com.yandex.mobile.ads.common.YandexAds;

public class BirdaysApplication extends Application {

    private static final String TAG = "BirdaysAds";

    private static boolean activityVisible;

    public static boolean isActivityVisible() {
        return activityVisible;
    }

    public static void activityResumed() {
        activityVisible = true;
    }

    public static void activityPaused() {
        activityVisible = false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        boolean debuggable = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        // Debug builds get the SDK logs and the on-ad integration indicator; release builds stay
        // quiet and rely on the app's own "BirdaysAds" failure logs.
        YandexAds.enableLogging(debuggable);
        YandexAds.enableDebugErrorIndicator(debuggable);
        YandexAds.initialize(this, () -> Log.i(TAG, "Yandex Mobile Ads initialized: "
                + YandexAds.getLibraryVersion()));
    }

    /**
     * Best-effort cleanup of the repository pool. Not relied on in production — the pool threads
     * are daemons and every database handle is closed in try-with-resources — but it gives tests
     * and emulators a deterministic shutdown point.
     */
    @Override
    public void onTerminate() {
        BirdaysRepository.getInstance(this).shutdown();
        super.onTerminate();
    }
}
