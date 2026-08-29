package com.basemax.smsforwarder.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.basemax.smsforwarder.ui.HomeUiState
import com.basemax.smsforwarder.ui.components.SectionCard
import com.basemax.smsforwarder.ui.icons.AppIcons

@Composable
fun ServerSection(
    state: HomeUiState,
    onBaseUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onDeviceIdChange: (String) -> Unit,
    onSave: () -> Unit,
    onTest: () -> Unit,
) {
    SectionCard(title = "Server", icon = AppIcons.Dns) {
        OutlinedTextField(
            value = state.baseUrl,
            onValueChange = onBaseUrlChange,
            label = { Text("Server URL") },
            placeholder = { Text("https://sms.example.com") },
            leadingIcon = { Icon(AppIcons.Dns, null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.apiKey,
            onValueChange = onApiKeyChange,
            label = { Text("API key") },
            leadingIcon = { Icon(AppIcons.Key, null) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.deviceId,
            onValueChange = onDeviceIdChange,
            label = { Text("Device label") },
            leadingIcon = { Icon(AppIcons.Smartphone, null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onSave, modifier = Modifier.weight(1f)) { Text("Save") }
            OutlinedButton(onClick = onTest, modifier = Modifier.weight(1f)) {
                Icon(AppIcons.Login, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Test")
            }
        }
    }
}
