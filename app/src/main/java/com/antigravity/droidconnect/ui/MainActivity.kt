package com.antigravity.droidconnect.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import com.antigravity.droidconnect.AntiGravityApp
import com.antigravity.droidconnect.R
import com.antigravity.droidconnect.databinding.ActivityMainBinding
import com.antigravity.droidconnect.model.NotificationPayload
import com.antigravity.droidconnect.network.WebhookClient
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val prefs by lazy { AntiGravityApp.instance.preferences }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
        updateBatteryStatus()
        updateSummary()
    }

    private fun setupUI() {
        // Master switch
        binding.switchMaster.isChecked = prefs.isForwardingEnabled
        updateSwitchStatusLabel(prefs.isForwardingEnabled)

        binding.switchMaster.setOnCheckedChangeListener { _, isChecked ->
            prefs.isForwardingEnabled = isChecked
            updateSwitchStatusLabel(isChecked)
        }

        // Webhook settings
        binding.etWebhookUrl.setText(prefs.webhookUrl)
        binding.etEncryptionKey.setText(prefs.encryptionKey)

        // Save button
        binding.btnSaveConfig.setOnClickListener {
            val url = binding.etWebhookUrl.text?.toString()?.trim().orEmpty()
            val key = binding.etEncryptionKey.text?.toString()?.trim().orEmpty()

            if (url.isBlank()) {
                binding.layoutWebhookUrl.error = "Webhook URL cannot be empty"
                return@setOnClickListener
            }
            binding.layoutWebhookUrl.error = null

            prefs.webhookUrl = url
            prefs.encryptionKey = key

            Snackbar.make(binding.root, R.string.config_saved, Snackbar.LENGTH_SHORT).show()
        }

        // Test push button
        binding.btnSendTest.setOnClickListener {
            sendTestNotification()
        }

        // Notification permission button
        binding.btnGrantPermission.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        // Battery optimization button
        binding.btnBatteryOptimization.setOnClickListener {
            requestIgnoreBatteryOptimization()
        }

        // Manage Apps button
        binding.btnManageApps.setOnClickListener {
            startActivity(Intent(this, AppFilterActivity::class.java))
        }
    }

    private fun updateSwitchStatusLabel(isActive: Boolean) {
        if (isActive) {
            binding.tvServiceStatus.setText(R.string.forwarding_active)
            binding.tvServiceSubstatus.text = "Interception and relay active"
            binding.tvServiceStatus.setTextColor(getColor(R.color.on_surface))
        } else {
            binding.tvServiceStatus.setText(R.string.forwarding_paused)
            binding.tvServiceSubstatus.text = "Forwarding paused by user"
            binding.tvServiceStatus.setTextColor(getColor(R.color.on_surface_variant))
        }
    }

    private fun updatePermissionStatus() {
        val hasPermission = NotificationManagerCompat.getEnabledListenerPackages(this)
            .contains(packageName)

        if (hasPermission) {
            binding.layoutPermissionWarning.visibility = View.GONE
        } else {
            binding.layoutPermissionWarning.visibility = View.VISIBLE
            binding.tvPermissionWarning.setText(R.string.permission_missing)
        }
    }

    private fun updateBatteryStatus() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isIgnoring = powerManager?.isIgnoringBatteryOptimizations(packageName) == true

        if (isIgnoring) {
            binding.tvBatteryStatus.setText(R.string.battery_ignored)
            binding.tvBatteryStatus.setTextColor(getColor(R.color.status_green))
            binding.btnBatteryOptimization.visibility = View.GONE
        } else {
            binding.tvBatteryStatus.setText(R.string.battery_restricted)
            binding.tvBatteryStatus.setTextColor(getColor(R.color.status_amber))
            binding.btnBatteryOptimization.visibility = View.VISIBLE
        }
    }

    private fun updateSummary() {
        val count = prefs.selectedPackages.size
        val mode = prefs.filterMode.name.lowercase()
        binding.tvAppFilterSummary.text = "Filter mode: $mode ($count custom rules applied)"
    }

    private fun requestIgnoreBatteryOptimization() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (e: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (ignored: Exception) {
                Toast.makeText(this, "Unable to open battery settings", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendTestNotification() {
        val url = binding.etWebhookUrl.text?.toString()?.trim().orEmpty()
        val key = binding.etEncryptionKey.text?.toString()?.trim().orEmpty()

        if (url.isBlank()) {
            Snackbar.make(binding.root, "Please configure a valid Webhook URL first", Snackbar.LENGTH_LONG).show()
            return
        }

        binding.btnSendTest.isEnabled = false

        val testPayload = NotificationPayload(
            appName = "Anti Gravity",
            packageName = packageName,
            title = "Test Notification",
            message = "If you see this on your Mac/iPad, your notification pipeline is working!",
            priority = 4,
            tags = listOf("tada", "rocket")
        )

        lifecycleScope.launch {
            val result = WebhookClient.sendNotification(url, testPayload, key)
            binding.btnSendTest.isEnabled = true

            if (result.isSuccess) {
                Snackbar.make(binding.root, R.string.test_sent_success, Snackbar.LENGTH_SHORT).show()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                Snackbar.make(
                    binding.root,
                    getString(R.string.test_sent_failed, error),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }
}
