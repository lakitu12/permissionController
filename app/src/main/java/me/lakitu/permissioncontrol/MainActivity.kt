package me.lakitu.permissioncontrol

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.lakitu.permissioncontrol.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var systemSettingsController: SystemSettingsController
    private lateinit var accessibilityAppManager: AccessibilityAppManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
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
            adb shell pm grant me.lakitu.permissioncontrol android.permission.WRITE_SECURE_SETTINGS
        """.trimIndent()
        AlertDialog.Builder(this)
            .setTitle("需要权限")
            .setMessage(message)
            .setPositiveButton("确定", null)
            .show()
    }

    private fun showSystemSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings_list, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recycler_settings)
        
        val settings = systemSettingsController.getAllSettings()
        val adapter = SettingsAdapter(settings) { setting, enable ->
            val result = setting.setValue(enable)
            if (result) {
                Toast.makeText(this, "${setting.name} 已${if (enable) "开启" else "关闭"}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "${setting.name} 设置失败", Toast.LENGTH_SHORT).show()
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        AlertDialog.Builder(this)
            .setTitle("系统设置")
            .setView(dialogView)
            .setPositiveButton("关闭", null)
            .show()
    }

    private fun showAppsAccessibilityDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_apps_list, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recycler_apps)
        
        val apps = accessibilityAppManager.getAllAppsWithAccessibilityService()
        val adapter = AppsAdapter(apps) { app, enable ->
            val result = accessibilityAppManager.setAppAccessibilityEnabled(app.packageName, enable)
            if (result) {
                Toast.makeText(this, "${app.appName} 已${if (enable) "开启" else "关闭"}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "设置失败", Toast.LENGTH_SHORT).show()
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        AlertDialog.Builder(this)
            .setTitle("应用无障碍权限")
            .setView(dialogView)
            .setPositiveButton("关闭", null)
            .show()
    }

    private fun updatePermissionStatus() {
        val secureSettings = hasWriteSecureSettings()

        updateStatus(binding.tvSecureSettingsStatus, binding.btnSecureSettings, secureSettings)

        binding.btnSystemSettings.isEnabled = secureSettings
        binding.btnAppAccessibility.isEnabled = secureSettings
    }

    private fun updateStatus(textView: TextView, button: Button, enabled: Boolean) {
        textView.text = if (enabled) "已授予" else "未授予"
        textView.setTextColor(getColor(if (enabled) R.color.permission_granted else R.color.permission_denied))
    }
}
