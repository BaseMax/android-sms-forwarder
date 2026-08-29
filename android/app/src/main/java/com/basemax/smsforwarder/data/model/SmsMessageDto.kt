package com.basemax.smsforwarder.data.model

import com.squareup.moshi.Json

/**
 * One SMS on its way to the server.
 *
 * `date` is the instant the message happened, in UTC epoch milliseconds, as a
 * String -- a bare integer that large is truncated by the backend's JSON
 * decoder, so it travels quoted. It is an absolute point in time and carries
 * no timezone.
 *
 * `tzOffsetMinutes` and `tzName` say where the phone was when that instant
 * passed, so the server can show the wall-clock reading the owner saw without
 * ever having to reinterpret `date`. They describe the message; they do not
 * modify it. A phone that has since flown somewhere else does not change what
 * its older messages mean.
 *
 * `type`: 1 = inbox (received), 2 = sent.
 */
data class SmsMessageDto(
    val address: String,
    val body: String,
    val date: String,
    val type: Int,
    val device: String,
    @Json(name = "tz_offset_minutes") val tzOffsetMinutes: Int = 0,
    @Json(name = "tz_name") val tzName: String = "",
)

/**
 * What the server made of an upload. `serverTimeMs` is its clock, in UTC
 * epoch milliseconds, at the moment it answered -- the app compares that
 * against its own to notice a phone whose date is wrong, which is the one
 * time-related fault a phone cannot detect on its own.
 */
data class IngestResponse(
    val received: Int,
    val stored: Int,
    val duplicates: Int,
    @Json(name = "server_time_ms") val serverTimeMs: Long = 0L,
    @Json(name = "server_time_utc") val serverTimeUtc: String = "",
)
