package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.data.model.BillLink
import com.example.data.repository.ContentRepository

data class FeatureItem(val title: String, val icon: ImageVector, val route: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(repository: ContentRepository, onNavigate: (String) -> Unit) {
    val features = listOf(
        FeatureItem("Bill Check (All)", Icons.Default.Receipt, "bills"),
        FeatureItem("Instant Search", Icons.Default.Search, "direct_bill"),
        FeatureItem("Bill Calculator", Icons.Default.Calculate, "calculator"),
        FeatureItem("Appliance Calc", Icons.Default.Bolt, "appliances"),
        FeatureItem("Helplines", Icons.Default.Phone, "helplines"),
        FeatureItem("Package Codes", Icons.Default.PhoneAndroid, "codes"),
        FeatureItem("Reminders", Icons.Default.Alarm, "reminders"),
        FeatureItem("Energy Tips", Icons.Default.Lightbulb, "tips"),
        FeatureItem("Feedback", Icons.Default.Feedback, "feedback"),
        FeatureItem("Settings", Icons.Default.Settings, "settings")
    )

    // Feature flags filter
    val activeFeatures = features.filter { item ->
        if (item.route == "bills") {
            val flag = repository.featureFlags.find { it.key == "bill_check" }
            flag?.enabled ?: true
        } else {
            true
        }
    }

    var quickRefNumber by remember { mutableStateOf(repository.getLastRefNumber()) }
    var selectedLink by remember(repository.billLinks) {
        mutableStateOf(repository.billLinks.firstOrNull())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bijli Bill & Utility Checker") },
                actions = {
                    IconButton(onClick = { onNavigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Announcement banner if active
            val announcement = repository.announcements.firstOrNull()
            if (announcement != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(announcement.titleEn, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(announcement.bodyEn, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Quick Bill Search Hero Card (0ms Instant Search from Local Storage)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Fast Bill Check (Saved Links)", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick board selector
                        if (repository.billLinks.isNotEmpty()) {
                            Text(
                                "Selected Board: ${selectedLink?.nameEn ?: repository.billLinks[0].nameEn}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        OutlinedTextField(
                            value = quickRefNumber,
                            onValueChange = { 
                                quickRefNumber = it.filter { ch -> ch.isDigit() }
                                repository.saveLastRefNumber(quickRefNumber)
                            },
                            label = { Text("Reference Number") },
                            placeholder = { Text("e.g. 14 digits ref number") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val targetLink = selectedLink ?: repository.billLinks.firstOrNull()
                                    val targetUrl = if (targetLink != null) {
                                        repository.resolveBillUrl(targetLink.url, quickRefNumber)
                                    } else {
                                        "https://bill.pitc.com.pk/gepcobill"
                                    }
                                    repository.currentSelectedUrl = targetUrl
                                    repository.currentSelectedTitle = targetLink?.nameEn ?: "Bill Viewer"
                                    onNavigate("webview")
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("View Bill ⚡")
                            }

                            OutlinedButton(
                                onClick = { onNavigate("bills") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("All Boards")
                            }
                        }
                    }
                }
            }

            // Grid of features
            item {
                Text("Features & Services", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                val chunkedFeatures = activeFeatures.chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    chunkedFeatures.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { item ->
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(105.dp)
                                        .clickable { onNavigate(item.route) },
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(14.dp),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(item.icon, contentDescription = item.title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(item.title, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
