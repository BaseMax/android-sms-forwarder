package com.basemax.smsforwarder

import android.app.Application
import com.basemax.smsforwarder.core.AppLog
import com.basemax.smsforwarder.service.Notifications
import com.basemax.smsforwarder.work.SyncScheduler

class SmsForwarderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            Notifications.ensureChannel(this)
            SyncScheduler.schedulePeriodic(this)
        } catch (e: Exception) {
            AppLog.e("App setup failed", e)
        }
    }
}
