/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.ad;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.djonique.birdays.R;
import com.djonique.birdays.views.RatingView;
import com.yandex.mobile.ads.common.AdBindingResult;
import com.yandex.mobile.ads.nativeads.MediaView;
import com.yandex.mobile.ads.nativeads.NativeAd;
import com.yandex.mobile.ads.nativeads.NativeAdAssets;
import com.yandex.mobile.ads.nativeads.NativeAdView;
import com.yandex.mobile.ads.nativeads.NativeAdViewBinder;

public final class NativeAdHelper {

    private static final String TAG = "BirdaysAds";

    private NativeAdHelper() {
    }

    /**
     * Creates a binder that maps every asset the SDK can deliver to a view of the ad layout.
     *
     * <p>The SDK fails binding when the ad contains an asset for which no view was supplied, so the
     * binder must know every view even if a particular ad does not use all of them. A binder is
     * tied to the views it was built from, so it is created once per ad view holder and reused.</p>
     */
    @NonNull
    public static NativeAdViewBinder createBinder(@NonNull NativeAdView nativeAdView) {
        TextView titleView = nativeAdView.findViewById(R.id.native_ad_title);
        TextView bodyView = nativeAdView.findViewById(R.id.native_ad_body);
        ImageView iconView = nativeAdView.findViewById(R.id.native_ad_icon);
        ImageView faviconView = nativeAdView.findViewById(R.id.native_ad_favicon);
        TextView domainView = nativeAdView.findViewById(R.id.native_ad_domain);
        TextView sponsoredView = nativeAdView.findViewById(R.id.native_ad_sponsored);
        TextView ageView = nativeAdView.findViewById(R.id.native_ad_age);
        TextView warningView = nativeAdView.findViewById(R.id.native_ad_warning);
        MediaView mediaView = nativeAdView.findViewById(R.id.native_ad_media);
        TextView priceView = nativeAdView.findViewById(R.id.native_ad_price);
        RatingView ratingView = nativeAdView.findViewById(R.id.native_ad_rating);
        TextView reviewCountView = nativeAdView.findViewById(R.id.native_ad_review_count);
        TextView callToActionView = nativeAdView.findViewById(R.id.native_ad_call_to_action);
        ImageView feedbackView = nativeAdView.findViewById(R.id.native_ad_feedback);

        NativeAdViewBinder.Builder binderBuilder = new NativeAdViewBinder.Builder(nativeAdView);

        if (titleView != null) binderBuilder.setTitleView(titleView);
        if (bodyView != null) binderBuilder.setBodyView(bodyView);
        if (iconView != null) binderBuilder.setIconView(iconView);
        if (faviconView != null) binderBuilder.setFaviconView(faviconView);
        if (domainView != null) binderBuilder.setDomainView(domainView);
        if (sponsoredView != null) binderBuilder.setSponsoredView(sponsoredView);
        if (ageView != null) binderBuilder.setAgeView(ageView);
        if (warningView != null) binderBuilder.setWarningView(warningView);
        if (mediaView != null) binderBuilder.setMediaView(mediaView);
        if (priceView != null) binderBuilder.setPriceView(priceView);
        if (ratingView != null) binderBuilder.setRatingView(ratingView);
        if (reviewCountView != null) binderBuilder.setReviewCountView(reviewCountView);
        if (callToActionView != null) binderBuilder.setCallToActionView(callToActionView);
        if (feedbackView != null) binderBuilder.setFeedbackView(feedbackView);

        return binderBuilder.build();
    }

    /**
     * Binds the ad assets to the view.
     *
     * @return {@code true} when the ad was bound and the view can be shown, {@code false} when
     * binding failed or the SDK reported a missing asset.
     */
    public static boolean bindNativeAd(@NonNull NativeAdView nativeAdView, @NonNull NativeAd nativeAd) {
        return bindNativeAd(nativeAdView, createBinder(nativeAdView), nativeAd);
    }

    /**
     * Binds the ad assets to the view using a binder that was created in advance.
     */
    public static boolean bindNativeAd(@NonNull NativeAdView nativeAdView,
                                       @NonNull NativeAdViewBinder binder,
                                       @NonNull NativeAd nativeAd) {
        // Optional assets are hidden only when the current ad does not carry them; a view that is
        // hidden while its asset exists would not count as an impression.
        NativeAdAssets assets = nativeAd.getAdAssets();
        setVisible(nativeAdView.findViewById(R.id.native_ad_icon), assets.getIcon() != null);
        setVisible(nativeAdView.findViewById(R.id.native_ad_favicon), assets.getFavicon() != null);
        setVisible(nativeAdView.findViewById(R.id.native_ad_price), !TextUtils.isEmpty(assets.getPrice()));
        setVisible(nativeAdView.findViewById(R.id.native_ad_rating), assets.getRating() != null);
        setVisible(nativeAdView.findViewById(R.id.native_ad_review_count),
                !TextUtils.isEmpty(assets.getReviewCount()));

        try {
            AdBindingResult result = nativeAd.bindNativeAd(binder);
            if (result instanceof AdBindingResult.Failure failure) {
                Log.e(TAG, "Native ad binding failed, missing asset: "
                        + failure.getMissingAssetName(), failure.getException());
                return false;
            }
            Log.d(TAG, "Native ad bound");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Native ad binding threw", e);
            return false;
        }
    }

    private static void setVisible(View view, boolean visible) {
        if (view != null) {
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
}
