package com.basemax.smsforwarder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.basemax.smsforwarder.data.Settings
import com.basemax.smsforwarder.ui.theme.WarnColor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    settings: Settings,
    hasPermission: Boolean,
    serviceRunning: Boolean,
    ignoringBattery: Boolean,
    onRequestPermissions: () -> Unit,
    onSave: (baseUrl: String, apiKey: String, deviceId: String) -> Unit,
    onTestConnection: suspend () -> String,
    onSyncNow: () -> Unit,
    onFixBattery: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    var baseUrl by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var deviceId by remember { mutableStateOf("") }
    var seeded by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    val lastSync by settings.lastSyncMs.collectAsState(0L)
    val uploaded by settings.uploadedTotal.collectAsState(0)

    LaunchedEffect(Unit) {
        if (!seeded) {
            baseUrl = settings.baseUrl.first()
            apiKey = settings.apiKey.first()
            deviceId = settings.deviceId.first().ifBlank { android.os.Build.MODEL ?: "" }
            seeded = true
        }
    }

    val configured = baseUrl.isNotBlank() && apiKey.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BrandMark()
                        Spacer(Modifier.width(10.dp))
                        Text("SMS Forwarder", fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HeroCard(
                hasPermission = hasPermission,
                configured = configured,
                serviceRunning = serviceRunning,
                uploaded = uploaded,
                onRequestPermissions = onRequestPermissions,
            )

            SectionCard(title = "Always-on protection", icon = Icons.Rounded.Shield) {
                StatusRow(
                    icon = Icons.Rounded.Sms,
                    label = "Background service",
                    value = if (serviceRunning) "Running" else "Starting…",
                    ok = serviceRunning,
                )
                StatusRow(
                    icon = Icons.Rounded.BatteryChargingFull,
                    label = "Battery exemption",
                    value = if (ignoringBattery) "Granted" else "Recommended",
                    ok = ignoringBattery,
                    action = if (ignoringBattery) null else "Allow" to onFixBattery,
                )
                Text(
                    "Exempting the app from battery optimisation is the single best " +
                        "way to keep backups running when the app is closed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionCard(title = "Server", icon = Icons.Rounded.Dns) {
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Server URL") },
                    placeholder = { Text("https://sms.example.com") },
                    leadingIcon = { Icon(Icons.Rounded.Dns, null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API key") },
                    leadingIcon = { Icon(Icons.Rounded.Key, null) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = deviceId,
                    onValueChange = { deviceId = it },
                    label = { Text("Device label") },
                    leadingIcon = { Icon(Icons.Rounded.Smartphone, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { onSave(baseUrl, apiKey, deviceId); status = "Saved." },
                        modifier = Modifier.weight(1f),
                    ) { Text("Save") }
                    OutlinedButton(
                        onClick = { scope.launch { status = onTestConnection() } },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.Login, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Test")
                    }
                }
            }

            SectionCard(title = "Backup", icon = Icons.Rounded.CloudUpload) {
                StatusRow(
                    icon = Icons.Rounded.CloudUpload,
                    label = "Messages uploaded",
                    value = uploaded.toString(),
                    ok = uploaded > 0,
                )
                StatusRow(
                    icon = Icons.Rounded.Sync,
                    label = "Last sync",
                    value = if (lastSync <= 0L) "Never"
                    else DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                        .format(Date(lastSync)),
                    ok = lastSync > 0L,
                )
                FilledTonalButton(
                    onClick = { onSyncNow(); status = "Sync started." },
                    enabled = hasPermission,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Sync, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sync all messages now")
                }
            }

            if (status.isNotBlank()) {
                Text(
                    status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun BrandMark() {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.Sms,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun HeroCard(
    hasPermission: Boolean,
    configured: Boolean,
    serviceRunning: Boolean,
    uploaded: Int,
    onRequestPermissions: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val icon: ImageVector
    val tint: Color
    val title: String
    val subtitle: String
    val action: Pair<String, () -> Unit>?

    when {
        !hasPermission -> {
            icon = Icons.Rounded.WarningAmber
            tint = WarnColor
            title = "Permission needed"
            subtitle = "Allow access to your SMS so they can be backed up."
            action = "Grant SMS permission" to onRequestPermissions
        }
        !configured -> {
            icon = Icons.Rounded.Dns
            tint = primary
            title = "Add your server"
            subtitle = "Enter your server URL and API key below, then tap Save."
            action = null
        }
        else -> {
            icon = Icons.Rounded.CheckCircle
            tint = primary
            title = if (serviceRunning) "Backups are active" else "Backups configured"
            subtitle = "$uploaded messages backed up · new texts upload automatically."
            action = null
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            Modifier.padding(20.dp),
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
                    Icon(icon, null, tint = tint, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            action?.let { (label, onClick) ->
                Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(label) }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}

@Composable
private fun StatusRow(
    icon: ImageVector,
    label: String,
    value: String,
    ok: Boolean,
    action: Pair<String, () -> Unit>? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (ok) MaterialTheme.colorScheme.primary else WarnColor,
            modifier = Modifier.size(20.dp),
        )
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
