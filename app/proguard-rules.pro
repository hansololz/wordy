# Compose, Room, Retrofit, OkHttp and kotlinx.serialization all ship their own consumer rules.

# Keep line numbers so crash reports from release builds can be retraced.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# A compile-time-only annotation that Play's review-ktx refers to but does not ship.
-dontwarn com.google.android.gms.common.annotation.NoNullnessRewrite
