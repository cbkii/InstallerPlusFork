# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Prevent ProGuard from stripping generic signatures and keep annotations
-keepattributes Signature,SourceFile,LineNumberTable,*Annotation*

# Keep only the Xposed entrypoint declared in xposed_init
# This is the only class LSPosed needs to find by name
-keep class ltd.nextalone.pkginstallerplus.HookEntry { *; }

# Keep BuildConfig for version checks
-keep class ltd.nextalone.pkginstallerplus.BuildConfig { *; }

# Xposed API - must be accessible via reflection
-keep class de.robv.android.xposed.** { *; }
-keep interface de.robv.android.xposed.** { *; }

# LSPosed needs to find IXposedHookLoadPackage and handleLoadPackage
-keep class * implements de.robv.android.xposed.IXposedHookLoadPackage {
    *;
}

# LSPosed needs to find IXposedHookInitPackageResources
-keep class * implements de.robv.android.xposed.IXposedHookInitPackageResources {
    *;
}

# Keep IXposedHookZygoteInit implementation
-keep class * implements de.robv.android.xposed.IXposedHookZygoteInit {
    *;
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

# Kotlin metadata (your codebase is 86% Kotlin)
-keep class kotlin.Metadata { *; }

# Prevent optimization that breaks Xposed hooking:
# - arithmetic/cast simplifications can break hook method signatures
# - field optimizations can prevent reflection-based field access
# - class merging can make LSPosed unable to locate hook classes reflectively
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
