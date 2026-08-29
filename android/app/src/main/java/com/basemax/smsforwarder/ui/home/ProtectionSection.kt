package com.basemax.smsforwarder.ui.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.basemax.smsforwarder.ui.HomeUiState
import com.basemax.smsforwarder.ui.components.SectionCard
import com.basemax.smsforwarder.ui.components.StatusRow
import com.basemax.smsforwarder.ui.icons.AppIcons

@Composable
fun ProtectionSection(
    state: HomeUiState,
    onFixBattery: () -> Unit,
) {
    SectionCard(title = "Always-on protection", icon = AppIcons.Shield) {
        StatusRow(
            icon = AppIcons.Sms,
            label = "Background service",
            value = if (state.serviceRunning) "Running" else "Starting",
            ok = state.serviceRunning,
        )
        StatusRow(
            icon = AppIcons.BatteryChargingFull,
            label = "Battery exemption",
            value = if (state.ignoringBattery) "Granted" else "Recommended",
            ok = state.ignoringBattery,
            action = if (state.ignoringBattery) null else "Allow" to onFixBattery,
        )
        Text(
            "Exempting the app from battery optimisation is the best way to keep " +
                "backups running when the app is closed.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
