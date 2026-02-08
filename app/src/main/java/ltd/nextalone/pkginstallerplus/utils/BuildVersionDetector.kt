package ltd.nextalone.pkginstallerplus.utils

import android.os.Build

/**
 * Utility class for detecting Android build versions and variants.
 * Provides specific detection for Android 16 QPR builds (Baklava variants).
 */
object BuildVersionDetector {
    
    /**
     * Android 16 Build ID patterns:
     * - QPR1: BD* format (e.g., BD1A.230803.001)
     * - QPR2: BP*, CD*, CP* formats
     * - Stable: AP* or similar patterns
     */
    private val ANDROID_16_BUILD_PREFIXES = arrayOf(
        "AP",  // Android 16 stable builds
        "BD",  // Android 16 QPR1 builds
        "BP",  // Android 16 QPR2 builds  
        "CD",  // Android 16 QPR2 builds (alternate)
        "CP",  // Android 16 QPR2 builds (alternate)
        "DP",  // Android 16 developer preview (if any)
    )
    
    /**
     * Checks if the current device is running Android 16 (API 36).
     * This includes all variants: stable, QPR1, QPR2, etc.
     * 
     * @return true if running Android 16, false otherwise
     */
    fun isAndroid16(): Boolean {
        return Build.VERSION.SDK_INT >= 36
    }
    
    /**
     * Checks if the current device is running Android 16 by checking both
     * SDK_INT and Build.ID patterns.
     * 
     * @return true if running Android 16 (verified by build ID), false otherwise
     */
    fun isAndroid16Verified(): Boolean {
        if (!isAndroid16()) return false
        
        val buildId = Build.ID ?: return false
        return ANDROID_16_BUILD_PREFIXES.any { buildId.startsWith(it, ignoreCase = true) }
    }
    
    /**
     * Gets the Android 16 variant based on Build.ID.
     * 
     * @return Variant name (e.g., "QPR1", "QPR2", "Stable") or null if not Android 16
     */
    fun getAndroid16Variant(): String? {
        if (!isAndroid16()) return null
        
        val buildId = Build.ID ?: return "Unknown"
        
        return when {
            buildId.startsWith("BD", ignoreCase = true) -> "QPR1"
            buildId.startsWith("BP", ignoreCase = true) -> "QPR2"
            buildId.startsWith("CD", ignoreCase = true) -> "QPR2"
            buildId.startsWith("CP", ignoreCase = true) -> "QPR2"
            buildId.startsWith("DP", ignoreCase = true) -> "Developer Preview"
            buildId.startsWith("AP", ignoreCase = true) -> "Stable"
            else -> "Unknown Variant"
        }
    }
    
    /**
     * Checks if the current Android version is 15 or higher (Baklava and later).
     * These versions use the v2 Package Installer architecture.
     * 
     * @return true if Android 15+, false otherwise
     */
    fun isAndroid15OrHigher(): Boolean {
        return Build.VERSION.SDK_INT >= 35
    }
    
    /**
     * Gets a detailed version string for logging and debugging.
     * 
     * @return Formatted version string with SDK, Build ID, and variant info
     */
    fun getDetailedVersionString(): String {
        val variant = getAndroid16Variant()
        val variantStr = if (variant != null) " ($variant)" else ""
        return "Android ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})$variantStr - Build: ${Build.ID}"
    }
}
