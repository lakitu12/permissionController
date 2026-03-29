package me.lakitu.permissioncontrol

import android.content.ContentResolver
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.provider.Settings

class SystemSettingsController(private val context: Context) {

    private val contentResolver: ContentResolver = context.contentResolver

    fun isUsbDebuggingEnabled(): Boolean {
        return try {
            Settings.Secure.getInt(contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        } catch (_: Exception) {
            false
        }
    }

    fun setUsbDebuggingEnabled(enabled: Boolean): Boolean {
        return try {
            Settings.Secure.putInt(
                contentResolver,
                Settings.Global.ADB_ENABLED,
                if (enabled) 1 else 0
            )
        } catch (_: Exception) {
            false
        }
    }

    fun isWirelessDebuggingEnabled(): Boolean {
        return try {
            Settings.Global.getInt(contentResolver, "adb_wifi_enabled", 0) == 1
        } catch (_: Exception) {
            false
        }
    }

    fun isWifiConnected(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } catch (_: Exception) {
            false
        }
    }

    fun isWifiEnabled(): Boolean {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiManager.isWifiEnabled
        } catch (_: Exception) {
            false
        }
    }

    sealed class WirelessDebuggingResult {
        data class Success(val enabled: Boolean) : WirelessDebuggingResult()
        data object WifiNotConnected : WirelessDebuggingResult()
        data object WifiDisabled : WirelessDebuggingResult()
        data class Error(val message: String) : WirelessDebuggingResult()
    }

    fun setWirelessDebuggingEnabled(enabled: Boolean): WirelessDebuggingResult {
        return try {
            if (enabled) {
                if (!isWifiEnabled()) {
                    return WirelessDebuggingResult.WifiDisabled
                }
                if (!isWifiConnected()) {
                    return WirelessDebuggingResult.WifiNotConnected
                }
            }
            Settings.Global.putInt(
                contentResolver,
                "adb_wifi_enabled",
                if (enabled) 1 else 0
            )
            WirelessDebuggingResult.Success(enabled)
        } catch (e: Exception) {
            android.util.Log.e("SettingsController", "Failed to set adb_wifi_enabled", e)
            WirelessDebuggingResult.Error(e.message ?: "Unknown error")
        }
    }

    fun isDevelopmentSettingsEnabled(): Boolean {
        return try {
            Settings.Secure.getInt(contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
        } catch (_: Exception) {
            false
        }
    }

    fun setDevelopmentSettingsEnabled(enabled: Boolean): Boolean {
        return try {
            Settings.Secure.putInt(
                contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                if (enabled) 1 else 0
            )
        } catch (_: Exception) {
            false
        }
    }

    sealed class SettingResult {
        data class Success(val enabled: Boolean) : SettingResult()
        data class Error(val message: String) : SettingResult()
        data object WifiDisabled : SettingResult()
        data object WifiNotConnected : SettingResult()
    }

    data class SettingItem(
        val name: String,
        val key: String,
        val isBoolean: Boolean = true,
        val getValue: () -> Any?,
        val setValue: (Boolean) -> SettingResult
    )

    fun getAllSettings(): List<SettingItem> {
        return listOf(
            SettingItem(
                name = "USB调试",
                key = "adb_enabled",
                getValue = { isUsbDebuggingEnabled() },
                setValue = { 
                    val result = setUsbDebuggingEnabled(it)
                    if (result) SettingResult.Success(it) else SettingResult.Error("设置失败")
                }
            ),
            SettingItem(
                name = "无线调试",
                key = "adb_wifi_enabled",
                getValue = { isWirelessDebuggingEnabled() },
                setValue = { 
                    when (val result = setWirelessDebuggingEnabled(it)) {
                        is WirelessDebuggingResult.Success -> SettingResult.Success(result.enabled)
                        is WirelessDebuggingResult.WifiDisabled -> SettingResult.WifiDisabled
                        is WirelessDebuggingResult.WifiNotConnected -> SettingResult.WifiNotConnected
                        is WirelessDebuggingResult.Error -> SettingResult.Error(result.message)
                    }
                }
            ),
            SettingItem(
                name = "开发者模式",
                key = "development_settings_enabled",
                getValue = { isDevelopmentSettingsEnabled() },
                setValue = { 
                    val result = setDevelopmentSettingsEnabled(it)
                    if (result) SettingResult.Success(it) else SettingResult.Error("设置失败")
                }
            )
        )
    }
}
