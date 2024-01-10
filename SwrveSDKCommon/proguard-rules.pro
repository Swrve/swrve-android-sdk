# GSON proguard config
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.swrve.sdk.notifications.model.** { *; }
# Additional rules for Gson, due to Gradle 8/R8 changes
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken
