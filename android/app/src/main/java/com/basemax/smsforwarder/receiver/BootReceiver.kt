package com.basemax.smsforwarder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.basemax.smsforwarder.core.AppLog
import com.basemax.smsforwarder.service.ServiceController
import com.basemax.smsforwarder.work.SyncScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val app = context.applicationContext
                try {
                    SyncScheduler.schedulePeriodic(app)
                    ServiceController.start(app)
                    SyncScheduler.syncNow(app)
                } catch (e: Exception) {
                    AppLog.e("Boot setup failed", e)
                }
            }
        }
    }
}
