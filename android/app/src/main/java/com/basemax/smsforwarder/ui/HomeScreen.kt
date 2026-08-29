package com.basemax.smsforwarder.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.basemax.smsforwarder.ui.components.BrandMark
import com.basemax.smsforwarder.ui.home.BackupSection
import com.basemax.smsforwarder.ui.home.ProtectionSection
import com.basemax.smsforwarder.ui.home.ServerSection
import com.basemax.smsforwarder.ui.home.StatusHero

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onBaseUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onDeviceIdChange: (String) -> Unit,
    onRequestPermissions: () -> Unit,
    onSave: () -> Unit,
    onTest: () -> Unit,
    onSyncNow: () -> Unit,
    onFixBattery: () -> Unit,
    onMessageShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        val text = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(text)
        onMessageShown()
    }

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatusHero(state = state, onRequestPermissions = onRequestPermissions)
            ProtectionSection(state = state, onFixBattery = onFixBattery)
            ServerSection(
                state = state,
                onBaseUrlChange = onBaseUrlChange,
                onApiKeyChange = onApiKeyChange,
                onDeviceIdChange = onDeviceIdChange,
                onSave = onSave,
                onTest = onTest,
            )
            BackupSection(state = state, onSyncNow = onSyncNow)
        }
    }
}
