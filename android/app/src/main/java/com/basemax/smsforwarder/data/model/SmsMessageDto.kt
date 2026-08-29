package com.basemax.smsforwarder.data.model

data class SmsMessageDto(
    val address: String,
    val body: String,
    val date: String,
    val type: Int,
    val device: String,
    val tzOffsetMinutes: Int = 0,
    val tzName: String = "",
)

data class IngestResponse(
    val received: Int,
    val stored: Int,
    val duplicates: Int,
    val serverTimeMs: Long = 0L,
    val serverTimeUtc: String = "",
)
