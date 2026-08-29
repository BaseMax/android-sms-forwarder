package com.basemax.smsforwarder

import android.app.Application
import android.util.Log
import com.basemax.smsforwarder.service.Notifications
import com.basemax.smsforwarder.work.SyncScheduler

class SmsForwarderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            Notifications.ensureChannel(this)
            SyncScheduler.schedulePeriodic(this)
        } catch (e: Exception) {
            // Do not let setup crash a background process start (e.g. an SMS broadcast).
            Log.e("SmsForwarder", "App setup failed", e)
        }
    }
}
