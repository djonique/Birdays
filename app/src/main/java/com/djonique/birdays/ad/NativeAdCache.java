/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.ad;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.djonique.birdays.BuildConfig;
import com.djonique.birdays.R;
import com.djonique.birdays.utils.BirdaysPreferencesManager;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.nativeads.NativeAd;
import com.yandex.mobile.ads.nativeads.NativeAdLoadListener;
import com.yandex.mobile.ads.nativeads.NativeAdLoader;

/**
 * Keeps at most one native ad loaded ahead of the screen that is going to display it.
 *
 * <p>Every loaded {@link NativeAd} is bound to exactly one ad view, so a cached ad is handed out
 * once. A preloaded ad that was not used in time is dropped: Yandex ads expire and an expired ad
 * can no longer be rendered, which used to leave the ad row silently empty.</p>
 */
public final class NativeAdCache {

    private static final String TAG = "BirdaysAds";

    /**
     * A preloaded ad is shown only while it is reasonably fresh; after this the SDK may have
     * invalidated the creative and binding/showing it would fail.
     */
    private static final long MAX_CACHE_AGE_MS = 15L * 60L * 1000L;

    private static NativeAdCache instance;

    private NativeAd cachedNativeAd;
    private long cachedAt;
    private boolean loading;

    private NativeAdCache() {
    }

    public static synchronized NativeAdCache getInstance() {
        if (instance == null) {
            instance = new NativeAdCache();
        }
        return instance;
    }

    /**
     * Delivers the preloaded ad when it is still fresh, otherwise requests a new one.
     */
    public void loadAd(@NonNull Context context, @NonNull NativeAdLoadListener listener) {
        NativeAd cachedAd = takeCachedAd();
        if (cachedAd != null) {
            listener.onAdLoaded(cachedAd);
            return;
        }
        requestAd(context, listener);
    }

    /**
     * Loads an ad for a later {@link #loadAd} call. Failures are only logged: the documentation
     * discourages retrying right away from the failure callback, so the next natural trigger (the
     * screen being shown again) requests the ad again.
     */
    public void preload(@NonNull Context context) {
        if (!areAdsEnabled(context)) {
            // Ads were switched off: drop the cached copy and stop keeping the cache warm.
            cachedNativeAd = null;
            return;
        }
        if (cachedNativeAd != null) {
            if (System.currentTimeMillis() - cachedAt <= MAX_CACHE_AGE_MS) {
                return;
            }
            Log.w(TAG, "Dropping stale preloaded native ad");
            cachedNativeAd = null;
        }
        if (loading) return;

        loading = true;
        new NativeAdLoader(context.getApplicationContext()).loadAd(
                new AdRequest.Builder(BuildConfig.NATIVE_AD_UNIT_ID).build(),
                new NativeAdLoadListener() {
                    @Override
                    public void onAdLoaded(@NonNull NativeAd nativeAd) {
                        loading = false;
                        // The preference can change while the request is in flight.
                        if (areAdsEnabled(context)) {
                            cachedNativeAd = nativeAd;
                            cachedAt = System.currentTimeMillis();
                        }
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull AdRequestError adRequestError) {
                        loading = false;
                        logFailure("preload", adRequestError);
                    }
                });
    }

    private void requestAd(@NonNull Context context, @NonNull NativeAdLoadListener listener) {
        new NativeAdLoader(context.getApplicationContext()).loadAd(
                new AdRequest.Builder(BuildConfig.NATIVE_AD_UNIT_ID).build(),
                new NativeAdLoadListener() {
                    @Override
                    public void onAdLoaded(@NonNull NativeAd nativeAd) {
                        listener.onAdLoaded(nativeAd);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull AdRequestError adRequestError) {
                        logFailure("load", adRequestError);
                        listener.onAdFailedToLoad(adRequestError);
                    }
                });
    }

    private NativeAd takeCachedAd() {
        if (cachedNativeAd == null) return null;

        NativeAd ad = cachedNativeAd;
        cachedNativeAd = null;
        if (System.currentTimeMillis() - cachedAt > MAX_CACHE_AGE_MS) {
            Log.w(TAG, "Discarding stale preloaded native ad");
            return null;
        }
        return ad;
    }

    private void logFailure(@NonNull String operation, @NonNull AdRequestError error) {
        // Kept out of BuildConfig.DEBUG on purpose: without it a release build with a wrong or
        // disabled ad unit silently renders no ads and there is nothing to look at in logcat.
        Log.w(TAG, "Native ad " + operation + " failed: code=" + error.getCode()
                + ", description=" + error.getDescription()
                + ", adUnitId=" + error.getAdUnitId()
                + ", configuredAdUnitId=" + BuildConfig.NATIVE_AD_UNIT_ID);
    }

    private boolean areAdsEnabled(@NonNull Context context) {
        return BirdaysPreferencesManager.getDefaultSharedPreferences(context)
                .getBoolean(context.getString(R.string.show_ads_key), true);
    }
}
