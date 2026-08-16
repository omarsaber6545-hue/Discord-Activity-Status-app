# Add project specific ProGuard rules here.
-keep class com.omardev.discordactivity.data.models.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
