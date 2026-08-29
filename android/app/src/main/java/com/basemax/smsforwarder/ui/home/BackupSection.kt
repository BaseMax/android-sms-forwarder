package com.basemax.smsforwarder.ui.home

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Sync
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
import kotlin.math.abs
import kotlin.math.roundToLong

@Composable
fun BackupSection(
    state: HomeUiState,
    onSyncNow: () -> Unit,
) {
    SectionCard(title = "Backup", icon = Icons.Rounded.CloudUpload) {
        StatusRow(
            icon = Icons.Rounded.CloudUpload,
            label = "Messages uploaded",
            value = state.uploaded.toString(),
            ok = state.uploaded > 0,
        )
        StatusRow(
            icon = Icons.Rounded.Sync,
            label = "Last sync",
            // In this phone's own timezone, and labelled with it: everything
            // that travels to the server is UTC, and this screen is the one
            // place that turns an instant back into local time.
            value = if (state.lastSyncMs <= 0L) "Never"
            else TimeUtils.formatForPeople(state.lastSyncMs),
            ok = state.lastSyncMs > 0L,
        )
        // Shown only when it is a problem. Messages are still backed up with a
        // wrong clock -- they are simply filed under the wrong day, and this
        // is the only warning the user would otherwise ever get.
        if (state.clockIsOff) {
            StatusRow(
                icon = Icons.Rounded.Schedule,
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
            Icon(Icons.Rounded.Sync, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Sync all messages now")
        }
    }
}

/**
 * "4 min behind server" / "2 h ahead of server".
 *
 * Phrased as a difference from the server rather than as a timezone, because
 * a timezone is never the cause: both clocks are compared as UTC instants, so
 * a gap means the date on this phone is genuinely wrong, not that it is set
 * to a different country.
 */
private fun describeSkew(skewMs: Long): String {
    val magnitude = abs(skewMs)
    val amount = when {
        magnitude < 3_600_000L -> "${(magnitude / 60_000.0).roundToLong()} min"
        magnitude < 86_400_000L -> "${(magnitude / 3_600_000.0).roundToLong()} h"
        else -> "${(magnitude / 86_400_000.0).roundToLong()} days"
    }
    return if (skewMs > 0) "$amount behind server" else "$amount ahead of server"
}
