package com.basemax.smsforwarder.network

import com.basemax.smsforwarder.data.model.IngestResponse
import com.basemax.smsforwarder.data.model.MessageCodec
import com.basemax.smsforwarder.data.model.SmsMessageDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class SmsApi internal constructor(
    private val baseUrl: String,
    private val apiKey: String,
) {

    suspend fun upload(messages: List<SmsMessageDto>): IngestResponse = withContext(Dispatchers.IO) {
        MessageCodec.decodeIngest(
            ApiClient.request(
                url = baseUrl + "api/sms",
                method = "POST",
                apiKey = apiKey,
                body = MessageCodec.encode(messages),
            )
        )
    }

    suspend fun health(): Map<String, Any> = withContext(Dispatchers.IO) {
        val json = JSONObject(
            ApiClient.request(url = baseUrl + "health", method = "GET", apiKey = apiKey, body = null)
        )
        json.keys().asSequence().associateWith { json.get(it) }
    }
}
