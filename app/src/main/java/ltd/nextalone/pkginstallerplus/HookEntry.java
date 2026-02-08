package ltd.nextalone.pkginstallerplus;

import android.annotation.SuppressLint;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import java.io.File;
import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import ltd.nextalone.pkginstallerplus.hook.InstallerHookBaklava;
import ltd.nextalone.pkginstallerplus.hook.InstallerHookN;
import ltd.nextalone.pkginstallerplus.hook.InstallerHookQ;
import ltd.nextalone.pkginstallerplus.utils.BuildVersionDetector;

import static ltd.nextalone.pkginstallerplus.utils.HookUtilsKt.isV2InstallerAvailable;
import static ltd.nextalone.pkginstallerplus.utils.LogUtilsKt.logDebug;
import static ltd.nextalone.pkginstallerplus.utils.LogUtilsKt.logDetail;
import static ltd.nextalone.pkginstallerplus.utils.LogUtilsKt.logError;
import static ltd.nextalone.pkginstallerplus.utils.LogUtilsKt.logThrowable;

public class HookEntry implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    public static ClassLoader myClassLoader;
    public static ClassLoader lpClassLoader;
    private static boolean sInitialized = false;
    private static String sModulePath = null;

    private static void initializeHookInternal(LoadPackageParam lpparam) {
        logDebug("initializeHookInternal start");
        lpClassLoader = lpparam.classLoader;
        
        // Log detailed Android version for debugging
        logDebug("Android version: " + BuildVersionDetector.INSTANCE.getDetailedVersionString());
        
        // Android 15+ uses v2 installer (Baklava codename)
        // This includes Android 16 with all QPR variants
        if (isV2InstallerAvailable()) {
            try {
                if (BuildVersionDetector.INSTANCE.isAndroid16()) {
                    String variant = BuildVersionDetector.INSTANCE.getAndroid16Variant();
                    logDebug("initializeHook: Android 16 " + (variant != null ? variant : "") + " detected");
                } else {
                    logDebug("initializeHook: Baklava (Android 15)");
                }
                InstallerHookBaklava.INSTANCE.initOnce();
            } catch (Exception e) {
                logThrowable("initializeHook(Baklava): ", e);
            }
        }
        
        // Fallback for Android 10-14 (Q/R/S/T)
        // Only attempt if not Android 15+ to avoid duplicate hooks
        if (!BuildVersionDetector.INSTANCE.isAndroid15OrHigher()) {
            try {
                if (VERSION.SDK_INT >= VERSION_CODES.Q && VERSION.SDK_INT < 35) {
                    logDebug("initializeHook: Q (Android 10-14)");
                    InstallerHookQ.INSTANCE.initOnce();
                } else if (VERSION.SDK_INT < VERSION_CODES.Q) {
                    // Android 7-9 (Nougat/Oreo/Pie)
                    throw new Exception("Pre-Q Android version, use Nougat hooks");
                }
            } catch (Exception e) {
                try {
                    // Android Nougat fallback (Android 7-9)
                    logDebug("initializeHook: N (Android 7-9)");
                    InstallerHookN.INSTANCE.initOnce();
                } catch (Exception e1) {
                    e.addSuppressed(e1);
                    logThrowable("initializeHookInternal: ", e);
                }
            }
        }
    }

    public static void injectModuleResources(Resources res) {
        if (res == null) {
            return;
        }
        // Quick check: if already injected, return early
        try {
            res.getString(R.string.IPP_res_inject_success);
            return;
        } catch (Resources.NotFoundException ignored) {
        }
        
        // Perform resource injection with minimal logging for stealth
        try {
            if (myClassLoader == null) {
                myClassLoader = HookEntry.class.getClassLoader();
            }
            if (sModulePath == null) {
                return; // Silently fail to avoid detection
            }
            
            AssetManager assets = res.getAssets();
            @SuppressLint("DiscouragedPrivateApi")
            Method addAssetPath = AssetManager.class.getDeclaredMethod("addAssetPath", String.class);
            addAssetPath.setAccessible(true);
            addAssetPath.invoke(assets, sModulePath);
            
            // Verify injection worked (silent check)
            try {
                res.getString(R.string.IPP_res_inject_success);
            } catch (Resources.NotFoundException e) {
                // Silent failure - avoid logging that reveals module presence
            }
        } catch (Exception e) {
            // Silent failure - avoid logging that reveals module internals
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // Only hook package installer apps - minimal logging for stealth
        if ("com.google.android.packageinstaller".equals(lpparam.packageName)
            || "com.android.packageinstaller".equals(lpparam.packageName)) {
            if (!sInitialized) {
                sInitialized = true;
                initializeHookInternal(lpparam);
            }
        }
    }

    @Override
    public void initZygote(StartupParam startupParam) {
        String modulePath = startupParam.modulePath;
        assert modulePath != null;
        sModulePath = modulePath;
    }
}
