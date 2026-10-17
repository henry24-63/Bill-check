package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.repository.ContentRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplianceCalculatorScreen(repository: ContentRepository, onBack: () -> Unit) {
    val appliances = repository.appliances
    var usageMap = remember { mutableStateMapOf<Int, Int>() } // appliance id -> hours per day

    LaunchedEffect(Unit) {
        repository.incrementStat("appliance_used")
    }

    BackHandler { onBack() }

    val totalKwhMonth = appliances.sumOf { app ->
        val hours = usageMap[app.id] ?: 0
        (app.defaultWatts * hours * 30) / 1000.0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appliance Calculator") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Estimated Monthly Consumption", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${Math.round(totalKwhMonth * 10) / 10.0} kWh (Units) / month", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(appliances) { app ->
                    val hours = usageMap[app.id] ?: 0
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(app.nameEn, style = MaterialTheme.typography.titleMedium)
                                Text("${app.defaultWatts} Watts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Button(onClick = { if (hours > 0) usageMap[app.id] = hours - 1 }) { Text("-") }
                                Text("$hours hrs", modifier = Modifier.padding(horizontal = 12.dp))
                                Button(onClick = { usageMap[app.id] = hours + 1 }) { Text("+") }
                            }
                        }
                    }
                }
            }
        }
    }
}
