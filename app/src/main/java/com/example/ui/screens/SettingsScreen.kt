package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.repository.ContentRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: ContentRepository,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    BackHandler { onBack() }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBars,
                title = { Text("Settings & About", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text("Privacy Policy", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("Admin managed & in-app viewer") },
                            leadingContent = { 
                                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary) 
                            },
                            trailingContent = {
                                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.clickable {
                                onNavigate("privacy_policy")
                            }
                        )
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ListItem(
                            headlineContent = { Text("Official Web Portal Links", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("All electricity & utility websites") },
                            leadingContent = { 
                                Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary) 
                            },
                            trailingContent = {
                                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.clickable {
                                onNavigate("bills")
                            }
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text("Share App", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("Share with friends and family") },
                            leadingContent = { 
                                Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary) 
                            },
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Check Pakistan Electricity Bills & Utilities with PowerPulse: Instant Bill Checking & Smart Calculators.")
                                }
                                context.startActivity(Intent.createChooser(intent, "Share App"))
                            }
                        )
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ListItem(
                            headlineContent = { Text("Send Feedback", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("Suggestions or report issues") },
                            leadingContent = { 
                                Icon(Icons.Default.Feedback, contentDescription = null, tint = MaterialTheme.colorScheme.primary) 
                            },
                            trailingContent = {
                                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.clickable {
                                onNavigate("feedback")
                            }
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    ListItem(
                        headlineContent = { Text("App Version", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { 
                            Text("1.0.0 (Build 1) • Content v${repository.getCachedContentVersion()}") 
                        },
                        leadingContent = { 
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) 
                        }
                    )
                }
            }
        }
    }
}
