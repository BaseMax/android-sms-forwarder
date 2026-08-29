package com.basemax.smsforwarder.data.model

// date: epoch milliseconds as a String (a bare integer that large is truncated
// by the backend's JSON decoder). type: 1 = inbox (received), 2 = sent.
data class SmsMessageDto(
    val address: String,
    val body: String,
    val date: String,
    val type: Int,
    val device: String,
)

data class IngestResponse(
    val received: Int,
    val stored: Int,
    val duplicates: Int,
)
