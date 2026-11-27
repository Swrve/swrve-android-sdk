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

# Keep BigPictureStyle.bigPicture(Icon) overloads used via reflection so R8 doesn't strip/rename them. Temp workaround
-keepclassmembers class androidx.core.app.NotificationCompat$BigPictureStyle {
  public androidx.core.app.NotificationCompat$BigPictureStyle bigPicture(android.graphics.drawable.Icon);
}

# WorkManager creates its Room database and its InputMerger by reflection, using their no-arg constructors.
# R8's strict full mode (the default from Android Gradle Plugin 9) only keeps a constructor that a rule names,
# and the rules shipped with WorkManager 2.9.1 and Room 2.5 don't name it.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class * extends androidx.work.InputMerger { <init>(); }
