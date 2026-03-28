package me.lakitu.permissioncontrol

import android.content.ContentResolver
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

class SystemSettingsController(private val context: Context) {

    private val contentResolver: ContentResolver = context.contentResolver

    fun isUsbDebuggingEnabled(): Boolean {
        return try {
            Settings.Secure.getInt(contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        } catch (e: Exception) {
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
        } catch (e: Exception) {
            false
        }
    }

    fun isWirelessDebuggingEnabled(): Boolean {
        return try {
            Settings.Global.getInt(contentResolver, "adb_wifi_enabled", 0) == 1
        } catch (e: Exception) {
            false
        }
    }

    fun setWirelessDebuggingEnabled(enabled: Boolean): Boolean {
        return try {
            Settings.Global.putInt(
                contentResolver,
                "adb_wifi_enabled",
                if (enabled) 1 else 0
            )
            true
        } catch (e: Exception) {
            android.util.Log.e("SettingsController", "Failed to set adb_wifi_enabled", e)
            false
        }
    }

    fun isStayOnWhilePluggedInEnabled(): Boolean {
        return try {
            val value = Settings.System.getInt(contentResolver, Settings.System.STAY_ON_WHILE_PLUGGED_IN, 0)
            value != 0
        } catch (e: Exception) {
            false
        }
    }

    fun setStayOnWhilePluggedInEnabled(enabled: Boolean): Boolean {
        val value = if (enabled) {
            (1 or 2 or 4)
        } else {
            0
        }
        return try {
            Settings.System.putInt(contentResolver, Settings.System.STAY_ON_WHILE_PLUGGED_IN, value)
        } catch (e: Exception) {
            false
        }
    }

    fun getWindowAnimationScale(): Float {
        return try {
            Settings.System.getFloat(contentResolver, Settings.System.WINDOW_ANIMATION_SCALE, 1.0f)
        } catch (e: Exception) {
            1.0f
        }
    }

    fun setWindowAnimationScale(scale: Float): Boolean {
        return try {
            Settings.System.putFloat(contentResolver, Settings.System.WINDOW_ANIMATION_SCALE, scale)
        } catch (e: Exception) {
            false
        }
    }

    fun getTransitionAnimationScale(): Float {
        return try {
            Settings.System.getFloat(contentResolver, Settings.System.TRANSITION_ANIMATION_SCALE, 1.0f)
        } catch (e: Exception) {
            1.0f
        }
    }

    fun setTransitionAnimationScale(scale: Float): Boolean {
        return try {
            Settings.System.putFloat(contentResolver, Settings.System.TRANSITION_ANIMATION_SCALE, scale)
        } catch (e: Exception) {
            false
        }
    }

    fun getAnimatorDurationScale(): Float {
        return try {
            Settings.System.getFloat(contentResolver, Settings.System.ANIMATOR_DURATION_SCALE, 1.0f)
        } catch (e: Exception) {
            1.0f
        }
    }

    fun setAnimatorDurationScale(scale: Float): Boolean {
        return try {
            Settings.System.putFloat(contentResolver, Settings.System.ANIMATOR_DURATION_SCALE, scale)
        } catch (e: Exception) {
            false
        }
    }

    fun isShowTouchesEnabled(): Boolean {
        return try {
            Settings.System.getInt(contentResolver, "show_touches", 0) == 1
        } catch (e: Exception) {
            false
        }
    }

    fun setShowTouchesEnabled(enabled: Boolean): Boolean {
        return try {
            Settings.System.putInt(
                contentResolver,
                "show_touches",
                if (enabled) 1 else 0
            )
        } catch (e: Exception) {
            false
        }
    }

    fun isPointerLocationEnabled(): Boolean {
        return try {
            Settings.System.getInt(contentResolver, "pointer_location", 0) == 1
        } catch (e: Exception) {
            false
        }
    }

    fun setPointerLocationEnabled(enabled: Boolean): Boolean {
        return try {
            Settings.System.putInt(
                contentResolver,
                "pointer_location",
                if (enabled) 1 else 0
            )
        } catch (e: Exception) {
            false
        }
    }

    fun isDevelopmentSettingsEnabled(): Boolean {
        return try {
            Settings.Secure.getInt(contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
        } catch (e: Exception) {
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
        } catch (e: Exception) {
            false
        }
    }

    data class SettingItem(
        val name: String,
        val key: String,
        val isBoolean: Boolean = true,
        val getValue: () -> Any?,
        val setValue: (Boolean) -> Boolean
    )

    fun getAllSettings(): List<SettingItem> {
        return listOf(
            SettingItem(
                name = "USB调试",
                key = "adb_enabled",
                getValue = { isUsbDebuggingEnabled() },
                setValue = { setUsbDebuggingEnabled(it) }
            ),
             SettingItem(
                 name = "无线调试",
                 key = "adb_wifi_enabled",
                 getValue = { isWirelessDebuggingEnabled() },
                 setValue = { setWirelessDebuggingEnabled(it) }
             ),



            SettingItem(
                name = "开发者模式",
                key = "development_settings_enabled",
                getValue = { isDevelopmentSettingsEnabled() },
                setValue = { setDevelopmentSettingsEnabled(it) }
            )
        )
    }
}
