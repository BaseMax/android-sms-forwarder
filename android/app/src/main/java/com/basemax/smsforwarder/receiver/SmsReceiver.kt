package com.basemax.smsforwarder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.basemax.smsforwarder.core.AppLog
import com.basemax.smsforwarder.data.model.SmsMessageDto
import com.basemax.smsforwarder.work.SyncScheduler

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        try {
            val messages = parseMessages(intent)
            if (messages.isEmpty()) {
                AppLog.w("SMS_RECEIVED with no messages")
                return
            }
            AppLog.i("Received ${messages.size} SMS; queueing upload")
            SyncScheduler.uploadIncoming(context.applicationContext, messages)
        } catch (e: Exception) {
            AppLog.e("Failed to handle incoming SMS", e)
        }
    }

    private fun parseMessages(intent: Intent): List<SmsMessageDto> {
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return emptyList()
        val bodies = LinkedHashMap<String, StringBuilder>()
        val timestamps = HashMap<String, Long>()
        for (part in parts) {
            val address = part.originatingAddress ?: part.displayOriginatingAddress ?: ""
            bodies.getOrPut(address) { StringBuilder() }
                .append(part.displayMessageBody ?: part.messageBody ?: "")
            val ts = part.timestampMillis
            timestamps[address] = minOf(timestamps[address] ?: ts, ts)
        }
        return bodies.map { (address, body) ->
            SmsMessageDto(
                address = address,
                body = body.toString(),
                date = (timestamps[address] ?: System.currentTimeMillis()).toString(),
                type = Telephony.Sms.MESSAGE_TYPE_INBOX,
                device = "",
            )
        }
    }
}
