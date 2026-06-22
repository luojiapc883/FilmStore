# FilmStore ProGuard Rules

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.filmstore.tv.model.** { *; }
-keep class com.filmstore.tv.api.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class com.bumptech.glide.** { *; }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Retrofit / OkHttp
-keepattributes Exceptions,InnerClasses,EnclosingMethod
