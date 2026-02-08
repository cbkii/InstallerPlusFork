package ltd.nextalone.pkginstallerplus.hook

import android.app.Activity
import android.app.Dialog
import android.content.pm.PackageInfo
import android.graphics.Typeface
import android.os.Build
import android.os.UserManager
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import ltd.nextalone.pkginstallerplus.HookEntry.injectModuleResources
import ltd.nextalone.pkginstallerplus.R
import ltd.nextalone.pkginstallerplus.utils.*

// Use generic, system-like tags to avoid detection
private const val TAG_INSTALL_DETAILS = "app_details_install"
private const val TAG_UNINSTALL_DETAILS = "app_details_uninstall"

object InstallerHookBaklava {
    fun initOnce() {
        // Hook Installation Fragment with enhanced error handling for Android 16
        try {
            "$INSTALLER_V2_PKG.fragments.InstallationFragment".clazz?.method("updateUI")?.hookAfter {
                try {
                    handleInstallationFragmentUpdate(it)
                } catch (e: Exception) {
                    logThrowable("Baklava: Installation hook error", e)
                }
            } ?: logError("Baklava: InstallationFragment class or updateUI method not found")
        } catch (e: Exception) {
            logThrowable("Baklava: Failed to hook InstallationFragment", e)
        }

        // Hook Uninstallation Fragment with enhanced error handling for Android 16
        try {
            "$INSTALLER_V2_PKG.fragments.UninstallationFragment".clazz?.method("updateUI")?.hookAfter {
                try {
                    handleUninstallationFragmentUpdate(it)
                } catch (e: Exception) {
                    logThrowable("Baklava: Uninstallation hook error", e)
                }
            } ?: logError("Baklava: UninstallationFragment class or updateUI method not found")
        } catch (e: Exception) {
            logThrowable("Baklava: Failed to hook UninstallationFragment", e)
        }
    }
    
    private fun handleInstallationFragmentUpdate(hookParam: Any) {
        val fragment = (hookParam as? de.robv.android.xposed.XC_MethodHook.MethodHookParam)?.thisObject ?: return
        
        val dialog = fragment.get("mDialog") as? Dialog
        if (dialog == null) {
            logError("Baklava: mDialog not found in fragment")
            return
        }

        val activity = try {
            fragment.javaClass.getMethod("requireActivity").invoke(fragment) as? Activity
        } catch (e: Exception) {
            logThrowable("Baklava: Failed to get activity", e)
            null
        } ?: return

        val isConfirmation = runCatching {
            val viewModel = activity.get("installViewModel")
            if (viewModel == null) {
                logError("Baklava: installViewModel not found")
                return@runCatching false
            }
            
            // Try multiple possible field names for compatibility
            // Check field existence by attempting access without creating unnecessary exceptions
            val fieldNames = listOf("_currentInstallStage", "currentInstallStage", "mCurrentInstallStage")
            var liveData: Any? = null
            
            for (fieldName in fieldNames) {
                liveData = viewModel.get(fieldName)
                if (liveData != null) {
                    logDebug("Baklava: Found install stage field: $fieldName")
                    break
                }
            }

            if (liveData == null) {
                logError("Baklava: currentInstallStage LiveData not found (tried: ${fieldNames.joinToString(", ")})")
                return@runCatching false
            }
            
            val data = liveData.get("mData")
            val simpleName = data?.javaClass?.simpleName
            
            simpleName == "InstallUserActionRequired"
        }.getOrElse { e ->
            logThrowable(msg = "Baklava: stage detection failed", t = e)
            false
        }

        if (isConfirmation) {
            injectModuleResources(activity.resources)
            addInstallDetails(activity, dialog)
        } else {
            removeInstallDetails(dialog)
        }
    }
    
    private fun handleUninstallationFragmentUpdate(hookParam: Any) {
        val fragment = (hookParam as? de.robv.android.xposed.XC_MethodHook.MethodHookParam)?.thisObject ?: return
        
        val dialog = fragment.get("mDialog") as? Dialog
        if (dialog == null) {
            logError("Baklava: mDialog not found in uninstall fragment")
            return
        }

        val activity = try {
            fragment.javaClass.getMethod("requireActivity").invoke(fragment) as? Activity
        } catch (e: Exception) {
            logThrowable("Baklava: Failed to get activity from uninstall fragment", e)
            null
        } ?: return
        
        injectModuleResources(activity.resources)
        addUninstallDetails(activity, dialog)
    }

