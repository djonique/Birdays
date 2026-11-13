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
-keep class com.yandex.mobile.ads.** { <fields>; <methods>; }
-dontwarn com.yandex.mobile.ads.**

# Material DateTime Picker
-keep class com.wdullaer.materialdatetimepicker.** { *; }
-dontwarn com.wdullaer.materialdatetimepicker.**

