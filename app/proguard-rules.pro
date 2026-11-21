#
# Copyright 2017-2026 Evgenii Timofeev
#
# SPDX-License-Identifier: Apache-2.0
#

-keepattributes SourceFile,LineNumberTable,Signature,*Annotation*

# Birdays Models
-keep class com.djonique.birdays.models.** { *; }

# Google API Client & Drive API
# Preserve fields annotated with @Key for JSON parsing via reflection
-keepclassmembers class * {
    @com.google.api.client.util.Key <fields>;
}
-dontwarn com.google.api.client.**
-dontwarn com.google.api.services.drive.**
-dontwarn javax.naming.**
-dontwarn org.ietf.jgss.**
-dontwarn org.apache.http.**

# Yandex Mobile Ads SDK: built-in consumer rules in com.yandex.android:mobileads AAR
# handle internal and mediation requirements automatically.

