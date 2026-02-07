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

# Keep module classes - already handles our reflection needs
-keep class ltd.nextalone.**

# Xposed API
-keep class de.robv.android.xposed.** { *; }

# Android 16: Keep PackageInfo fields accessed via reflection
-keepclassmembers class android.content.pm.PackageInfo {
    public long longVersionCode;
    public java.lang.String versionName;
    public java.lang.String packageName;
}

# Android 16: Keep UserManager methods accessed via reflection
-keep class android.os.UserManager {
    public java.lang.String getUserName();
}
