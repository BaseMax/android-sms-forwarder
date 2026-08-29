package com.basemax.smsforwarder.core

import android.util.Log

object AppLog {

    private const val TAG = "SmsForwarder"

    fun i(message: String) = Log.i(TAG, message)

    fun w(message: String, error: Throwable? = null) {
        if (error == null) Log.w(TAG, message) else Log.w(TAG, message, error)
    }

    fun e(message: String, error: Throwable? = null) {
        if (error == null) Log.e(TAG, message) else Log.e(TAG, message, error)
    }
}
