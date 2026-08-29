package com.basemax.smsforwarder.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.basemax.smsforwarder.R
import com.basemax.smsforwarder.core.AppLog
import com.basemax.smsforwarder.domain.BackupManager
import com.basemax.smsforwarder.work.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class SmsForwarderService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        goForeground()
        startHeartbeat()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        goForeground()
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun startHeartbeat() {
        scope.launch {
            val backup = BackupManager(applicationContext)
            while (isActive) {
                try {
                    if (backup.isConfigured()) SyncScheduler.syncNow(applicationContext)
                } catch (e: Exception) {
                    AppLog.e("Heartbeat sync failed", e)
                }
                delay(HEARTBEAT_MS)
            }
        }
    }

    private fun goForeground() {
        try {
            val notification = Notifications.buildServiceNotification(
                this, getString(R.string.service_active),
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    Notifications.SERVICE_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            } else {
                startForeground(Notifications.SERVICE_NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            AppLog.e("Could not enter foreground; stopping service", e)
            stopSelf()
        }
    }

    companion object {
        private val HEARTBEAT_MS = TimeUnit.HOURS.toMillis(2)
    }
}
