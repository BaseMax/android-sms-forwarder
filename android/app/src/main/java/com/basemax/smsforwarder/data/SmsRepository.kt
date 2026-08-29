package com.basemax.smsforwarder.data

import android.content.Context
import android.provider.Telephony
import com.basemax.smsforwarder.data.model.SmsMessageDto

class SmsRepository(private val context: Context) {

    fun readSince(
        sinceMs: Long,
        limit: Int,
        offset: Int,
        device: String,
    ): List<SmsMessageDto> {
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
        )
        val selection = "${Telephony.Sms.DATE} > ?"
        val args = arrayOf(sinceMs.toString())
        val sortOrder = "${Telephony.Sms.DATE} ASC LIMIT $limit OFFSET $offset"

        val out = ArrayList<SmsMessageDto>(limit)
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI, projection, selection, args, sortOrder,
        )?.use { c ->
            val iAddress = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val iBody = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val iDate = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val iType = c.getColumnIndexOrThrow(Telephony.Sms.TYPE)
            while (c.moveToNext()) {
                out.add(
                    SmsMessageDto(
                        address = c.getString(iAddress) ?: "",
                        body = c.getString(iBody) ?: "",
                        date = c.getLong(iDate).toString(),
                        type = c.getInt(iType),
                        device = device,
                    )
                )
            }
        }
        return out
    }
}
