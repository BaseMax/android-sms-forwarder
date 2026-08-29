package com.basemax.smsforwarder.service

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

object ServiceController {

    private const val TAG = "SmsForwarder"

    fun start(context: Context) {
        val intent = Intent(context, SmsForwarderService::class.java)
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (e: Exception) {
            Log.w(TAG, "Could not start the foreground service now; START_STICKY will recover it", e)
        }
    }

    @Suppress("DEPRECATION")
    fun isRunning(context: Context): Boolean {
        return try {
            val am = context.getSystemService(ActivityManager::class.java) ?: return false
            val name = SmsForwarderService::class.java.name
            am.getRunningServices(Int.MAX_VALUE).any { it.service.className == name }
        } catch (e: Exception) {
            Log.w(TAG, "Could not query service state", e)
            false
        }
    }
}
