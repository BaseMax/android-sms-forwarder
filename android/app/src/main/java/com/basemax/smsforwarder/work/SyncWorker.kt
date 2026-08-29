package com.basemax.smsforwarder.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.basemax.smsforwarder.core.AppLog
import com.basemax.smsforwarder.data.model.MessageCodec
import com.basemax.smsforwarder.domain.BackupManager

class SyncWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val backup = BackupManager(applicationContext)
        if (!backup.isConfigured()) {
            AppLog.i("Not configured (no server URL or API key); skipping")
            return Result.success()
        }
        return try {
            val queued = inputData.getString(KEY_MESSAGES)
            if (queued != null) {
                backup.uploadMessages(MessageCodec.decode(queued))
            } else {
                backup.sweep()
            }
            Result.success()
        } catch (e: SecurityException) {
            AppLog.e("READ_SMS permission missing; cannot read messages", e)
            Result.success()
        } catch (e: Exception) {
            AppLog.e("Upload failed; will retry with backoff", e)
            Result.retry()
        }
    }

    companion object {
        const val KEY_MESSAGES = "messages_json"
    }
}
