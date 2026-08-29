package com.basemax.smsforwarder.domain

import android.content.Context
import com.basemax.smsforwarder.core.AppLog
import com.basemax.smsforwarder.data.Settings
import com.basemax.smsforwarder.data.SmsRepository
import com.basemax.smsforwarder.data.model.SmsMessageDto
import com.basemax.smsforwarder.network.ApiClient
import com.basemax.smsforwarder.network.SmsApi
import kotlinx.coroutines.flow.first

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
        val stored = api.upload(messages).stored
        settings.addUploaded(stored)
        AppLog.i("Pushed ${messages.size} message(s); stored $stored")
        return stored
    }

    suspend fun sweep(): Int {
        val api = api() ?: return 0
        val since = settings.lastSyncMs.first()
        val device = settings.deviceId.first()

        var offset = 0
        var newestSeen = since
        var stored = 0
        while (true) {
            val batch = smsRepository.readSince(since, PAGE_SIZE, offset, device)
            if (batch.isEmpty()) break
            stored += api.upload(batch).stored
            newestSeen = maxOf(newestSeen, batch.maxOf { it.date.toLongOrNull() ?: 0L })
            offset += batch.size
            if (batch.size < PAGE_SIZE) break
        }
        if (newestSeen > since) settings.setLastSyncMs(newestSeen)
        settings.addUploaded(stored)
        AppLog.i("Sweep complete; stored $stored new message(s)")
        return stored
    }

    suspend fun testConnection(): Result<String> {
        val api = api()
            ?: return Result.failure(NotConfiguredException())
        return try {
            val status = api.health()["status"]?.toString() ?: "ok"
            Result.success(status)
        } catch (e: Exception) {
            AppLog.e("Connection test failed", e)
            Result.failure(e)
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
