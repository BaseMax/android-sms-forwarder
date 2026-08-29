package com.basemax.smsforwarder.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.basemax.smsforwarder.data.Settings
import com.basemax.smsforwarder.data.SmsRepository
import com.basemax.smsforwarder.data.model.SmsMessageDto
import com.basemax.smsforwarder.network.ApiClient
import com.basemax.smsforwarder.network.SmsApi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.first

class SyncWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = Settings(applicationContext)
        val baseUrl = settings.baseUrl.first()
        val apiKey = settings.apiKey.first()
        if (baseUrl.isBlank() || apiKey.isBlank()) {
            Log.i(TAG, "Not configured (no server URL or API key); skipping")
            return Result.success()
        }
        val api = ApiClient.create(baseUrl, apiKey)

        return try {
            val direct = inputData.getString(KEY_MESSAGES)
            if (direct != null) {
                uploadDirect(direct, api, settings)
            } else {
                sweepProvider(api, settings)
            }
            Result.success()
        } catch (se: SecurityException) {
            Log.e(TAG, "READ_SMS permission missing; cannot read messages", se)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed; will retry with backoff", e)
            Result.retry()
        }
    }

    private suspend fun uploadDirect(json: String, api: SmsApi, settings: Settings) {
        val list = try {
            messageListAdapter.fromJson(json)
        } catch (e: Exception) {
            Log.e(TAG, "Could not parse queued messages: $json", e)
            null
        } ?: return
        if (list.isEmpty()) return
        val resp = api.upload(list)
        settings.addUploaded(resp.stored)
        Log.i(TAG, "Pushed ${list.size} received message(s); stored ${resp.stored}")
    }

    private suspend fun sweepProvider(api: SmsApi, settings: Settings) {
        val since = settings.lastSyncMs.first()
        val device = settings.deviceId.first()
        val repo = SmsRepository(applicationContext)

        var offset = 0
        var maxDate = since
        var storedTotal = 0
        while (true) {
            val batch = repo.readSince(since, PAGE, offset, device)
            if (batch.isEmpty()) break
            val resp = api.upload(batch)
            storedTotal += resp.stored
            for (m in batch) {
                val d = m.date.toLongOrNull() ?: 0L
                if (d > maxDate) maxDate = d
            }
            offset += batch.size
            if (batch.size < PAGE) break
        }
        if (maxDate > since) settings.setLastSyncMs(maxDate)
        settings.addUploaded(storedTotal)
        Log.i(TAG, "Sweep complete; stored $storedTotal new message(s)")
    }

    companion object {
        const val KEY_MESSAGES = "messages_json"
        private const val TAG = "SmsForwarder"
        private const val PAGE = 500

        private val messageListAdapter by lazy {
            val type = Types.newParameterizedType(List::class.java, SmsMessageDto::class.java)
            ApiClient.moshi.adapter<List<SmsMessageDto>>(type)
        }

        fun encodeMessages(messages: List<SmsMessageDto>): String =
            messageListAdapter.toJson(messages)
    }
}
