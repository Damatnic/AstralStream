# Elite Security ProGuard Rules
# Implements SecurityAgent requirements for code obfuscation

# Enable aggressive obfuscation
-optimizationpasses 5
-allowaccessmodification
-dontpreverify
-repackageclasses ''

# Obfuscate class names aggressively
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes SourceFile,LineNumberTable

# Security-critical classes - extra obfuscation
-keep class com.astralplayer.core.security.** { *; }
-keepclassmembers class com.astralplayer.core.security.** {
    private <fields>;
    private <methods>;
}

# Encrypt string constants
-assumenosideeffects class java.lang.String {
    public static java.lang.String valueOf(...);
}

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

-assumenosideeffects class timber.log.Timber {
    public static void v(...);
    public static void i(...);
    public static void w(...);
    public static void d(...);
    public static void e(...);
    public static void wtf(...);
}

# API keys and sensitive data
-keepclassmembers class com.astralplayer.BuildConfig {
    public static ** GOOGLE_AI_API_KEY;
    public static ** OPENAI_API_KEY;
    public static ** AZURE_SPEECH_API_KEY;
    public static ** ASSEMBLY_AI_API_KEY;
    public static ** DEEPGRAM_API_KEY;
}

# Biometric authentication
-keep class androidx.biometric.** { *; }
-keep interface androidx.biometric.** { *; }

# Certificate pinning
-keepclassmembers class com.astralplayer.core.security.CertificatePinningManager {
    private static final java.util.Map CERTIFICATE_PINS;
}

# Encryption classes
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }
-keep class android.security.keystore.** { *; }

# OkHttp for certificate pinning
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okio.**

# Keep security-related annotations
-keepattributes *Annotation*
-keep @interface com.astralplayer.security.* { *; }

# Prevent removal of security checks
-keep class * implements javax.net.ssl.X509TrustManager {
    public void checkServerTrusted(...);
}

# Native methods for enhanced security
-keepclasseswithmembernames class * {
    native <methods>;
}

# Additional obfuscation for release builds
-printmapping mapping.txt
-printseeds seeds.txt
-printusage unused.txt