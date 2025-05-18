# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
############################################
# 🚁 DronePilotApp ProGuard Rules
############################################

# =============== Firebase (Crashlytics, Auth, Firestore, etc.) ===============
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# =============== Kotlin & Coroutines ===============
-keep class kotlin.Metadata { *; }
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# =============== Glide ===============
-keep class com.bumptech.glide.** { *; }
-dontwarn com.bumptech.glide.**
-keep interface com.bumptech.glide.module.GlideModule
-keep public class * implements com.bumptech.glide.module.GlideModule

# =============== OkHttp ===============
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# =============== JSoup ===============
-dontwarn org.jsoup.**
-keep class org.jsoup.** { *; }

# =============== Gson ===============
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# For Gson to keep model classes
-keepclassmembers class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# =============== Volley ===============
-dontwarn com.android.volley.**
-keep class com.android.volley.** { *; }

# =============== Maps & Play Services ===============
-keep class com.google.android.gms.maps.** { *; }
-dontwarn com.google.android.gms.maps.**

# =============== RecyclerView & ViewBinding ===============
-keep class androidx.recyclerview.widget.** { *; }
-keep class androidx.viewbinding.** { *; }

# =============== WebView JS Interface (facoltativo) ===============
#-keepclassmembers class com.kwos.dronepilotapp.MyWebInterface {
#    public *;
#}

# =============== Preserve line numbers for Crashlytics ===============
-keepattributes SourceFile,LineNumberTable

# Optional: hides source file name but preserves line numbers
#-renamesourcefileattribute SourceFile
