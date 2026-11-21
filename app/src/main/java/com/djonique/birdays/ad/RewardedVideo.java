/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.ad;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Loads a rewarded ad in advance and shows it after an explicit user action.
 *
 * <p>The reward is granted only from {@link RewardCallback#onRewarded()}, never from show or
 * dismiss. The ad loader and the loaded ad are kept for the lifetime of the screen so the ad
 * survives configuration changes while it is on screen in its own activity.</p>
 */
public class RewardedVideo {

    private static final String TAG = "BirdaysAds";

    private final RewardedAdLoader rewardedAdLoader;

    @Nullable
    private RewardedAd rewardedAd;
    private boolean loading;
    private boolean showing;

    public RewardedVideo(@NonNull Context context) {
        rewardedAdLoader = new RewardedAdLoader(context.getApplicationContext());
    }

    public void preload() {
        if (loading || showing || rewardedAd != null) return;
        loading = true;
        rewardedAdLoader.loadAd(new AdRequest.Builder(BuildConfig.REWARDED_AD_UNIT_ID).build(),
                new RewardedAdLoadListener() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        loading = false;
                        rewardedAd = ad;
                        Log.d(TAG, "Rewarded ad loaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull AdRequestError adRequestError) {
                        loading = false;
                        // No retry here: the SDK documentation discourages reloading from the
                        // failure callback. The next screen visit or user tap loads again.
                        Log.w(TAG, "Rewarded ad failed to load: code=" + adRequestError.getCode()
                                + ", description=" + adRequestError.getDescription()
                                + ", adUnitId=" + adRequestError.getAdUnitId()
                                + ", configuredAdUnitId=" + BuildConfig.REWARDED_AD_UNIT_ID);
                    }
                });
    }

    public void show(@NonNull final Activity activity) {
        show(activity, null);
    }

    public void show(@NonNull final Activity activity, @Nullable final RewardCallback callback) {
        if (showing) return;

        if (rewardedAd == null) {
            Log.w(TAG, "Rewarded ad is not loaded yet, showing the unavailable message");
            Toast.makeText(activity, R.string.ad_rewarded_unavailable, Toast.LENGTH_SHORT).show();
            if (callback != null) {
                callback.onAdFailed();
            }
            preload();
            return;
        }

        final RewardedAd ad = rewardedAd;
        rewardedAd = null;
        showing = true;

        final AtomicBoolean terminalCallbackSent = new AtomicBoolean(false);
        final boolean[] rewardEarned = {false};

        ad.setAdEventListener(new RewardedAdEventListener() {
            @Override
            public void onAdShown() {
                Log.d(TAG, "Rewarded ad shown");
            }

            @Override
            public void onAdFailedToShow(@NonNull AdError adError) {
                if (terminalCallbackSent.compareAndSet(false, true)) {
                    Log.w(TAG, "Rewarded ad failed to show: " + adError.getDescription());
                    finish(ad, callback, false, rewardEarned[0]);
                }
            }

            @Override
            public void onAdDismissed() {
                if (terminalCallbackSent.compareAndSet(false, true)) {
                    Log.d(TAG, "Rewarded ad dismissed, reward earned=" + rewardEarned[0]);
                    finish(ad, callback, true, rewardEarned[0]);
                }
            }

            @Override
            public void onAdClicked() {
            }

            @Override
            public void onAdImpression(@Nullable ImpressionData impressionData) {
            }

            @Override
            public void onRewarded(@NonNull Reward reward) {
                // The reward is granted here and only here.
                rewardEarned[0] = true;
                if (callback != null) {
                    callback.onRewarded();
                }
            }
        });
        ad.show(activity);
    }

    private void finish(@NonNull RewardedAd ad, @Nullable RewardCallback callback,
                        boolean dismissed, boolean rewardEarned) {
        ad.setAdEventListener(null);
        showing = false;
        if (callback != null) {
            if (dismissed) {
                callback.onAdDismissed(rewardEarned);
            } else {
                callback.onAdFailed();
            }
        }
        preload();
    }

    public void destroy() {
        rewardedAdLoader.cancelLoading();
        if (rewardedAd != null) {
            rewardedAd.setAdEventListener(null);
            rewardedAd = null;
        }
        loading = false;
    }

    public interface RewardCallback {
        void onRewarded();

        void onAdDismissed(boolean rewardEarned);

        void onAdFailed();
    }
}
