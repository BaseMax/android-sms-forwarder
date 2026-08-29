package com.basemax.smsforwarder.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.basemax.smsforwarder.ui.theme.WarnColor

@Composable
fun StatusRow(
    icon: ImageVector,
    label: String,
    value: String,
    ok: Boolean,
    action: Pair<String, () -> Unit>? = null,
) {
    val accent = if (ok) MaterialTheme.colorScheme.primary else WarnColor
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (ok) MaterialTheme.colorScheme.onSurface else WarnColor,
        )
        if (action != null) {
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = action.second) { Text(action.first) }
        }
    }
}
