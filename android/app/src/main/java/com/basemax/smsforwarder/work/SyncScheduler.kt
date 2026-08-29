package com.basemax.smsforwarder.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.basemax.smsforwarder.data.model.SmsMessageDto
import java.util.concurrent.TimeUnit

object SyncScheduler {

    private const val DAILY = "sms_daily_sync"
    private const val SYNC_NOW = "sms_sync_now"

    private val networkConstraint = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.DAYS)
            .setConstraints(networkConstraint)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DAILY, ExistingPeriodicWorkPolicy.KEEP, request,
        )
    }

    fun syncNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(networkConstraint)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            SYNC_NOW, ExistingWorkPolicy.REPLACE, request,
        )
    }

    fun uploadIncoming(context: Context, messages: List<SmsMessageDto>) {
        if (messages.isEmpty()) return
        val data = Data.Builder()
            .putString(SyncWorker.KEY_MESSAGES, SyncWorker.encodeMessages(messages))
            .build()
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(data)
            .setConstraints(networkConstraint)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
