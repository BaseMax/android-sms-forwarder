package com.basemax.smsforwarder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.basemax.smsforwarder.data.model.SmsMessageDto
import com.basemax.smsforwarder.work.SyncScheduler

private const val TAG = "SmsForwarder"

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        try {
            val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (parts.isNullOrEmpty()) {
                Log.w(TAG, "SMS_RECEIVED with no messages")
                return
            }

            val bodies = LinkedHashMap<String, StringBuilder>()
            val timestamps = HashMap<String, Long>()
            for (part in parts) {
                val address = part.originatingAddress ?: part.displayOriginatingAddress ?: ""
                bodies.getOrPut(address) { StringBuilder() }
                    .append(part.displayMessageBody ?: part.messageBody ?: "")
                val ts = part.timestampMillis
                timestamps[address] = minOf(timestamps[address] ?: ts, ts)
            }

            val messages = bodies.map { (address, body) ->
                SmsMessageDto(
                    address = address,
                    body = body.toString(),
                    date = (timestamps[address] ?: System.currentTimeMillis()).toString(),
                    type = Telephony.Sms.MESSAGE_TYPE_INBOX,
                    device = "",
                )
            }

            Log.i(TAG, "Received ${messages.size} SMS; queueing upload")
            SyncScheduler.uploadIncoming(context.applicationContext, messages)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle incoming SMS", e)
        }
    }
}
