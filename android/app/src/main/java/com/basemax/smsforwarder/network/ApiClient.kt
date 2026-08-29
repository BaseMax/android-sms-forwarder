package com.basemax.smsforwarder.network

import com.basemax.smsforwarder.core.TimeUtils
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    fun create(baseUrl: String, apiKey: String): SmsApi {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                // Where the phone is, said once per request rather than on
                // every message in it. Read at send time, so a handset that
                // crosses a border -- or a daylight-saving boundary -- is
                // describing itself as it is now, not as it was at install.
                val now = TimeUtils.nowMs()
                val request = chain.request().newBuilder()
                    .addHeader("X-API-Key", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-Tz-Offset", TimeUtils.offsetMinutesAt(now).toString())
                    .addHeader("X-Tz-Name", TimeUtils.zoneName())
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(normalize(baseUrl))
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SmsApi::class.java)
    }

    private fun normalize(url: String): String =
        if (url.endsWith("/")) url else "$url/"
}
