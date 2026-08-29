package com.basemax.smsforwarder.service

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.basemax.smsforwarder.core.AppLog

object ServiceController {

    fun start(context: Context) {
        try {
            ContextCompat.startForegroundService(
                context, Intent(context, SmsForwarderService::class.java),
            )
        } catch (e: Exception) {
            AppLog.w("Could not start the foreground service now; START_STICKY will recover it", e)
        }
    }

    @Suppress("DEPRECATION")
    fun isRunning(context: Context): Boolean = try {
        val am = context.getSystemService(ActivityManager::class.java)
        val name = SmsForwarderService::class.java.name
        am?.getRunningServices(Int.MAX_VALUE)?.any { it.service.className == name } ?: false
    } catch (e: Exception) {
        AppLog.w("Could not query service state", e)
        false
    }
}
