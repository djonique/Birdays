/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.ad;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.djonique.birdays.BuildConfig;
import com.djonique.birdays.R;
import com.yandex.mobile.ads.common.AdError;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.rewarded.Reward;
import com.yandex.mobile.ads.rewarded.RewardedAd;
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener;
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener;
import com.yandex.mobile.ads.rewarded.RewardedAdLoader;

/**
 * Loads and shows Yandex rewarded video. The app does not grant any reward:
 * watching the video only supports the project.
 */
public class RewardedVideo {

    private final RewardedAdLoader rewardedAdLoader;
    private RewardedAd rewardedAd;
    private boolean loading;

    public RewardedVideo(@NonNull Context context) {
        rewardedAdLoader = new RewardedAdLoader(context);
    }

    public void preload() {
        if (loading) return;
        loading = true;
        rewardedAdLoader.loadAd(new AdRequest.Builder(BuildConfig.REWARDED_AD_UNIT_ID).build(),
                new RewardedAdLoadListener() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        loading = false;
                        rewardedAd = ad;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull AdRequestError adRequestError) {
                        loading = false;
                    }
                });
    }

    public void show(@NonNull final Activity activity) {
        if (rewardedAd == null) {
            Toast.makeText(activity, R.string.ad_rewarded_unavailable, Toast.LENGTH_SHORT).show();
            preload();
            return;
        }

        RewardedAd ad = rewardedAd;
        rewardedAd = null;
        ad.setAdEventListener(new RewardedAdEventListener() {
            @Override
            public void onAdShown() {
            }

            @Override
            public void onAdFailedToShow(@NonNull AdError adError) {
                preload();
            }

            @Override
            public void onAdDismissed() {
                preload();
            }

            @Override
            public void onAdClicked() {
            }

            @Override
            public void onAdImpression(@NonNull ImpressionData impressionData) {
            }

            @Override
            public void onRewarded(@NonNull Reward reward) {
            }
        });
        ad.show(activity);
    }

    public void destroy() {
        rewardedAdLoader.cancelLoading();
        if (rewardedAd != null) {
            rewardedAd.setAdEventListener(null);
            rewardedAd = null;
        }
    }
}
