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
                Toast.makeText(this, R.string.grant_permission_first, Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnAppAccessibility.setOnClickListener {
            if (hasWriteSecureSettings()) {
                showAppsAccessibilityDialog()
            } else {
                Toast.makeText(this, R.string.grant_permission_first, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showSecureSettingsInfo() {
        AlertDialog.Builder(this)
            .setTitle(R.string.permission_status)
            .setMessage(R.string.permission_granted_message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun showGrantSecureSettingsDialog() {
        val copyText = "adb shell pm grant me.lakitu.permissioncontroller android.permission.WRITE_SECURE_SETTINGS"
        val message = getString(R.string.grant_permission_message) + "\n\n" +
                getString(R.string.grant_permission_instruction) + "\n" +
                copyText

        val dialogView = layoutInflater.inflate(R.layout.dialog_permission_message_copy, null)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tv_message)
        tvMessage.text = message
        tvMessage.setTextIsSelectable(true)

        AlertDialog.Builder(this)
            .setTitle(R.string.permission_required)
            .setView(dialogView)
            .setPositiveButton(R.string.ok, null)
            .setNeutralButton(R.string.copy) { _, _ ->
                @Suppress("UnnecessaryQualifiedReference")
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("permission_grant_command", copyText)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
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
                    getString(R.string.app_name) + " " + if (result.enabled) getString(R.string.enabled) else getString(R.string.disabled)
                is SystemSettingsController.SettingResult.Error -> 
                    "${setting.name} " + getString(R.string.setting_failed) + ": ${result.message}"
                is SystemSettingsController.SettingResult.WifiDisabled -> 
                    getString(R.string.enable_wifi_first)
                is SystemSettingsController.SettingResult.WifiNotConnected -> 
                    getString(R.string.connect_wifi_first)
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            result
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = settingsAdapter
        
        AlertDialog.Builder(this)
            .setTitle(R.string.system_settings)
            .setView(dialogView)
            .setPositiveButton(R.string.close) { _, _ ->
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
                Toast.makeText(this, "${app.appName} ${if (enable) getString(R.string.enabled) else getString(R.string.disabled)}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, R.string.setting_failed, Toast.LENGTH_SHORT).show()
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = appsAdapter
        
        AlertDialog.Builder(this)
            .setTitle(R.string.app_accessibility)
            .setView(dialogView)
            .setPositiveButton(R.string.close) { _, _ ->
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
        textView.text = if (enabled) getString(R.string.granted) else getString(R.string.not_granted)
        textView.setTextColor(getColor(if (enabled) R.color.permission_granted else R.color.permission_denied))
    }
}
