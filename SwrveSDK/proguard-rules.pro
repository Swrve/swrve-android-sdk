# GSON proguard config
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.swrve.sdk.messaging.model.** { *; }
