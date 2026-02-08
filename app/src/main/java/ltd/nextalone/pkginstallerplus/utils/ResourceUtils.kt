package ltd.nextalone.pkginstallerplus.utils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.view.View

/**
 * Get resource ID from host package, trying actual package first then fallbacks.
 * This supports both com.google.android.packageinstaller and com.android.packageinstaller.
 */
internal fun Context.hostId(name: String): Int {
    // Try actual host package first
    var id = resources.getIdentifier(name, "id", packageName)
    if (id != 0) return id
    
    // Fallback to known installer packages
    val fallbackPackages = listOf(
        "com.google.android.packageinstaller",
        "com.android.packageinstaller"
    )
    
    for (pkg in fallbackPackages) {
        if (pkg != packageName) {
            id = resources.getIdentifier(name, "id", pkg)
            if (id != 0) return id
        }
    }
    
    return 0
}

internal fun Context.hostString(name: String): String? {
    // Try actual host package first
    var id = resources.getIdentifier(name, "string", packageName)
    if (id != 0) return getString(id)
    
    // Fallback to known installer packages
    val fallbackPackages = listOf(
        "com.google.android.packageinstaller",
        "com.android.packageinstaller"
    )
    
    for (pkg in fallbackPackages) {
        if (pkg != packageName) {
            id = resources.getIdentifier(name, "string", pkg)
            if (id != 0) return getString(id)
        }
    }
    
    return null
}

internal fun <T : View?> Any.findHostView(name: String): T? {
    return when (this) {
        is View -> {
            val id = this.context.hostId(name)
            if (id != 0) this.findViewById<T>(id) else null
        }
        is Activity -> {
            val id = this.hostId(name)
            if (id != 0) this.findViewById<T>(id) else null
        }
        is Dialog -> {
            val id = this.context.hostId(name)
            if (id != 0) this.findViewById<T>(id) else null
        }
        else -> null
    }
}
