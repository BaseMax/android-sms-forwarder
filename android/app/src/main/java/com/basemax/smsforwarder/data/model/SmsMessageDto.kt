package com.basemax.smsforwarder.data.model

import com.squareup.moshi.Json

data class SmsMessageDto(
    val address: String,
    val body: String,
    val date: String,
    val type: Int,
    val device: String,
    @Json(name = "tz_offset_minutes") val tzOffsetMinutes: Int = 0,
    @Json(name = "tz_name") val tzName: String = "",
)

data class IngestResponse(
    val received: Int,
    val stored: Int,
    val duplicates: Int,
    @Json(name = "server_time_ms") val serverTimeMs: Long = 0L,
    @Json(name = "server_time_utc") val serverTimeUtc: String = "",
)
