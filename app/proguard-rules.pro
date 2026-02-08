# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line numbers for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# Keep all Xposed module classes and their members
-keep class ltd.nextalone.** { *; }

# Keep all inner classes explicitly
-keep class ltd.nextalone.**$* { *; }

# Xposed API - must be accessible via reflection
-keep class de.robv.android.xposed.** { *; }
-keep interface de.robv.android.xposed.** { *; }

# LSPosed needs to find IXposedHookLoadPackage and handleLoadPackage
-keep class * implements de.robv.android.xposed.IXposedHookLoadPackage {
    <methods>;
}

# LSPosed needs to find IXposedHookInitPackageResources
-keep class * implements de.robv.android.xposed.IXposedHookInitPackageResources {
    <methods>;
}

# Keep IXposedHookZygoteInit implementation
-keep class * implements de.robv.android.xposed.IXposedHookZygoteInit {
    <methods>;
}

# Android 16: Keep PackageInfo fields accessed via reflection
-keepclassmembers class android.content.pm.PackageInfo {
    public long longVersionCode;
    public java.lang.String versionName;
    public java.lang.String packageName;
    public android.content.pm.ApplicationInfo applicationInfo;
}

# Android 16: Keep UserManager methods accessed via reflection
-keep class android.os.UserManager {
    public java.lang.String getUserName();
    public android.os.UserHandle getUserForSerialNumber(long);
}

# Keep BuildConfig for debug checks
-keep class ltd.nextalone.pkginstallerplus.BuildConfig { *; }

# Prevent ProGuard from stripping generic signatures needed for reflection
-keepattributes Signature

# Keep annotations used by Xposed/LSPosed
-keepattributes *Annotation*

# Kotlin metadata (your codebase is 86% Kotlin)
-keep class kotlin.Metadata { *; }

# Prevent optimization that breaks Xposed hooking
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
