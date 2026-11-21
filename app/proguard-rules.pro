#
# Copyright 2017-2026 Evgenii Timofeev
#
# SPDX-License-Identifier: Apache-2.0
#

-keepattributes SourceFile,LineNumberTable,Signature,*Annotation*

# Birdays Models
-keep class com.djonique.birdays.models.** { *; }

# Google API Client, Drive API & Auth Library
-keepattributes Signature,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault

-keep class com.google.api.client.googleapis.** { *; }
-keep class com.google.api.client.http.** { *; }
-keep class com.google.api.client.json.** { *; }
-keep class com.google.api.client.util.** { *; }
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.auth.** { *; }

-keepclassmembers class * {
    @com.google.api.client.util.Key <fields>;
}

-keepclassmembers class * extends com.google.api.client.util.GenericData {
    <fields>;
    <methods>;
}

-keepclassmembers class * extends com.google.api.client.googleapis.services.AbstractGoogleClientRequest {
    <fields>;
    <methods>;
}

-dontwarn com.google.api.client.util.escape.**
-dontwarn com.google.api.client.repackaged.**
-dontwarn com.google.auth.**
-dontwarn javax.naming.**
-dontwarn org.ietf.jgss.**
-dontwarn org.apache.http.**

# Yandex Mobile Ads SDK: built-in consumer rules in com.yandex.android:mobileads AAR
# handle internal and mediation requirements automatically.
