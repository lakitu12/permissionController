package me.lakitu.permissioncontroller

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.provider.Settings

class AccessibilityAppManager(context: Context) {

    private val contentResolver: ContentResolver = context.contentResolver
    private val pm: PackageManager = context.packageManager

    data class AppInfo(
        val packageName: String,
        val appName: String,
        val serviceName: String,
        val icon: Drawable?,
        val getIsAccessibilityEnabled: () -> Boolean
    )

    private fun getEnabledServicesSet(): Set<String> {
        val enabledServicesStr = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        return enabledServicesStr.split(":").filter { it.isNotEmpty() }.toSet()
    }

    fun getAllAppsWithAccessibilityService(): List<AppInfo> {
        val resolveInfos = pm.queryIntentServices(
            android.content.Intent("android.accessibilityservice.AccessibilityService"),
            PackageManager.GET_SERVICES
        )

        val appServices = mutableMapOf<String, MutableList<String>>()

        for (resolveInfo in resolveInfos) {
            val serviceInfo = resolveInfo.serviceInfo ?: continue
            val packageName = serviceInfo.packageName
            val className = serviceInfo.name
            val flatName = "$packageName/$className"

            appServices.getOrPut(packageName) { mutableListOf() }.add(flatName)
        }

        return appServices.mapNotNull { (packageName, services) ->
            try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                AppInfo(
                    packageName = packageName,
                    appName = pm.getApplicationLabel(appInfo).toString(),
                    serviceName = services.first(),
                    icon = try { pm.getApplicationIcon(packageName) } catch (_: Exception) { null },
                    getIsAccessibilityEnabled = {
                        val currentEnabled = getEnabledServicesSet()
                        currentEnabled.any { enabled ->
                            enabled.startsWith("$packageName/")
                        }
                    }
                )
            } catch (_: Exception) {
                null
            }
        }.sortedBy { it.appName }
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
        } catch (_: Exception) {
            false
        }
    }
}
