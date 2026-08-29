package com.basemax.smsforwarder

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.basemax.smsforwarder.data.Settings
import com.basemax.smsforwarder.network.ApiClient
import com.basemax.smsforwarder.service.PowerSettings
import com.basemax.smsforwarder.service.ServiceController
import com.basemax.smsforwarder.ui.HomeScreen
import com.basemax.smsforwarder.ui.theme.SmsForwarderTheme
import com.basemax.smsforwarder.work.SyncScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "SmsForwarder"

class MainActivity : ComponentActivity() {

    private lateinit var settings: Settings
    private val hasPermission = mutableStateOf(false)
    private val serviceRunning = mutableStateOf(false)
    private val ignoringBattery = mutableStateOf(false)

    private val requiredPermissions: Array<String> = buildList {
        add(Manifest.permission.READ_SMS)
        add(Manifest.permission.RECEIVE_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasPermission.value = smsGranted()
        if (hasPermission.value) {
            ServiceController.start(this)
            SyncScheduler.syncNow(this)
        } else {
            val denied = result.filterValues { !it }.keys
            Log.w(TAG, "Permissions not granted: $denied")
            toast("SMS permission is needed to back up your messages")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = Settings(applicationContext)
        refreshState()
        ServiceController.start(this)

        setContent {
            SmsForwarderTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    HomeScreen(
                        settings = settings,
                        hasPermission = hasPermission.value,
                        serviceRunning = serviceRunning.value,
                        ignoringBattery = ignoringBattery.value,
                        onRequestPermissions = { permissionLauncher.launch(requiredPermissions) },
                        onSave = { baseUrl, apiKey, deviceId ->
                            lifecycleScope.launch {
                                try {
                                    settings.setConfig(baseUrl, apiKey, deviceId)
                                    ServiceController.start(this@MainActivity)
                                    toast("Saved")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to save settings", e)
                                    toast("Could not save settings: ${e.message ?: "unknown error"}")
                                }
                            }
                        },
                        onTestConnection = { testConnection() },
                        onSyncNow = { syncNow() },
                        onFixBattery = { requestBatteryExemption() },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshState()
    }

    private fun refreshState() {
        hasPermission.value = smsGranted()
        serviceRunning.value = ServiceController.isRunning(this)
        ignoringBattery.value = PowerSettings.isIgnoringBatteryOptimizations(this)
    }

    private fun syncNow() {
        try {
            SyncScheduler.syncNow(this)
        } catch (e: Exception) {
            Log.e(TAG, "Could not enqueue sync", e)
            toast("Could not start sync: ${e.message ?: "unknown error"}")
        }
    }

    private fun requestBatteryExemption() {
        try {
            startActivity(PowerSettings.requestExemptionIntent(this))
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Direct battery-exemption prompt unavailable, opening settings list", e)
            try {
                startActivity(PowerSettings.settingsIntent())
            } catch (e2: ActivityNotFoundException) {
                Log.e(TAG, "No battery-optimisation screen on this device", e2)
                toast("Battery settings are not available on this device")
            }
        }
    }

    private fun smsGranted(): Boolean =
        granted(Manifest.permission.READ_SMS) && granted(Manifest.permission.RECEIVE_SMS)

    private fun granted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private suspend fun testConnection(): String = try {
        val base = settings.baseUrl.first()
        val key = settings.apiKey.first()
        if (base.isBlank() || key.isBlank()) {
            "Enter the server URL and API key, then tap Save first."
        } else {
            val status = ApiClient.create(base, key).health()["status"] ?: "ok"
            "Connected — server says \"$status\""
        }
    } catch (e: Exception) {
        Log.e(TAG, "Connection test failed", e)
        "Connection failed: ${e.message ?: e.javaClass.simpleName}"
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
