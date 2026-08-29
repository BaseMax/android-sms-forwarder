package com.basemax.smsforwarder.network

import com.basemax.smsforwarder.data.model.IngestResponse
import com.basemax.smsforwarder.data.model.SmsMessageDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface SmsApi {

    @POST("api/sms")
    suspend fun upload(@Body messages: List<SmsMessageDto>): IngestResponse

    @GET("health")
    suspend fun health(): Map<String, Any>
}
