package com.basemax.smsforwarder.core

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * The app's half of the time contract, and the mirror of the backend's
 * clock.salam.
 *
 * THE RULE: every instant that leaves this phone is UTC epoch milliseconds.
 * That is already what Android hands us -- [android.provider.Telephony.Sms.DATE]
 * and `SmsMessage.timestampMillis` are absolute instants, not wall-clock
 * readings -- so nothing is converted on the way out. The phone's timezone is
 * *reported* alongside, never applied.
 *
 * That distinction is the whole point. Two handsets on opposite sides of the
 * world receive the same broadcast SMS: both upload the same number, both
 * de-duplicate onto one row on the server, and each still knows what its own
 * clock read at the time, because the offset travelled with the message
 * instead of being baked into it.
 *
 * Local time appears in exactly one place -- the screen -- and [formatForPeople]
 * is the only function that produces it.
 */
object TimeUtils {

    /**
     * 2000-01-01T00:00:00Z, in each of the four units a timestamp might turn
     * up in. The bands do not overlap, which is what lets [normalizeMs] tell
     * them apart. These are the same four constants the backend uses.
     */
    private const val MS_FLOOR = 946_684_800_000L
    private const val SEC_FLOOR = MS_FLOOR / 1_000
    private const val US_FLOOR = MS_FLOOR * 1_000
    private const val NS_FLOOR = MS_FLOOR * 1_000_000

    /** A timestamp more than a day ahead of this phone is a broken clock. */
    private const val FUTURE_SLACK_MS = 86_400_000L

    /** Beyond this, phone and server disagree enough to be worth saying so. */
    const val CLOCK_SKEW_TOLERANCE_MS = 120_000L

    fun nowMs(): Long = System.currentTimeMillis()

    /**
     * A timestamp from anywhere, as UTC epoch milliseconds.
     *
     * Almost every SMS reaches us already correct and passes through
     * untouched. The two cases that do not are worth the check: a few
     * providers and restore tools write the SMS provider's `date` column in
     * seconds, which read as milliseconds would land every message in January
     * 1970; and a phone whose clock was never set reports a date that would
     * sort and de-duplicate wrongly forever after.
     *
     * Anything unplaceable, or more than [FUTURE_SLACK_MS] ahead, becomes
     * [nowMs] -- the same fallback, and the same thresholds, as the server, so
     * the two ends of the wire never disagree about what a number meant.
     */
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

    /**
     * This phone's UTC offset in minutes at [ms] -- 210 for Tehran, -300 for
     * New York in winter, -240 for New York in summer.
     *
     * Deliberately taken at the instant of the message and not "now": a
     * message received before a daylight-saving change belongs to the offset
     * that was in force then, and a phone that flew to another country last
     * week should not relabel the SMS it received at home.
     */
    fun offsetMinutesAt(ms: Long, zone: ZoneId = ZoneId.systemDefault()): Int =
        zone.rules.getOffset(Instant.ofEpochMilli(ms)).totalSeconds / 60

    /** The IANA name of this phone's zone, e.g. "Asia/Tehran". */
    fun zoneName(zone: ZoneId = ZoneId.systemDefault()): String = zone.id

    /** RFC 3339 in UTC: "2026-08-29T12:34:56.789Z". Never shown to a user. */
    fun formatUtc(ms: Long): String =
        UTC_FORMAT.format(Instant.ofEpochMilli(ms).atOffset(ZoneOffset.UTC))

    /**
     * The one function that renders local time, and the only place in the app
     * where a timezone reaches the screen: "29 Aug 2026, 16:04 +0330".
     *
     * The zone is named rather than left implicit, because the reader may
     * well have travelled since the timestamp was recorded, and a bare
     * "16:04" would not tell them whose 16:04 it was.
     */
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
