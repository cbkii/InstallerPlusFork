package ltd.nextalone.pkginstallerplus.hook

import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.UserManager
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi

import ltd.nextalone.pkginstallerplus.HookEntry.injectModuleResources
import ltd.nextalone.pkginstallerplus.R
import ltd.nextalone.pkginstallerplus.utils.*

@RequiresApi(29)
object InstallerHookQ {
    fun initOnce() {
        "com.android.packageinstaller.PackageInstallerActivity".clazz?.method("startInstallConfirm")?.hookAfter {
            val ctx: Activity = it.thisObject as? Activity ?: return@hookAfter
            val resourcesInjected = injectModuleResources(ctx.resources)
            Thread {
                Thread.sleep(100)
                ctx.runOnUiThread {
                    try {
                        addInstallDetails(ctx, resourcesInjected)
                    } catch (t: Throwable) {
                        logThrowable("InstallerHookQ.addInstallDetails", t)
                    }
                }
            }.start()
        }

        "com.android.packageinstaller.UninstallerActivity".clazz?.method("showConfirmationDialog")?.hookBefore {
            val ctx: Activity = it.thisObject as? Activity ?: return@hookBefore
            val resourcesInjected = injectModuleResources(ctx.resources)
            "com.android.packageinstaller.handheld.UninstallAlertDialogFragment".clazz?.method("onCreateDialog")?.hookAfter { it2 ->
                val dialog = it2.result as? AlertDialog ?: return@hookAfter
                ctx.runOnUiThread {
                    try {
                        addUninstallDetails(ctx, dialog, resourcesInjected)
                    } catch (t: Throwable) {
                        logThrowable("InstallerHookQ.addUninstallDetails", t)
                    }
                }
            }
        }
    }

