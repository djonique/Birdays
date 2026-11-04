/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.ad;

import android.os.Build;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public class Ad {

    /**
     * Loads AdMob banner into MainActivity
     */
    public static void showBannerAd(final ViewGroup viewGroup, final AdView banner, final View view) {
        banner.loadAd(new AdRequest.Builder().build());

        banner.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                setupContentViewPadding(viewGroup, banner.getHeight());
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    setBottomMargin(view, -(banner.getHeight() / 4));
                }
            }
        });
    }

    /**
     * Set up bottom padding of view under AdMob banner
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