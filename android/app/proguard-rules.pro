# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Preserve line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# Hide the original source file name
-renamesourcefileattribute SourceFile

# Keep generic type information
-keepattributes Signature
-keepattributes *Annotation*

# Preserve runtime visible annotations
-keepattributes RuntimeVisibleAnnotations

# Kotlin
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Jetpack Compose
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }

# Keep custom Application class
-keep class com.astralplayer.nextplayer.AstralStreamApplication { *; }
-keep class com.astralplayer.nextplayer.AstralVuApplication { *; }

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ExoPlayer/Media3
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-keep class com.google.android.exoplayer2.** { *; }
-keepclassmembers class com.google.android.exoplayer2.** {
    <fields>;
    <methods>;
}
-dontwarn com.google.android.exoplayer2.**
-dontwarn androidx.media3.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel
-keep @dagger.hilt.android.qualifiers.ApplicationContext class * { *; }
-keep @dagger.Module class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.* <methods>;
}
-keepclasseswithmembers class * {
    @javax.inject.* <methods>;
}

# Room
-keep class androidx.room.** { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <fields>;
    <methods>;
}
-dontwarn androidx.room.paging.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep data classes
-keep class com.astralplayer.nextplayer.VideoMetadata { *; }
-keep class com.astralplayer.nextplayer.VideoPlaylist { *; }
-keep class com.astralplayer.nextplayer.PlaybackSettings { *; }
-keep class com.astralplayer.nextplayer.SubtitleTrack { *; }
-keep class com.astralplayer.nextplayer.VideoFolder { *; }
-keep class com.astralplayer.nextplayer.data.SettingsDataStore$Settings { *; }

# Keep all models and data classes
-keep class com.astralplayer.nextplayer.**.*Model { *; }
-keep class com.astralplayer.nextplayer.**.*State { *; }
-keep class com.astralplayer.nextplayer.**.*Info { *; }

# WebView
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Retrofit (if used in future)
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# OkHttp (if used in future)
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Keep Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-dontwarn org.codehaus.mojo.animal_sniffer.*
-dontwarn okhttp3.internal.platform.ConscryptPlatform

# Google Drive API
-keep class com.google.api.** { *; }
-keep class com.google.apis.** { *; }
-dontwarn com.google.api.**
-dontwarn com.google.apis.**

# Application specific
-keep class com.astralplayer.nextplayer.data.model.** { *; }
-keep class com.astralplayer.nextplayer.data.database.entity.** { *; }
-keep class com.astralplayer.nextplayer.utils.IntentUtils { *; }
-keep class com.astralplayer.nextplayer.utils.CodecManager { *; }

# Keep all activities
-keep class * extends android.app.Activity
-keep class * extends androidx.activity.ComponentActivity

# AndroidX
-keep class androidx.** { *; }
-dontwarn androidx.**

# Debugging - remove in production
-keepattributes LocalVariableTable
-keepattributes LocalVariableTypeTable

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}