/*
 * Copyright 2017-2026 Evgenii Timofeev
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.djonique.birdays.views;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatRatingBar;

import com.yandex.mobile.ads.nativeads.Rating;

/**
 * Rating bar that the Yandex Mobile Ads SDK can fill with a native ad's star rating.
 *
 * <p>{@link com.yandex.mobile.ads.nativeads.NativeAdViewBinder.Builder#setRatingView} only accepts a
 * {@code View} that also implements {@link Rating}; a plain {@code RatingBar} cannot be used.</p>
 */
public class RatingView extends AppCompatRatingBar implements Rating {

    public RatingView(@NonNull Context context) {
        super(context);
    }

    public RatingView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RatingView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