    private fun addInstallDetails(activity: Activity, resourcesInjected: Boolean) {
        val textView = TextView(activity)
        textView.setTextIsSelectable(true)
        textView.typeface = Typeface.MONOSPACE

        val layout = LinearLayout(activity)
        val newPkgInfo: PackageInfo = activity.get("mPkgInfo") as? PackageInfo ?: return
        val usrManager: UserManager = activity.get("mUserManager") as? UserManager ?: return
        val pkgName = newPkgInfo.packageName
        val oldPkgInfo = try {
            activity.packageManager.getPackageInfo(pkgName, PackageManager.MATCH_UNINSTALLED_PACKAGES)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }

        val sb = SpannableStringBuilder()
        if (oldPkgInfo == null) {
            val install: View? = activity.findHostView("install_confirm_question") ?:
            activity.get("mDialog")?.findHostView("install_confirm_question") // QPR2+
            val newVersionStr = (newPkgInfo.versionName ?: "N/A") + "(" + newPkgInfo.longVersionCode + ")"
            val newSdkStr = newPkgInfo.applicationInfo?.targetSdkVersion?.toString() ?: "N/A"

            // Use localized strings if resources were injected, otherwise use English fallbacks
            val userLabel = if (resourcesInjected) activity.getString(R.string.IPP_info_user) else "User"
            val packageLabel = if (resourcesInjected) activity.getString(R.string.IPP_info_package) else "Package name"
            val versionLabel = if (resourcesInjected) activity.getString(R.string.IPP_info_version) else "Version"
            val sdkLabel = if (resourcesInjected) activity.getString(R.string.IPP_info_sdk) else "Target SDK"

            sb.append("$userLabel: ")
                .append(usrManager.userName)
                .append('\n')
                .append("$packageLabel: ")
                .append(pkgName, ForegroundColorSpan(ThemeUtil.colorGreen), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                .append('\n')
                .append("$versionLabel: ")
                .append(newVersionStr, ForegroundColorSpan(ThemeUtil.colorGreen), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                .append('\n')
                .append("$sdkLabel: ")
                .append(newSdkStr, ForegroundColorSpan(ThemeUtil.colorGreen), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            if (install != null) {
                layout.setPadding(0, install.height, 0, 0)
                textView.text = sb
                layout.addView(textView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                (install.parent as? ViewGroup)?.addView(layout)
            }
        } else {
            val update: View? = activity.findHostView("install_confirm_question_update") ?:
            activity.get("mDialog")?.findHostView("install_confirm_question_update") // QPR2+
            val oldVersionStr = """${oldPkgInfo.versionName ?: "N/A"}(${oldPkgInfo.longVersionCode})"""
            val newVersionStr = """${newPkgInfo.versionName ?: "N/A"}(${newPkgInfo.longVersionCode})"""
            val oldSdkStr = oldPkgInfo.applicationInfo?.targetSdkVersion?.toString() ?: "N/A"
            val newSdkStr = newPkgInfo.applicationInfo?.targetSdkVersion?.toString() ?: "N/A"

            // Use localized strings if resources were injected, otherwise use English fallbacks
            val userLabel = if (resourcesInjected) activity.getString(R.string.IPP_info_user) else "User"
            val packageLabel = if (resourcesInjected) activity.getString(R.string.IPP_info_package) else "Package name"
            val versionLabel = if (resourcesInjected) activity.getString(R.string.IPP_info_version) else "Version"
            val sdkLabel = if (resourcesInjected) activity.getString(R.string.IPP_info_sdk) else "Target SDK"

            sb.append("$userLabel: ")
                .append(usrManager.userName)
                .append('\n')
                .append("$packageLabel: ")
                .append(pkgName, ForegroundColorSpan(ThemeUtil.colorGreen), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                .append('\n')
                .append("$versionLabel: ")
                .append(oldVersionStr, ForegroundColorSpan(ThemeUtil.colorRed), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                .append(" ➞ ")
                .append(newVersionStr, ForegroundColorSpan(ThemeUtil.colorGreen), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                .append('\n')
                .append("$sdkLabel: ")
                .append(oldSdkStr, ForegroundColorSpan(ThemeUtil.colorRed), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                .append(" ➞ ")
                .append(newSdkStr, ForegroundColorSpan(ThemeUtil.colorGreen), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            if (update != null) {
                layout.setPadding(0, update.height, 0, 0)
                textView.text = sb
                layout.addView(textView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                (update.parent as? ViewGroup)?.addView(layout)
            }
        }
    }

    private fun addUninstallDetails(activity: Activity, dialog: AlertDialog, resourcesInjected: Boolean) {
        val textView = TextView(activity)
        textView.setTextIsSelectable(true)
        textView.typeface = Typeface.MONOSPACE

        val layout = LinearLayout(activity)
        if (activity.taskId == -1) return
        val packageName = activity.get("mDialogInfo")?.get("appInfo")?.get("packageName") as? String ?: return
        val oldPkgInfo = try {
            activity.packageManager.getPackageInfo(packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }

        val sb = SpannableStringBuilder()
        if (oldPkgInfo != null) {
            val oldVersionStr = (oldPkgInfo.versionName ?: "N/A") + "(" + oldPkgInfo.longVersionCode + ")"
            val oldSdkStr = oldPkgInfo.applicationInfo?.targetSdkVersion?.toString() ?: "N/A"

            // Use localized strings if resources were injected, otherwise use English fallbacks
            val packageLabel = if (resourcesInjected) activity.getString(R.string.IPP_info_package) else "Package name"
            val versionLabel = if (resourcesInjected) activity.getString(R.string.IPP_info_version) else "Version"
            val sdkLabel = if (resourcesInjected) activity.getString(R.string.IPP_info_sdk) else "Target SDK"

            sb.append("$packageLabel: ")
                .append(packageName, ForegroundColorSpan(ThemeUtil.colorRed), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                .append('\n')
                .append("$versionLabel: ")
                .append(oldVersionStr, ForegroundColorSpan(ThemeUtil.colorRed), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                .append('\n')
                .append("$sdkLabel: ")
                .append(oldSdkStr, ForegroundColorSpan(ThemeUtil.colorRed), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            layout.setPadding(activity.dip2px(24f), 0, activity.dip2px(24f), 0)
            textView.text = sb
            layout.addView(textView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            dialog.setView(layout)
        }
    }
}
