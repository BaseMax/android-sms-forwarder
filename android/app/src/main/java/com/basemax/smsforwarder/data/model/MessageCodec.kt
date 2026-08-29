package com.basemax.smsforwarder.data.model

import com.basemax.smsforwarder.core.AppLog
import org.json.JSONArray
import org.json.JSONObject

object MessageCodec {

    fun encode(messages: List<SmsMessageDto>): String {
        val array = JSONArray()
        for (message in messages) array.put(message.toJson())
        return array.toString()
    }

    fun decode(json: String): List<SmsMessageDto> = try {
        val array = JSONArray(json)
        val out = ArrayList<SmsMessageDto>(array.length())
        for (i in 0 until array.length()) out.add(array.getJSONObject(i).toMessage())
        out
    } catch (e: Exception) {
        AppLog.e("Could not parse queued messages: $json", e)
        emptyList()
    }

    fun decodeIngest(json: String): IngestResponse = with(JSONObject(json)) {
        IngestResponse(
            received = optInt("received"),
            stored = optInt("stored"),
            duplicates = optInt("duplicates"),
            serverTimeMs = optLong("server_time_ms"),
            serverTimeUtc = optString("server_time_utc"),
        )
    }

    private fun SmsMessageDto.toJson(): JSONObject = JSONObject()
        .put("address", address)
        .put("body", body)
        .put("date", date)
        .put("type", type)
        .put("device", device)
        .put("tz_offset_minutes", tzOffsetMinutes)
        .put("tz_name", tzName)

    private fun JSONObject.toMessage() = SmsMessageDto(
        address = optString("address"),
        body = optString("body"),
        date = optString("date"),
        type = optInt("type"),
        device = optString("device"),
        tzOffsetMinutes = optInt("tz_offset_minutes"),
        tzName = optString("tz_name"),
    )
}
