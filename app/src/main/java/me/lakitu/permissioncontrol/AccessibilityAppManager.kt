package me.lakitu.permissioncontrol

import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.provider.Settings

class AccessibilityAppManager(private val context: Context) {

    private val contentResolver: ContentResolver = context.contentResolver
    private val pm: PackageManager = context.packageManager

    data class AppInfo(
        val packageName: String,
        val appName: String,
        val serviceName: String,
        val icon: Drawable?,
        val isAccessibilityEnabled: Boolean
    )

    private fun getEnabledServicesSet(): Set<String> {
        val enabledServicesStr = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        return enabledServicesStr.split(":").filter { it.isNotEmpty() }.toSet()
    }

    fun getAllAppsWithAccessibilityService(): List<AppInfo> {
        val enabledServices = getEnabledServicesSet()
        val apps = mutableListOf<AppInfo>()

        val resolveInfos = pm.queryIntentServices(
            android.content.Intent("android.accessibilityservice.AccessibilityService"),
            PackageManager.GET_SERVICES
        )

        for (resolveInfo in resolveInfos) {
            val serviceInfo = resolveInfo.serviceInfo ?: continue
            val packageName = serviceInfo.packageName
            val className = serviceInfo.name
            val flatName = "$packageName/$className"
            
            try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                apps.add(AppInfo(
                    packageName = packageName,
                    appName = pm.getApplicationLabel(appInfo).toString(),
                    serviceName = flatName,
                    icon = try { pm.getApplicationIcon(packageName) } catch (e: Exception) { null },
                    isAccessibilityEnabled = enabledServices.contains(flatName)
                ))
            } catch (e: Exception) {
            }
        }

        return apps.distinctBy { it.packageName }.sortedBy { it.appName }
    }

    fun isAppAccessibilityEnabled(packageName: String): Boolean {
        val enabledServices = getEnabledServicesSet()
        return enabledServices.any { it.startsWith(packageName) }
    }

    fun setAppAccessibilityEnabled(packageName: String, enable: Boolean): Boolean {
        return try {
            val enabledServices = getEnabledServicesSet().toMutableSet()

            val resolveInfos = pm.queryIntentServices(
                android.content.Intent("android.accessibilityservice.AccessibilityService"),
                PackageManager.GET_SERVICES
            )

            for (resolveInfo in resolveInfos) {
                val serviceInfo = resolveInfo.serviceInfo ?: continue
                if (serviceInfo.packageName == packageName) {
                    val flatName = "${serviceInfo.packageName}/${serviceInfo.name}"
                    
                    if (enable) {
                        enabledServices.add(flatName)
                    } else {
                        enabledServices.remove(flatName)
                    }
                }
            }

            val newValue = enabledServices.joinToString(":")

            Settings.Secure.putString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                newValue
            )

            Settings.Secure.putInt(
                contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED,
                1
            )

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
