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

# Add support for Kotlin coroutines
-dontwarn kotlinx.coroutines.**
-dontwarn kotlinx.coroutines.swing.**
-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** { *; }

# Keep Compose related classes
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }

# Keep Material Design 3 classes
-keep class com.google.android.material.** { *; }
-keepclassmembers class com.google.android.material.** { *; }

# Keep AndroidX classes
-keep class androidx.** { *; }
-keepclassmembers class androidx.** { *; }

# Keep your application classes
-keep class net.currit.tonality.** { *; }
-keepclassmembers class net.currit.tonality.** { *; }

# Keep the semitone module classes
-keep class mn.tck.semitone.** { *; }
-keepclassmembers class mn.tck.semitone.** { *; }
