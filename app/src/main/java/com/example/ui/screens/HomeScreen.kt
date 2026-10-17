package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.data.repository.ContentRepository

data class FeatureItem(val title: String, val icon: ImageVector, val route: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(repository: ContentRepository, onNavigate: (String) -> Unit) {
    val features = listOf(
        FeatureItem("Bill Checker", Icons.Default.Receipt, "bills"),
        FeatureItem("Bill Calculator", Icons.Default.Calculate, "calculator"),
        FeatureItem("Appliance Calculator", Icons.Default.Bolt, "appliances"),
        FeatureItem("Helplines", Icons.Default.Phone, "helplines"),
        FeatureItem("Package Codes", Icons.Default.PhoneAndroid, "codes"),
        FeatureItem("Reminders", Icons.Default.Alarm, "reminders"),
        FeatureItem("Energy Tips", Icons.Default.Lightbulb, "tips"),
        FeatureItem("Feedback", Icons.Default.Feedback, "feedback"),
        FeatureItem("Settings", Icons.Default.Settings, "settings")
    )

    // Check feature flags if any
    val activeFeatures = features.filter { item ->
        if (item.route == "bills") {
            val flag = repository.featureFlags.find { it.key == "bill_check" }
            flag?.enabled ?: true
        } else {
            true
        }
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Announcement banner if active
            val announcement = repository.announcements.firstOrNull()
            if (announcement != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(announcement.titleEn, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(announcement.bodyEn, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(activeFeatures) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clickable { onNavigate(item.route) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(item.icon, contentDescription = item.title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(item.title, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
