package ltd.nextalone.pkginstallerplus.utils

import android.util.Log
import ltd.nextalone.pkginstallerplus.BuildConfig

// Use generic system-like tag for stealth - appears as system component
internal const val TAG = "PackageInstaller"

// Only log in debug builds to avoid detection in production
internal fun logDebug(msg: String) {
    if (BuildConfig.DEBUG) {
        Log.d(TAG, msg)
    }
}

internal fun logError(msg: String) {
    if (BuildConfig.DEBUG) {
        Log.e(TAG, msg)
    }
}

internal fun logThrowable(msg: String, t: Throwable? = null) {
    if (BuildConfig.DEBUG) {
        Log.e(TAG, msg + t?.message, t)
    }
}

internal fun <T : Any> T.logDetail(info: String, vararg msg: Any) {
    logDebug("${this.javaClass.simpleName}: $info, ${msg.joinToString(", ")}")
}

internal fun <T : Any> T.logStart() {
    logDebug("$this: Start")
}

internal fun <T : Any> T.logBefore(msg: String = "") {
    logDebug("$this: Before, $msg")
}

internal fun <T : Any> T.logAfter(msg: String = "") {
    logDebug("$this: After, $msg")
}
