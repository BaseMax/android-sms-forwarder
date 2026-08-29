package com.basemax.smsforwarder

import android.content.ActivityNotFoundException
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.basemax.smsforwarder.core.AppLog
import com.basemax.smsforwarder.core.Permissions
import com.basemax.smsforwarder.service.PowerSettings
import com.basemax.smsforwarder.service.ServiceController
import com.basemax.smsforwarder.ui.HomeScreen
import com.basemax.smsforwarder.ui.MainViewModel
import com.basemax.smsforwarder.ui.theme.SmsForwarderTheme
import com.basemax.smsforwarder.work.SyncScheduler

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshDeviceStatus()
        if (Permissions.hasSmsAccess(this)) {
            ServiceController.start(this)
            SyncScheduler.syncNow(this)
        } else {
            viewModel.showMessage("SMS permission is needed to back up your messages")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ServiceController.start(this)
        refreshDeviceStatus()

        setContent {
            SmsForwarderTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val state by viewModel.state.collectAsStateWithLifecycle()
                    HomeScreen(
                        state = state,
                        onBaseUrlChange = viewModel::onBaseUrlChange,
                        onApiKeyChange = viewModel::onApiKeyChange,
                        onDeviceIdChange = viewModel::onDeviceIdChange,
                        onRequestPermissions = { permissionLauncher.launch(Permissions.required) },
                        onSave = viewModel::save,
                        onTest = viewModel::testConnection,
                        onSyncNow = viewModel::syncNow,
                        onFixBattery = ::requestBatteryExemption,
                        onMessageShown = viewModel::consumeMessage,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshDeviceStatus()
    }

    private fun refreshDeviceStatus() {
        viewModel.updateDeviceStatus(
            hasPermission = Permissions.hasSmsAccess(this),
            serviceRunning = ServiceController.isRunning(this),
            ignoringBattery = PowerSettings.isIgnoringBatteryOptimizations(this),
        )
    }

    private fun requestBatteryExemption() {
        try {
            startActivity(PowerSettings.requestExemptionIntent(this))
        } catch (e: ActivityNotFoundException) {
            AppLog.w("Direct battery-exemption prompt unavailable, opening settings list", e)
            try {
                startActivity(PowerSettings.settingsIntent())
            } catch (e2: ActivityNotFoundException) {
                AppLog.e("No battery-optimisation screen on this device", e2)
                viewModel.showMessage("Battery settings are not available on this device")
            }
        }
    }
}
