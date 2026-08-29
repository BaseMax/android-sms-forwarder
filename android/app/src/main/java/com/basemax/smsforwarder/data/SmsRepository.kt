package com.basemax.smsforwarder.data

import android.content.Context
import android.provider.Telephony
import com.basemax.smsforwarder.core.TimeUtils
import com.basemax.smsforwarder.data.model.SmsMessageDto
import java.time.ZoneId

/**
 * A batch of messages read out of the SMS provider, and the cursor to ask for
 * the next one with.
 *
 * The two are separate on purpose. [messages] carry normalised UTC
 * milliseconds -- what the server stores. [cursor] is the largest *raw*
 * `Telephony.Sms.DATE` the batch contained, in whatever units this device's
 * provider happens to use, because that is the only value the next query's
 * `DATE > ?` can be compared against. Feeding a normalised timestamp back
 * into that query on a device whose provider stores seconds would silently
 * match nothing and stop the sync forever.
 */
data class SmsPage(
    val messages: List<SmsMessageDto>,
    val cursor: Long,
)

class SmsRepository(private val context: Context) {

    fun readSince(
        sinceRaw: Long,
        limit: Int,
        offset: Int,
        device: String,
    ): SmsPage {
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
        )
        val selection = "${Telephony.Sms.DATE} > ?"
        val args = arrayOf(sinceRaw.toString())
        val sortOrder = "${Telephony.Sms.DATE} ASC LIMIT $limit OFFSET $offset"

        // Resolved once per batch: a thousand messages from one sweep all get
        // their offset from the same zone rules, and each still gets the
        // offset that applied at its own instant.
        val zone = ZoneId.systemDefault()
        val zoneName = TimeUtils.zoneName(zone)
        val now = TimeUtils.nowMs()

        val out = ArrayList<SmsMessageDto>(limit)
        var cursor = sinceRaw
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI, projection, selection, args, sortOrder,
        )?.use { c ->
            val iAddress = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val iBody = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val iDate = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val iType = c.getColumnIndexOrThrow(Telephony.Sms.TYPE)
            while (c.moveToNext()) {
                val raw = c.getLong(iDate)
                val date = TimeUtils.normalizeMs(raw, now)
                if (raw > cursor) cursor = raw
                out.add(
                    SmsMessageDto(
                        address = c.getString(iAddress) ?: "",
                        body = c.getString(iBody) ?: "",
                        date = date.toString(),
                        type = c.getInt(iType),
                        device = device,
                        tzOffsetMinutes = TimeUtils.offsetMinutesAt(date, zone),
                        tzName = zoneName,
                    )
                )
            }
        }
        return SmsPage(out, cursor)
    }
}
