package com.basemax.smsforwarder.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object Permissions {

    val required: Array<String> = buildList {
        add(Manifest.permission.READ_SMS)
        add(Manifest.permission.RECEIVE_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    fun hasSmsAccess(context: Context): Boolean =
        isGranted(context, Manifest.permission.READ_SMS) &&
            isGranted(context, Manifest.permission.RECEIVE_SMS)

    private fun isGranted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
