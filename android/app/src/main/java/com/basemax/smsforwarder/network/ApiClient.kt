package com.basemax.smsforwarder.network

import com.basemax.smsforwarder.core.AppLog
import com.basemax.smsforwarder.core.TimeUtils
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * The HTTP plumbing behind [SmsApi].
 *
 * Two JSON endpoints do not pay for Retrofit + Moshi + kotlin-reflect + OkHttp,
 * which between them are several megabytes of the APK. HttpURLConnection and
 * org.json are part of Android, so they cost nothing to ship - and on Android
 * HttpURLConnection is itself backed by OkHttp inside the platform.
 */
object ApiClient {

    private const val TIMEOUT_MS = 30_000

    fun create(baseUrl: String, apiKey: String): SmsApi = SmsApi(normalize(baseUrl), apiKey)

    /**
     * Runs one request and returns the response body. Blocking - [SmsApi] keeps
     * it on the IO dispatcher.
     */
    internal fun request(url: String, method: String, apiKey: String, body: String?): String {
        val started = TimeUtils.nowMs()
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("X-API-Key", apiKey)
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("X-Tz-Offset", TimeUtils.offsetMinutesAt(started).toString())
            setRequestProperty("X-Tz-Name", TimeUtils.zoneName())
        }
        try {
            AppLog.i("--> $method $url")
            if (body != null) {
                val bytes = body.toByteArray(Charsets.UTF_8)
                connection.doOutput = true
                connection.setFixedLengthStreamingMode(bytes.size)
                connection.outputStream.use { it.write(bytes) }
            }
            val code = connection.responseCode
            val ok = code in 200..299
            val stream = if (ok) connection.inputStream else connection.errorStream
            val text = stream?.use { it.reader(Charsets.UTF_8).readText() }.orEmpty()
            AppLog.i("<-- $code $method $url (${TimeUtils.nowMs() - started}ms)")
            if (!ok) throw IOException("HTTP $code from $url: ${text.take(200)}")
            return text
        } finally {
            connection.disconnect()
        }
    }

    private fun normalize(url: String): String =
        if (url.endsWith("/")) url else "$url/"
}