    private fun addInstallDetails(
        activity: Activity,
        dialog: Dialog,
    ) {
        val appSnippet: ViewGroup? = try {
            dialog.findHostView("app_snippet")
        } catch (e: Exception) {
            logThrowable("Baklava: Failed to find app_snippet", e)
            null
        }
        
        if (appSnippet == null) {
            logError("Baklava: app_snippet view not found")
            return
        }
        
        val parent = appSnippet.parent as? ViewGroup
        if (parent == null) {
            logError("Baklava: app_snippet parent is not a ViewGroup")
            return
        }

        if (parent.findViewWithTag<TextView>(TAG_INSTALL_DETAILS) != null) return

        val viewModel = activity.get("installViewModel")
        if (viewModel == null) {
            logError("Baklava: installViewModel not found in activity")
            return
        }
        
        val repository = viewModel.get("repository")
        if (repository == null) {
            logError("Baklava: repository not found in viewModel")
            return
        }
        
        val newPkgInfo = repository.get("newPackageInfo") as? PackageInfo
        if (newPkgInfo == null) {
            logError("Baklava: newPackageInfo not found in repository")
            return
        }
        
        val usrManager = repository.get("userManager") as? UserManager
        val oldPkgInfo = activity.packageManager.getPackageInfoOrNull(newPkgInfo.packageName)

        val sb = SpannableStringBuilder()
        sb.append(activity.getString(R.string.IPP_info_user) + ": ")
        
        // Android 16 compatibility: Handle null userName gracefully
        val userName = try {
            usrManager?.userName ?: "Unknown"
        } catch (e: Exception) {
            logThrowable("Baklava: Failed to get userName", e)
            "Unknown"
        }
        
        sb.append(userName)
            .append('\n')
            .append(activity.getString(R.string.IPP_info_package) + ": ")
            .append(
                newPkgInfo.packageName,
                ForegroundColorSpan(ThemeUtil.colorGreen),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            ).append('\n')

        if (oldPkgInfo == null) {
            sb
                .append(activity.getString(R.string.IPP_info_version) + ": ")
                .append(
                    "${newPkgInfo.versionName ?: "N/A"}(${newPkgInfo.compatLongVersionCode()})",
                    ForegroundColorSpan(ThemeUtil.colorGreen),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                ).append('\n')
                .append(activity.getString(R.string.IPP_info_sdk) + ": ")
                .append(
                    newPkgInfo.applicationInfo?.targetSdkVersion?.toString() ?: "N/A",
                    ForegroundColorSpan(ThemeUtil.colorGreen),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
        } else {
            sb
                .append(activity.getString(R.string.IPP_info_version) + ": ")
                .append(
                    "${oldPkgInfo.versionName ?: "N/A"}(${oldPkgInfo.compatLongVersionCode()})",
                    ForegroundColorSpan(ThemeUtil.colorRed),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                ).append(" ➞ ")
                .append(
                    "${newPkgInfo.versionName ?: "N/A"}(${newPkgInfo.compatLongVersionCode()})",
                    ForegroundColorSpan(ThemeUtil.colorGreen),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                ).append('\n')
                .append(activity.getString(R.string.IPP_info_sdk) + ": ")
                .append(
                    oldPkgInfo.applicationInfo?.targetSdkVersion?.toString() ?: "N/A",
                    ForegroundColorSpan(ThemeUtil.colorRed),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                ).append(" ➞ ")
                .append(
                    newPkgInfo.applicationInfo?.targetSdkVersion?.toString() ?: "N/A",
                    ForegroundColorSpan(ThemeUtil.colorGreen),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
        }

        parent.addView(
            TextView(activity).apply {
                tag = TAG_INSTALL_DETAILS
                setTextIsSelectable(true)
                typeface = Typeface.MONOSPACE
                text = sb
            },
        )
    }

    private fun removeInstallDetails(dialog: Dialog) {
        val appSnippet: ViewGroup = dialog.findHostView("app_snippet") ?: return
        val parent = appSnippet.parent as? ViewGroup ?: return
        parent.findViewWithTag<View>(TAG_INSTALL_DETAILS)?.let { parent.removeView(it) }
    }

    private fun addUninstallDetails(
        activity: Activity,
        dialog: Dialog,
    ) {
        val appSnippet: ViewGroup? = try {
            dialog.findHostView("app_snippet")
        } catch (e: Exception) {
            logThrowable("Baklava: Failed to find app_snippet in uninstall", e)
            null
        }
        
        if (appSnippet == null) {
            logError("Baklava: app_snippet view not found in uninstall dialog")
            return
        }
        
        val parent = appSnippet.parent as? ViewGroup
        if (parent == null) {
            logError("Baklava: app_snippet parent is not a ViewGroup in uninstall")
            return
        }

        if (parent.findViewWithTag<TextView>(TAG_UNINSTALL_DETAILS) != null) return

        val viewModel = activity.get("uninstallViewModel")
        if (viewModel == null) {
            logError("Baklava: uninstallViewModel not found")
            return
        }
        
        val repository = viewModel.get("repository")
        if (repository == null) {
            logError("Baklava: repository not found in uninstallViewModel")
            return
        }
        
        val packageName = repository.get("targetPackageName") as? String
        if (packageName == null) {
            logError("Baklava: targetPackageName not found")
            return
        }
        
        val pkgInfo = activity.packageManager.getPackageInfoOrNull(packageName)
        if (pkgInfo == null) {
            logError("Baklava: Package info not found for $packageName")
            return
        }

        val sb = SpannableStringBuilder()
        sb
            .append(activity.getString(R.string.IPP_info_package) + ": ")
            .append(
                packageName,
                ForegroundColorSpan(ThemeUtil.colorRed),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            ).append('\n')
            .append(activity.getString(R.string.IPP_info_version) + ": ")
            .append(
                "${pkgInfo.versionName ?: "N/A"}(${pkgInfo.compatLongVersionCode()})",
                ForegroundColorSpan(ThemeUtil.colorRed),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            ).append('\n')
            .append(activity.getString(R.string.IPP_info_sdk) + ": ")
            .append(
                pkgInfo.applicationInfo?.targetSdkVersion?.toString() ?: "N/A",
                ForegroundColorSpan(ThemeUtil.colorRed),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )

        parent.addView(
            TextView(activity).apply {
                tag = TAG_UNINSTALL_DETAILS
                setTextIsSelectable(true)
                typeface = Typeface.MONOSPACE
                text = sb
            },
        )
    }
}

@Suppress("DEPRECATION")
private fun PackageInfo.compatLongVersionCode(): Long =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) longVersionCode else versionCode.toLong()
