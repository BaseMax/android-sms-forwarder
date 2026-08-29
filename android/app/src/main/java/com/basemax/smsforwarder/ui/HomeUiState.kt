package com.basemax.smsforwarder.ui

import com.basemax.smsforwarder.core.TimeUtils
import kotlin.math.abs

data class HomeUiState(
    val baseUrl: String = "",
    val apiKey: String = "",
    val deviceId: String = "",
    val uploaded: Int = 0,
    val lastSyncMs: Long = 0L,
    val clockSkewMs: Long = 0L,
    val hasPermission: Boolean = false,
    val serviceRunning: Boolean = false,
    val ignoringBattery: Boolean = false,
    val message: String? = null,
) {
    val isConfigured: Boolean get() = baseUrl.isNotBlank() && apiKey.isNotBlank()

    val clockIsOff: Boolean
        get() = abs(clockSkewMs) > TimeUtils.CLOCK_SKEW_TOLERANCE_MS
}
