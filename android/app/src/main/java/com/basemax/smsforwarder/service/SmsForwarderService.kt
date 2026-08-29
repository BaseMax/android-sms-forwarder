package com.basemax.smsforwarder.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.basemax.smsforwarder.R
import com.basemax.smsforwarder.data.Settings
import com.basemax.smsforwarder.work.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

private const val TAG = "SmsForwarder"

class SmsForwarderService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        goForeground(getString(R.string.service_active))
        startHeartbeat()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        goForeground(getString(R.string.service_active))
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun startHeartbeat() {
        scope.launch {
            val settings = Settings(applicationContext)
            while (isActive) {
                try {
                    val configured = settings.baseUrl.first().isNotBlank() &&
                        settings.apiKey.first().isNotBlank()
                    if (configured) SyncScheduler.syncNow(applicationContext)
                } catch (e: Exception) {
                    Log.e(TAG, "Heartbeat sync failed", e)
                }
                delay(HEARTBEAT_MS)
            }
        }
    }

    private fun goForeground(text: String) {
        try {
            val notification = Notifications.buildServiceNotification(this, text)
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
            Log.e(TAG, "Could not enter foreground; stopping service", e)
            stopSelf()
        }
    }

    companion object {
        private val HEARTBEAT_MS = TimeUnit.HOURS.toMillis(2)
    }
}
