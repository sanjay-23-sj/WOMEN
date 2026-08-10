# SanX — ProGuard Rules

# Kotlin / Coroutines
-keep class kotlin.** { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings { <fields>; }

# Jetpack Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Room Database — keep all entity, DAO, and database classes
-keep class com.sanx.app.data.local.** { *; }
-keepclassmembers class com.sanx.app.data.local.** { *; }

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# CameraX
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# BLE
-keep class android.bluetooth.** { *; }

# SanX models — preserve all data classes
-keep class com.sanx.app.data.model.** { *; }
-keep class com.sanx.app.data.local.entity.** { *; }
