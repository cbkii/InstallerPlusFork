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

private const val TAG_INSTALL_DETAILS = "IPP_install_details"
private const val TAG_UNINSTALL_DETAILS = "IPP_uninstall_details"

object InstallerHookBaklava {
    fun initOnce() {
        "$INSTALLER_V2_PKG.fragments.InstallationFragment".clazz?.method("updateUI")?.hookAfter {
            val fragment = it.thisObject
            val dialog = fragment.get("mDialog") as? Dialog ?: return@hookAfter

            val activity =
                fragment.javaClass.getMethod("requireActivity").invoke(fragment) as? Activity
                    ?: return@hookAfter

            val isConfirmation = runCatching {
                val viewModel = activity.get("installViewModel") ?: return@hookAfter
                val liveData = listOf("_currentInstallStage", "currentInstallStage")
                    .firstNotNullOfOrNull { key -> viewModel.get(key) }

                liveData?.get("mData")?.javaClass?.simpleName == "InstallUserActionRequired"
            }.getOrElse { e ->
                logThrowable(msg = "Baklava: stage detection failed", t = e)
                false
            }

            try {
                if (isConfirmation) {
                    val resourcesInjected = injectModuleResources(activity.resources)
                    addInstallDetails(activity, dialog, resourcesInjected)
                } else {
                    removeInstallDetails(dialog)
                }
            } catch (t: Throwable) {
                logThrowable("InstallerHookBaklava.updateUI", t)
            }
        }

        "$INSTALLER_V2_PKG.fragments.UninstallationFragment".clazz?.method("updateUI")?.hookAfter {
            val fragment = it.thisObject
            val dialog = fragment.get("mDialog") as? Dialog ?: return@hookAfter

            val activity =
                fragment.javaClass.getMethod("requireActivity").invoke(fragment) as? Activity
                    ?: return@hookAfter
            
            try {
                val resourcesInjected = injectModuleResources(activity.resources)
                addUninstallDetails(activity, dialog, resourcesInjected)
            } catch (t: Throwable) {
                logThrowable("InstallerHookBaklava.addUninstallDetails", t)
            }
        }
    }

    private fun addInstallDetails(
        activity: Activity,
        dialog: Dialog,
        resourcesInjected: Boolean,
    ) {
        val appSnippet: ViewGroup = dialog.findHostView("app_snippet") ?: return
        val parent = appSnippet.parent as? ViewGroup ?: return

        if (parent.findViewWithTag<TextView>(TAG_INSTALL_DETAILS) != null) return

        val viewModel = activity.get("installViewModel") ?: return
        val repository = viewModel.get("repository") ?: return
        val newPkgInfo = repository.get("newPackageInfo") as? PackageInfo ?: return
        val usrManager = repository.get("userManager") as? UserManager ?: return
        val oldPkgInfo = activity.packageManager.getPackageInfoOrNull(newPkgInfo.packageName)

        // Use localized strings if resources were injected, otherwise use English fallbacks
        val userLabel = if (resourcesInjected) activity.getString(R.string.IPP_info_user) else "User"
        val packageLabel = if (resourcesInjected) activity.getString(R.string.IPP_info_package) else "Package name"
        val versionLabel = if (resourcesInjected) activity.getString(R.string.IPP_info_version) else "Version"
        val sdkLabel = if (resourcesInjected) activity.getString(R.string.IPP_info_sdk) else "Target SDK"

        val sb = SpannableStringBuilder()
        sb
            .append("$userLabel: ")
            .append(usrManager.userName ?: "Owner")
            .append('\n')
            .append("$packageLabel: ")
            .append(
                newPkgInfo.packageName,
                ForegroundColorSpan(ThemeUtil.colorGreen),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            ).append('\n')

        if (oldPkgInfo == null) {
            sb
                .append("$versionLabel: ")
                .append(
                    "${newPkgInfo.versionName ?: "N/A"}(${newPkgInfo.compatLongVersionCode()})",
                    ForegroundColorSpan(ThemeUtil.colorGreen),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                ).append('\n')
                .append("$sdkLabel: ")
                .append(
                    newPkgInfo.applicationInfo?.targetSdkVersion?.toString() ?: "N/A",
                    ForegroundColorSpan(ThemeUtil.colorGreen),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
        } else {
            sb
                .append("$versionLabel: ")
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
                .append("$sdkLabel: ")
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
        resourcesInjected: Boolean,
    ) {
        val appSnippet: ViewGroup = dialog.findHostView("app_snippet") ?: return
        val parent = appSnippet.parent as? ViewGroup ?: return

        if (parent.findViewWithTag<TextView>(TAG_UNINSTALL_DETAILS) != null) return

        val viewModel = activity.get("uninstallViewModel") ?: return
        val repository = viewModel.get("repository") ?: return
        val packageName = repository.get("targetPackageName") as? String ?: return
        val pkgInfo = activity.packageManager.getPackageInfoOrNull(packageName) ?: return

        // Use localized strings if resources were injected, otherwise use English fallbacks
        val packageLabel = if (resourcesInjected) activity.getString(R.string.IPP_info_package) else "Package name"
        val versionLabel = if (resourcesInjected) activity.getString(R.string.IPP_info_version) else "Version"
        val sdkLabel = if (resourcesInjected) activity.getString(R.string.IPP_info_sdk) else "Target SDK"

        val sb = SpannableStringBuilder()
        sb
            .append("$packageLabel: ")
            .append(
                packageName,
                ForegroundColorSpan(ThemeUtil.colorRed),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            ).append('\n')
            .append("$versionLabel: ")
            .append(
                "${pkgInfo.versionName ?: "N/A"}(${pkgInfo.compatLongVersionCode()})",
                ForegroundColorSpan(ThemeUtil.colorRed),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            ).append('\n')
            .append("$sdkLabel: ")
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
