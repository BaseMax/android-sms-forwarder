package com.basemax.smsforwarder.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.basemax.smsforwarder.ui.HomeUiState
import com.basemax.smsforwarder.ui.icons.AppIcons
import com.basemax.smsforwarder.ui.theme.WarnColor

@Composable
fun StatusHero(
    state: HomeUiState,
    onRequestPermissions: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val content: HeroContent = when {
        !state.hasPermission -> HeroContent(
            icon = AppIcons.WarningAmber,
            tint = WarnColor,
            title = "Permission needed",
            subtitle = "Allow access to your SMS so they can be backed up.",
            action = "Grant SMS permission" to onRequestPermissions,
        )
        !state.isConfigured -> HeroContent(
            icon = AppIcons.Dns,
            tint = primary,
            title = "Add your server",
            subtitle = "Enter your server URL and API key below, then tap Save.",
            action = null,
        )
        else -> HeroContent(
            icon = AppIcons.CheckCircle,
            tint = primary,
            title = if (state.serviceRunning) "Backups are active" else "Backups configured",
            subtitle = "${state.uploaded} messages backed up, new texts upload automatically.",
            action = null,
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(content.icon, null, tint = content.tint, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        content.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        content.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            content.action?.let { (label, onClick) ->
                Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(label) }
            }
        }
    }
}

private data class HeroContent(
    val icon: ImageVector,
    val tint: Color,
    val title: String,
    val subtitle: String,
    val action: Pair<String, () -> Unit>?,
)
