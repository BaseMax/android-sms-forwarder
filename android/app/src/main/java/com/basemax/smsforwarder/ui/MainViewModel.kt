package com.basemax.smsforwarder.ui

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.basemax.smsforwarder.core.AppLog
import com.basemax.smsforwarder.data.Settings
import com.basemax.smsforwarder.domain.BackupManager
import com.basemax.smsforwarder.work.SyncScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = Settings(app)
    private val backup = BackupManager(app)

    private val form = MutableStateFlow(Form())
    private val device = MutableStateFlow(Device())
    private val message = MutableStateFlow<String?>(null)

    // The three stored values are folded together first: `combine` is typed
    // up to five sources, and the form, the device state and the transient
    // message already claim three of them.
    private val stored = combine(
        settings.uploadedTotal, settings.lastSyncMs, settings.clockSkewMs,
    ) { uploaded, lastSync, skew -> Stored(uploaded, lastSync, skew) }

    val state: StateFlow<HomeUiState> = combine(
        stored, form, device, message,
    ) { stored, form, device, message ->
        HomeUiState(
            baseUrl = form.baseUrl,
            apiKey = form.apiKey,
            deviceId = form.deviceId,
            uploaded = stored.uploaded,
            lastSyncMs = stored.lastSyncMs,
            clockSkewMs = stored.clockSkewMs,
            hasPermission = device.hasPermission,
            serviceRunning = device.serviceRunning,
            ignoringBattery = device.ignoringBattery,
            message = message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch {
            form.value = Form(
                baseUrl = settings.baseUrl.first(),
                apiKey = settings.apiKey.first(),
                deviceId = settings.deviceId.first().ifBlank { Build.MODEL ?: "" },
            )
        }
    }

    fun onBaseUrlChange(value: String) = form.update { it.copy(baseUrl = value) }
    fun onApiKeyChange(value: String) = form.update { it.copy(apiKey = value) }
    fun onDeviceIdChange(value: String) = form.update { it.copy(deviceId = value) }

    fun save() {
        val current = form.value
        viewModelScope.launch {
            try {
                settings.setConfig(current.baseUrl, current.apiKey, current.deviceId)
                message.value = "Saved"
            } catch (e: Exception) {
                AppLog.e("Failed to save settings", e)
                message.value = "Could not save settings: ${e.message ?: "unknown error"}"
            }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            message.value = backup.testConnection().fold(
                onSuccess = { status -> "Connected, server says \"$status\"" },
                onFailure = { error -> error.message ?: "Connection failed" },
            )
        }
    }

    fun syncNow() {
        try {
            SyncScheduler.syncNow(getApplication())
            message.value = "Sync started"
        } catch (e: Exception) {
            AppLog.e("Could not enqueue sync", e)
            message.value = "Could not start sync: ${e.message ?: "unknown error"}"
        }
    }

    fun showMessage(text: String) {
        message.value = text
    }

    fun consumeMessage() {
        message.value = null
    }

    fun updateDeviceStatus(hasPermission: Boolean, serviceRunning: Boolean, ignoringBattery: Boolean) {
        device.value = Device(hasPermission, serviceRunning, ignoringBattery)
    }

    private data class Stored(
        val uploaded: Int = 0,
        val lastSyncMs: Long = 0L,
        val clockSkewMs: Long = 0L,
    )

    private data class Form(
        val baseUrl: String = "",
        val apiKey: String = "",
        val deviceId: String = "",
    )

    private data class Device(
        val hasPermission: Boolean = false,
        val serviceRunning: Boolean = false,
        val ignoringBattery: Boolean = false,
    )
}
