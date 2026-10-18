package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.AppConfig
import com.example.data.repository.ContentRepository
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    repository: ContentRepository,
    onNavigateHome: () -> Unit,
    onNavigateMaintenance: (String) -> Unit,
    onNavigateUpdate: (AppConfig, Boolean) -> Unit
) {
    var statusText by remember { mutableStateOf("Initializing...") }

    LaunchedEffect(Unit) {
        statusText = "Checking for updates..."
        val config = repository.fetchAppConfig()

        if (config != null && config.maintenanceMode) {
            onNavigateMaintenance(config.maintenanceMessage ?: "App is under maintenance.")
            return@LaunchedEffect
        }

        // Version check (mock current version code 1)
        val currentVersionCode = 1
        if (config != null) {
            if (currentVersionCode < config.minVersionCode) {
                onNavigateUpdate(config, true) // Force update
                return@LaunchedEffect
            } else if (currentVersionCode < config.latestVersionCode) {
                onNavigateUpdate(config, false) // Optional update
                return@LaunchedEffect
            }
        }

        statusText = "Loading content..."
        repository.fetchAllContent()
        repository.incrementStat("app_open")

        delay(800)
        onNavigateHome()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("⚡", style = MaterialTheme.typography.displayLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Text("PowerPulse", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Smart Electricity & Utility Suite", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(statusText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
