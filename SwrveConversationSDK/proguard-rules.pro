# GSON proguard config
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.Unsafe
-keep class sun.misc.Unsafe { *; }
-keep class com.swrve.sdk.conversations.engine.model.** { *; }
