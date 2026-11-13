/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.ad;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.djonique.birdays.R;
import com.yandex.mobile.ads.common.AdBindingResult;
import com.yandex.mobile.ads.nativeads.MediaView;
import com.yandex.mobile.ads.nativeads.NativeAd;
import com.yandex.mobile.ads.nativeads.NativeAdView;
import com.yandex.mobile.ads.nativeads.NativeAdViewBinder;

public class NativeAdHelper {

    private static final String TAG = "NativeAdHelper";

    public static void bindNativeAd(@NonNull NativeAdView nativeAdView, @NonNull NativeAd nativeAd) {
        TextView titleView = nativeAdView.findViewById(R.id.native_ad_title);
        TextView bodyView = nativeAdView.findViewById(R.id.native_ad_body);
        ImageView iconView = nativeAdView.findViewById(R.id.native_ad_icon);
        TextView domainView = nativeAdView.findViewById(R.id.native_ad_domain);
        TextView sponsoredView = nativeAdView.findViewById(R.id.native_ad_sponsored);
        TextView ageView = nativeAdView.findViewById(R.id.native_ad_age);
        TextView warningView = nativeAdView.findViewById(R.id.native_ad_warning);
        MediaView mediaView = nativeAdView.findViewById(R.id.native_ad_media);
        TextView callToActionView = nativeAdView.findViewById(R.id.native_ad_call_to_action);
        ImageView feedbackView = nativeAdView.findViewById(R.id.native_ad_feedback);

        NativeAdViewBinder.Builder binderBuilder = new NativeAdViewBinder.Builder(nativeAdView);

        if (titleView != null) binderBuilder.setTitleView(titleView);
        if (bodyView != null) binderBuilder.setBodyView(bodyView);
        if (iconView != null) binderBuilder.setIconView(iconView);
        if (domainView != null) binderBuilder.setDomainView(domainView);
        if (sponsoredView != null) binderBuilder.setSponsoredView(sponsoredView);
        if (ageView != null) binderBuilder.setAgeView(ageView);
        if (warningView != null) binderBuilder.setWarningView(warningView);
        if (mediaView != null) binderBuilder.setMediaView(mediaView);
        if (callToActionView != null) binderBuilder.setCallToActionView(callToActionView);
        if (feedbackView != null) binderBuilder.setFeedbackView(feedbackView);

        try {
            NativeAdViewBinder binder = binderBuilder.build();
            AdBindingResult result = nativeAd.bindNativeAd(binder);
            nativeAdView.setVisibility(View.VISIBLE);
            if (result instanceof AdBindingResult.Failure failure) {
                Log.e(TAG, "Ad binding failure for asset: " + failure.getMissingAssetName(), failure.getException());
            } else {
                Log.d(TAG, "Ad binding succeeded");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to bind native ad", e);
            nativeAdView.setVisibility(View.VISIBLE);
        }
    }
}
