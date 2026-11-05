/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.ad;

import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.view.OneShotPreDrawListener;

import com.djonique.birdays.BuildConfig;
import com.yandex.mobile.ads.banner.BannerAdEventListener;
import com.yandex.mobile.ads.banner.BannerAdSize;
import com.yandex.mobile.ads.banner.BannerAdView;
import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;

public class Ad {

    /**
     * Loads Yandex sticky banner into MainActivity
     */
    public static void showBannerAd(final ViewGroup viewGroup, final BannerAdView banner, final View view) {
        OneShotPreDrawListener.add(banner, () -> loadBanner(viewGroup, banner, view));
    }

    private static void loadBanner(ViewGroup viewGroup, BannerAdView banner, View view) {
        Context context = banner.getContext();
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int widthPixels = banner.getWidth();
        if (widthPixels <= 0) {
            widthPixels = metrics.widthPixels;
        }
        int widthDp = Math.round(widthPixels / metrics.density);

        BannerAdSize adSize = BannerAdSize.sticky(context, widthDp);
        banner.setAdSize(adSize);

        ViewGroup.LayoutParams params = banner.getLayoutParams();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = adSize.getHeightInPixels(context);
        banner.setLayoutParams(params);

        banner.setBannerAdEventListener(new BannerAdEventListener() {
            @Override
            public void onAdLoaded() {
                setupContentViewPadding(viewGroup, banner.getHeight());
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    setBottomMargin(view, -(banner.getHeight() / 4));
                }
            }

            @Override
            public void onAdFailedToLoad(@NonNull AdRequestError adRequestError) {
            }

            @Override
            public void onAdClicked() {
            }

            @Override
            public void onImpression(@NonNull ImpressionData impressionData) {
            }
        });

        banner.loadAd(new AdRequest.Builder(BuildConfig.BANNER_AD_UNIT_ID).build());
    }

    /**
     * Set up bottom padding of view under Yandex banner
     */
    private static void setupContentViewPadding(View view, int padding) {
        view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(),
                padding);
    }

    private static void setBottomMargin(View view, int margin) {
        if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            params.bottomMargin = margin;
            view.requestLayout();
        }
    }
}
