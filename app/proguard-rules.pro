#
# Copyright 2017-2026 Evgenii Timofeev
#
# SPDX-License-Identifier: Apache-2.0
#

-keepattributes SourceFile,LineNumberTable,Signature,*Annotation*

# Birdays Models
-keep class com.djonique.birdays.models.** { *; }

# Firebase Crashlytics
-keep class com.crashlytics.** { *; }
-dontwarn com.crashlytics.**

# Google API Client & Drive API
-keep class com.google.api.services.drive.model.** { <fields>; <methods>; }
-keep class com.google.api.client.json.** { <fields>; <methods>; }
-dontwarn com.google.api.client.**

# Yandex Mobile Ads SDK
# Keep only the public packages the app references; the SDK ships its own consumer rules for
# its internal and mediation code, which must not be pinned here.
-keep class com.yandex.mobile.ads.common.** { *; }
-keep class com.yandex.mobile.ads.banner.** { *; }
-keep class com.yandex.mobile.ads.interstitial.** { *; }
-keep class com.yandex.mobile.ads.rewarded.** { *; }
-keep class com.yandex.mobile.ads.nativeads.** { *; }
-dontwarn com.yandex.mobile.ads.**

