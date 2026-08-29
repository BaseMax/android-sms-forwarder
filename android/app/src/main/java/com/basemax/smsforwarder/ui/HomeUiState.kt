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

    /**
     * True when this phone's clock is far enough from the server's to be
     * worth telling its owner about. A wrong date does not stop a backup, but
     * it does file every message under the wrong day, so it is the one clock
     * problem the app surfaces rather than silently correcting.
     */
    val clockIsOff: Boolean
        get() = abs(clockSkewMs) > TimeUtils.CLOCK_SKEW_TOLERANCE_MS
}
