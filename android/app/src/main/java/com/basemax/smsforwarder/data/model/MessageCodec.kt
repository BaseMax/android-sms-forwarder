package com.basemax.smsforwarder.data.model

import com.basemax.smsforwarder.core.AppLog
import com.basemax.smsforwarder.network.ApiClient
import com.squareup.moshi.Types

object MessageCodec {

    private val adapter by lazy {
        val type = Types.newParameterizedType(List::class.java, SmsMessageDto::class.java)
        ApiClient.moshi.adapter<List<SmsMessageDto>>(type)
    }

    fun encode(messages: List<SmsMessageDto>): String = adapter.toJson(messages)

    fun decode(json: String): List<SmsMessageDto> = try {
        adapter.fromJson(json) ?: emptyList()
    } catch (e: Exception) {
        AppLog.e("Could not parse queued messages: $json", e)
        emptyList()
    }
}
