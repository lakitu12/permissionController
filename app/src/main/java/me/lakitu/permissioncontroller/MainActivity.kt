package me.lakitu.permissioncontroller

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.lakitu.permissioncontroller.databinding.ActivityMainBinding
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var systemSettingsController: SystemSettingsController
    private lateinit var accessibilityAppManager: AccessibilityAppManager

    private var settingsAdapter: SettingsAdapter? = null
    private var appsAdapter: AppsAdapter? = null

    private val refreshHandler = Handler(Looper.getMainLooper())
    @Suppress("NotifyDataSetChanged")
    private val refreshRunnable = object : Runnable {
        override fun run() {
            settingsAdapter?.notifyDataSetChanged()
            appsAdapter?.notifyDataSetChanged()
            refreshHandler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            binding.statusBarView.layoutParams.height = statusBarHeight
            binding.statusBarView.requestLayout()
            insets
        }

        systemSettingsController = SystemSettingsController(this)
        accessibilityAppManager = AccessibilityAppManager(this)

        setupListeners()
        updatePermissionStatus()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
        refreshHandler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        refreshHandler.removeCallbacks(refreshRunnable)
    }

    private fun hasWriteSecureSettings(): Boolean {
        return packageManager.checkPermission(
            android.Manifest.permission.WRITE_SECURE_SETTINGS,
            packageName
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun setupListeners() {
        binding.btnSecureSettings.setOnClickListener {
            if (hasWriteSecureSettings()) {
                showSecureSettingsInfo()
            } else {
                showGrantSecureSettingsDialog()
            }
        }

        binding.btnSystemSettings.setOnClickListener {
            if (hasWriteSecureSettings()) {
                showSystemSettingsDialog()
            } else {
                Toast.makeText(this, "请先授予WRITE_SECURE_SETTINGS权限", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnAppAccessibility.setOnClickListener {
            if (hasWriteSecureSettings()) {
                showAppsAccessibilityDialog()
            } else {
                Toast.makeText(this, "请先授予WRITE_SECURE_SETTINGS权限", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showSecureSettingsInfo() {
        val message = "WRITE_SECURE_SETTINGS 权限已授予！"
        AlertDialog.Builder(this)
            .setTitle("权限状态")
            .setMessage(message)
            .setPositiveButton("确定", null)
            .show()
    }

    private fun showGrantSecureSettingsDialog() {
        val message = """
            需要 WRITE_SECURE_SETTINGS 权限。
            
            请在电脑上执行：
            adb shell pm grant me.lakitu.permissioncontroller android.permission.WRITE_SECURE_SETTINGS
        """.trimIndent()
        // 仅复制指令文本，不复制完整提示信息
        val copyText = "adb shell pm grant me.lakitu.permissioncontroller android.permission.WRITE_SECURE_SETTINGS"

        val dialogView = layoutInflater.inflate(R.layout.dialog_permission_message_copy, null)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tv_message)
        tvMessage.text = message
        // 允许文本选择复制（弹窗文本可长按复制）
        tvMessage.setTextIsSelectable(true)

        AlertDialog.Builder(this)
            .setTitle("需要权限")
            .setView(dialogView)
            .setPositiveButton("确定", null)
            .setNeutralButton("复制") { _, _ ->
                // 将指令文本复制到剪贴板（仅命令）
                @Suppress("UnnecessaryQualifiedReference")
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("permission_grant_command", copyText)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showSystemSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings_list, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recycler_settings)
        
        val settings = systemSettingsController.getAllSettings()
        settingsAdapter = SettingsAdapter(settings) { setting, enable ->
            val result = setting.setValue(enable)
            val message = when (result) {
                is SystemSettingsController.SettingResult.Success -> 
                    "${setting.name} 已${if (result.enabled) "开启" else "关闭"}"
                is SystemSettingsController.SettingResult.Error -> 
                    "${setting.name} 设置失败: ${result.message}"
                is SystemSettingsController.SettingResult.WifiDisabled -> 
                    "请先开启WiFi"
                is SystemSettingsController.SettingResult.WifiNotConnected -> 
                    "请先连接WiFi网络"
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            result
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = settingsAdapter
        
        AlertDialog.Builder(this)
            .setTitle("系统设置")
            .setView(dialogView)
            .setPositiveButton("关闭") { _, _ ->
                settingsAdapter = null
            }
            .show()
    }

    private fun showAppsAccessibilityDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_apps_list, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recycler_apps)
        
        val apps = accessibilityAppManager.getAllAppsWithAccessibilityService()
        appsAdapter = AppsAdapter(apps) { app, enable ->
            val result = accessibilityAppManager.setAppAccessibilityEnabled(app.packageName, enable)
            if (result) {
                Toast.makeText(this, "${app.appName} 已${if (enable) "开启" else "关闭"}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "设置失败", Toast.LENGTH_SHORT).show()
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = appsAdapter
        
        AlertDialog.Builder(this)
            .setTitle("应用无障碍权限")
            .setView(dialogView)
            .setPositiveButton("关闭") { _, _ ->
                appsAdapter = null
            }
            .show()
    }

    private fun updatePermissionStatus() {
        val secureSettings = hasWriteSecureSettings()

        updateStatus(binding.tvSecureSettingsStatus, secureSettings)

        binding.btnSystemSettings.isEnabled = secureSettings
        binding.btnAppAccessibility.isEnabled = secureSettings
    }

    private fun updateStatus(textView: TextView, enabled: Boolean) {
        textView.text = if (enabled) "已授予" else "未授予"
        textView.setTextColor(getColor(if (enabled) R.color.permission_granted else R.color.permission_denied))
    }
}
