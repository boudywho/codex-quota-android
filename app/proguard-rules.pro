# ProGuard rules for Codex Quota

# Keep Kotlinx serialization models
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Room generated classes
-keep class androidx.room.** { *; }

# Keep Glance AppWidget classes
-keep class androidx.glance.** { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }

# Keep OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
