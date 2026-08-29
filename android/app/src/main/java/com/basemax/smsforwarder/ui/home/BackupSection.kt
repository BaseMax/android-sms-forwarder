package com.basemax.smsforwarder.ui.home

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.basemax.smsforwarder.core.TimeUtils
import com.basemax.smsforwarder.ui.HomeUiState
import com.basemax.smsforwarder.ui.components.SectionCard
import com.basemax.smsforwarder.ui.components.StatusRow
import com.basemax.smsforwarder.ui.icons.AppIcons
import kotlin.math.abs
import kotlin.math.roundToLong

@Composable
fun BackupSection(
    state: HomeUiState,
    onSyncNow: () -> Unit,
) {
    SectionCard(title = "Backup", icon = AppIcons.CloudUpload) {
        StatusRow(
            icon = AppIcons.CloudUpload,
            label = "Messages uploaded",
            value = state.uploaded.toString(),
            ok = state.uploaded > 0,
        )
        StatusRow(
            icon = AppIcons.Sync,
            label = "Last sync",
            value = if (state.lastSyncMs <= 0L) "Never"
            else TimeUtils.formatForPeople(state.lastSyncMs),
            ok = state.lastSyncMs > 0L,
        )
        if (state.clockIsOff) {
            StatusRow(
                icon = AppIcons.Schedule,
                label = "Phone clock",
                value = describeSkew(state.clockSkewMs),
                ok = false,
            )
        }
        FilledTonalButton(
            onClick = onSyncNow,
            enabled = state.hasPermission,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(AppIcons.Sync, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Sync all messages now")
        }
    }
}

private fun describeSkew(skewMs: Long): String {
    val magnitude = abs(skewMs)
    val amount = when {
        magnitude < 3_600_000L -> "${(magnitude / 60_000.0).roundToLong()} min"
        magnitude < 86_400_000L -> "${(magnitude / 3_600_000.0).roundToLong()} h"
        else -> "${(magnitude / 86_400_000.0).roundToLong()} days"
    }
    return if (skewMs > 0) "$amount behind server" else "$amount ahead of server"
}
