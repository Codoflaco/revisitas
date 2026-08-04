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
# Protege las clases de datos que usa GSON para el Backup
-keep class com.example.revisit.data.** { *; }

# Mantener atributos para depuración y GSON
-keepattributes Signature, EnclosingMethod, InnerClasses, *Annotation*

# Reglas para Room (aunque suele manejarse solo, es mejor asegurar)
-keep class androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}