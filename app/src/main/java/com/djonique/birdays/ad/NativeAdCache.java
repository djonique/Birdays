/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.ad;

import android.content.Context;

import androidx.annotation.NonNull;

import com.djonique.birdays.BuildConfig;
import com.djonique.birdays.R;
import com.djonique.birdays.utils.BirdaysPreferencesManager;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.nativeads.NativeAd;
import com.yandex.mobile.ads.nativeads.NativeAdLoadListener;
import com.yandex.mobile.ads.nativeads.NativeAdLoader;

public class NativeAdCache {

    private static NativeAdCache instance;
    private NativeAd cachedNativeAd;
    private boolean loading;

    public static synchronized NativeAdCache getInstance() {
        if (instance == null) {
            instance = new NativeAdCache();
        }
        return instance;
    }

    public void loadAd(@NonNull Context context, @NonNull NativeAdLoadListener listener) {
        if (cachedNativeAd != null) {
            NativeAd ad = cachedNativeAd;
            cachedNativeAd = null;
            listener.onAdLoaded(ad);
            preload(context);
            return;
        }

        NativeAdLoader loader = new NativeAdLoader(context.getApplicationContext());
        loader.loadAd(new AdRequest.Builder(BuildConfig.NATIVE_AD_UNIT_ID).build(),
                new NativeAdLoadListener() {
                    @Override
                    public void onAdLoaded(@NonNull NativeAd nativeAd) {
                        listener.onAdLoaded(nativeAd);
                        preload(context);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull AdRequestError adRequestError) {
                        listener.onAdFailedToLoad(adRequestError);
                        preload(context);
                    }
                });
    }

    public void preload(@NonNull Context context) {
        if (!areAdsEnabled(context)) {
            // Ads were switched off: drop the cached copy and stop keeping the cache warm.
            cachedNativeAd = null;
            return;
        }
        if (cachedNativeAd != null || loading) return;
        loading = true;
        NativeAdLoader loader = new NativeAdLoader(context.getApplicationContext());
        loader.loadAd(new AdRequest.Builder(BuildConfig.NATIVE_AD_UNIT_ID).build(),
                new NativeAdLoadListener() {
                    @Override
                    public void onAdLoaded(@NonNull NativeAd nativeAd) {
                        loading = false;
                        // The preference can change while the request is in flight.
                        cachedNativeAd = areAdsEnabled(context) ? nativeAd : null;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull AdRequestError adRequestError) {
                        loading = false;
                    }
                });
    }

    private boolean areAdsEnabled(@NonNull Context context) {
        return BirdaysPreferencesManager.getDefaultSharedPreferences(context)
                .getBoolean(context.getString(R.string.show_ads_key), true);
    }
}
