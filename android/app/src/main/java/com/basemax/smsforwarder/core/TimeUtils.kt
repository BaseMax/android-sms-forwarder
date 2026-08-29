package com.basemax.smsforwarder.core

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object TimeUtils {

    private const val MS_FLOOR = 946_684_800_000L
    private const val SEC_FLOOR = MS_FLOOR / 1_000
    private const val US_FLOOR = MS_FLOOR * 1_000
    private const val NS_FLOOR = MS_FLOOR * 1_000_000

    private const val FUTURE_SLACK_MS = 86_400_000L

    const val CLOCK_SKEW_TOLERANCE_MS = 120_000L

    fun nowMs(): Long = System.currentTimeMillis()

    fun normalizeMs(raw: Long, nowMs: Long = nowMs()): Long {
        val ms = when {
            raw >= NS_FLOOR -> raw / 1_000_000
            raw >= US_FLOOR -> raw / 1_000
            raw >= MS_FLOOR -> raw
            raw >= SEC_FLOOR -> raw * 1_000
            else -> 0L
        }
        return if (ms < MS_FLOOR || ms > nowMs + FUTURE_SLACK_MS) nowMs else ms
    }

    fun offsetMinutesAt(ms: Long, zone: ZoneId = ZoneId.systemDefault()): Int =
        zone.rules.getOffset(Instant.ofEpochMilli(ms)).totalSeconds / 60

    fun zoneName(zone: ZoneId = ZoneId.systemDefault()): String = zone.id

    fun formatUtc(ms: Long): String =
        UTC_FORMAT.format(Instant.ofEpochMilli(ms).atOffset(ZoneOffset.UTC))

    fun formatForPeople(ms: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val at = ZonedDateTime.ofInstant(Instant.ofEpochMilli(ms), zone)
        return PEOPLE_FORMAT.format(at) + " " + ZONE_FORMAT.format(at)
    }

    private val UTC_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

    private val PEOPLE_FORMAT =
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

    private val ZONE_FORMAT = DateTimeFormatter.ofPattern("zzz", Locale.US)
}
