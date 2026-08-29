package com.basemax.smsforwarder.domain

import android.content.Context
import com.basemax.smsforwarder.core.AppLog
import com.basemax.smsforwarder.core.TimeUtils
import com.basemax.smsforwarder.data.Settings
import com.basemax.smsforwarder.data.SmsRepository
import com.basemax.smsforwarder.data.model.IngestResponse
import com.basemax.smsforwarder.data.model.SmsMessageDto
import com.basemax.smsforwarder.network.ApiClient
import com.basemax.smsforwarder.network.SmsApi
import kotlinx.coroutines.flow.first
import kotlin.math.abs

class BackupManager(
    context: Context,
    private val settings: Settings = Settings(context),
    private val smsRepository: SmsRepository = SmsRepository(context),
) {

    suspend fun isConfigured(): Boolean =
        settings.baseUrl.first().isNotBlank() && settings.apiKey.first().isNotBlank()

    suspend fun uploadMessages(messages: List<SmsMessageDto>): Int {
        if (messages.isEmpty()) return 0
        val api = api() ?: return 0
        val sentAt = TimeUtils.nowMs()
        val response = api.upload(messages)
        recordSync(response, sentAt)
        settings.addUploaded(response.stored)
        AppLog.i("Pushed ${messages.size} message(s); stored ${response.stored}")
        return response.stored
    }

    suspend fun sweep(): Int {
        val api = api() ?: return 0
        val startCursor = settings.syncCursor.first()
        val device = settings.deviceId.first()

        var offset = 0
        var newest = startCursor
        var stored = 0
        while (true) {
            val page = smsRepository.readSince(startCursor, PAGE_SIZE, offset, device)
            if (page.messages.isEmpty()) break
            val sentAt = TimeUtils.nowMs()
            val response = api.upload(page.messages)
            recordSync(response, sentAt)
            stored += response.stored
            newest = maxOf(newest, page.cursor)
            offset += page.messages.size
            if (page.messages.size < PAGE_SIZE) break
        }
        if (newest > startCursor) settings.setSyncCursor(newest)
        settings.setLastSyncAt(TimeUtils.nowMs())
        settings.addUploaded(stored)
        AppLog.i("Sweep complete; stored $stored new message(s)")
        return stored
    }

    suspend fun testConnection(): Result<String> {
        val api = api()
            ?: return Result.failure(NotConfiguredException())
        return try {
            val health = api.health()
            val status = health["status"]?.toString() ?: "ok"
            Result.success(status)
        } catch (e: Exception) {
            AppLog.e("Connection test failed", e)
            Result.failure(e)
        }
    }

    private suspend fun recordSync(response: IngestResponse, sentAtMs: Long) {
        settings.setLastSyncAt(TimeUtils.nowMs())
        if (response.serverTimeMs <= 0L) return
        val skew = response.serverTimeMs - sentAtMs
        settings.setClockSkewMs(skew)
        if (abs(skew) > TimeUtils.CLOCK_SKEW_TOLERANCE_MS) {
            AppLog.w(
                "This phone's clock is ${skew / 1000}s from the server's " +
                    "(server says ${response.serverTimeUtc}); timestamps may be filed wrong"
            )
        }
    }

    private suspend fun api(): SmsApi? {
        val base = settings.baseUrl.first()
        val key = settings.apiKey.first()
        if (base.isBlank() || key.isBlank()) return null
        return ApiClient.create(base, key)
    }

    class NotConfiguredException :
        Exception("Enter the server URL and API key, then tap Save first.")

    companion object {
        private const val PAGE_SIZE = 500
    }
}
